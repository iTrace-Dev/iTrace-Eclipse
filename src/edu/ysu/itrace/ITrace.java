package edu.ysu.itrace;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import javax.swing.JWindow;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import edu.ysu.itrace.gaze.IGazeHandler;
import edu.ysu.itrace.GazeCursorWindow;
import edu.ysu.itrace.gaze.IGazeResponse;
import edu.ysu.itrace.gaze.IStyledTextGazeResponse;
import edu.ysu.itrace.solvers.ISolver;
import edu.ysu.itrace.solvers.XMLGazeExportSolver;

/**
 * The activator class controls the plug-in life cycle
 */
public class ITrace extends AbstractUIPlugin implements EventHandler {

    // The plug-in ID
    public static final String PLUGIN_ID = "edu.ysu.itrace"; //$NON-NLS-1$
    public long sessionStartTime;
    public Rectangle monitorBounds;
    // The shared instance
    private static ITrace plugin;
    private IEditorPart activeEditor;
    private HashMap<IEditorPart,TokenHighlighter> tokenHighlighters = new HashMap<IEditorPart,TokenHighlighter>();
    private boolean showTokenHighlights = false;

    private volatile boolean recording;
    private XMLGazeExportSolver xmlSolver;
    private boolean jsonOutput = true;
    private boolean xmlOutput = true;
    private ConnectionManager connectionManager;
    
    private IActionBars actionBars;
    private IStatusLineManager statusLineManager;
    private long registerTime = 2000;
    private IEventBroker eventBroker;
    private JWindow crosshairWindow = new GazeCursorWindow();
    private Shell rootShell;
    private int runCounter = 0;
    private String dirLocation = "";
    
    /**
     * The constructor
     */
    public ITrace() {
    	IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    	eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    	eventBroker.subscribe("iTrace/newgaze", this);
    	xmlSolver = new XMLGazeExportSolver();
    	eventBroker.subscribe("iTrace/xmlOutput", xmlSolver);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
    	if (recording) {
            disconnectFromServer();
        }
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static ITrace getDefault() {
        return plugin;
    }
    
    public Shell getRootShell(){
    	return rootShell;
    }
    public void setRootShell(Shell shell){
    	rootShell = shell;
    }
    
    public void setActionBars(IActionBars bars){
    	actionBars = bars;
    	statusLineManager = actionBars.getStatusLineManager();
    }
    
    public void setLineManager(IStatusLineManager manager){
    	statusLineManager = manager;
    }
    
    public void setXmlOutput(boolean value){
    	xmlOutput = value;
    }
    public void displayXmlExportFile(){
    	xmlSolver.displayExportFile();
    }
    
    public boolean connectToServer() {
    	connectionManager = new ConnectionManager();
        if (recording) {
            eventBroker.post("iTrace/error", "Tracking is already in progress.");
            return recording;
        }
        ITrace.getDefault().sessionStartTime = System.nanoTime();
        // Need to check for runCounter because init needs to be done only when we connect for the second time. 
        // However, init() safely assures that there will be no problem to initialize xml buffer.
        xmlSolver.init();
        recording = true;
        return recording;
    }
    
    public boolean disconnectFromServer() {
        if (!recording) {
        	eventBroker.post("iTrace/error", "Tracking is not in progress.");
            return false;
        }
        connectionManager.endSocketConnection();
        connectionManager = null;
        xmlSolver.dispose();
        statusLineManager.setMessage("");
        recording = false;
        return true;
    }
    
    public void setDirLocation() {
    	// This should set the directory name and location.
    	//xmlSolver.dirName = connectionManager.dirLocation;  	
    }
    
    public boolean toggleTracking(){
    	if(recording) return disconnectFromServer();
    	else return connectToServer();
    }
    
    public boolean displayGazeCursor(boolean display){
        connectionManager.showGazeCursor(display);	
    	return display;
    }
    
    public void setActiveEditor(IEditorPart editorPart){
    	activeEditor = editorPart;
    	if(activeEditor == null) return;
    	if(!tokenHighlighters.containsKey(editorPart)){
    		StyledText styledText = (StyledText) editorPart.getAdapter(Control.class);
    		if(styledText != null){
				ITextOperationTarget t = (ITextOperationTarget) activeEditor.getAdapter(ITextOperationTarget.class);
				if(t instanceof ProjectionViewer){
					ProjectionViewer projectionViewer = (ProjectionViewer)t;
					tokenHighlighters.put(activeEditor, new TokenHighlighter(styledText, showTokenHighlights, projectionViewer));
				}
			}
    	}
    	
    }
    
    public void activateHighlights(){
        	showTokenHighLights();
    }
    
    public void removeHighlighter(IEditorPart editorPart){
    	tokenHighlighters.remove(editorPart);
    }
    
    public void showTokenHighLights(){
    	showTokenHighlights = !showTokenHighlights;
    	if(activeEditor == null) return;
		if(!tokenHighlighters.containsKey(activeEditor)){
			StyledText styledText = (StyledText) activeEditor.getAdapter(Control.class);
			if(styledText != null){
				ITextOperationTarget t = (ITextOperationTarget) activeEditor.getAdapter(ITextOperationTarget.class);
				if(t instanceof ProjectionViewer){
					ProjectionViewer projectionViewer = (ProjectionViewer)t;
					tokenHighlighters.put(activeEditor, new TokenHighlighter(styledText, showTokenHighlights, projectionViewer));
				}
			}
		}
    	for(TokenHighlighter tokenHighlighter: tokenHighlighters.values()){
    		tokenHighlighter.setShow(showTokenHighlights);
    	}
    }
    
    /**
     * Finds the control under the specified screen coordinates and calls its
     * gaze handler on the localized point. Returns the gaze response or null if
     * the gaze is not handled.
     */
    private IGazeResponse handleGaze(int screenX, int screenY, Gaze gaze){
    	Queue<Control[]> childrenQueue = new LinkedList<Control[]>();
    	childrenQueue.add(rootShell.getChildren());
    	Rectangle monitorBounds = rootShell.getMonitor().getBounds();
        while (!childrenQueue.isEmpty()) {
            for (Control child : childrenQueue.remove()) {
                Rectangle childScreenBounds = child.getBounds();
                Point screenPos = child.toDisplay(0, 0);
                childScreenBounds.x = screenPos.x - monitorBounds.x;
                childScreenBounds.y = screenPos.y - monitorBounds.y;
                if (childScreenBounds.contains(screenX, screenY)) {
                    if (child instanceof Composite) {
                        Control[] nextChildren =
                                ((Composite) child).getChildren();
                        if (nextChildren.length > 0 && nextChildren[0] != null) {
                            childrenQueue.add(nextChildren);
                        }
                    }
                    IGazeHandler handler =
                            (IGazeHandler) child
                                    .getData(HandlerBindManager.KEY_HANDLER);
                    if (child.isVisible() && handler != null) {
                        return handler.handleGaze(screenX, screenY,
                                screenX - childScreenBounds.x, screenY
                                        - childScreenBounds.y, gaze);
                    }
                }
            }
        }
        return null;
    }
    
    @Override
   	public void handleEvent(Event event) {
   		if(event.getTopic() == "iTrace/newgaze"){
   			String[] propertyNames = event.getPropertyNames();
   			Gaze g = (Gaze)event.getProperty(propertyNames[0]);
   			dirLocation = g.getEventID();
   			 if (g != null) {
   	             if(!rootShell.isDisposed()){
   	            	 Rectangle monitorBounds = rootShell.getMonitor().getBounds();
   	            	 int screenX = (int) (g.getX());
   		             int screenY = (int) (g.getY());
   		             IGazeResponse response;
   	            	 response = handleGaze(screenX, screenY, g);
   	            	 
   	            	 if (response != null) {
   		                	 if(recording){
   		                		 statusLineManager
   		                 			.setMessage(String.valueOf(response.getGaze().getSessionTime()));
   		                 		registerTime = System.currentTimeMillis();
   		                 		if(xmlOutput) eventBroker.post("iTrace/xmlOutput", response);
   		                 		//if(jsonOutput) eventBroker.post("iTrace/jsonOutput", response);
   		                	 }
   		                     
   		                     if(response instanceof IStyledTextGazeResponse && response != null && showTokenHighlights){
   		                     	IStyledTextGazeResponse styledTextResponse = (IStyledTextGazeResponse)response;
   		                     	System.out.println("I am here");
   		                     	eventBroker.post("iTrace/newstresponse", styledTextResponse);
   		                     }
   		             }
   		         }else{
   		         	if((System.currentTimeMillis()-registerTime) > 2000){
   		         		statusLineManager.setMessage("");
   		         	}
   		         }
   	         }
   		}
   	}
}