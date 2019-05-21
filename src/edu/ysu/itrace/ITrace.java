package edu.ysu.itrace;

import java.util.HashMap;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import edu.ysu.itrace.gaze.IGazeResponse;
import edu.ysu.itrace.gaze.IStyledTextGazeResponse;
import edu.ysu.itrace.gaze.handlers.IGazeHandler;
import edu.ysu.itrace.gaze.handlers.StyledTextGazeHandler;
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
    private HashMap<IEditorPart,IGazeHandler> editorHandlers = new HashMap<IEditorPart,IGazeHandler>();
    private boolean showTokenHighlights = false;

    private volatile boolean isConnected;
    private boolean isRecording;
    private XMLGazeExportSolver xmlSolver;
    private boolean xmlOutput = true;
    private ConnectionManager connectionManager;
    
    private IActionBars actionBars;
    private IStatusLineManager statusLineManager;
    private long registerTime = 2000;
    private IEventBroker eventBroker;
    private Shell rootShell;
    
    /**
     * The constructor
     */
    public ITrace() {
    	/** 
    	 * This part is used to stabilize the functioning of token highlighter. 
    	 */
    	IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    	if(editorPart != null){
	    	StyledText styledText = (StyledText) editorPart.getAdapter(Control.class);
	    	if(styledText != null){
				tokenHighlighters.put(editorPart, new TokenHighlighter(editorPart, showTokenHighlights));
				editorHandlers.put(editorPart, new StyledTextGazeHandler(styledText, editorPart));
			}
    	}
    	connectionManager = new ConnectionManager();
    	//iTrace invokes the events to the Eventbroker and these events are subscribed in an order.
    	eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    	eventBroker.subscribe("iTrace/newgaze", this);
    	eventBroker.subscribe("iTrace/sessionstart", this);
    	eventBroker.subscribe("iTrace/sessionend", this);
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
    	if (isConnected) {
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
        if (isConnected) {
            eventBroker.post("iTrace/error", "Tracking is already in progress.");
            return isConnected;
        }
    	connectionManager.startConnection();
        ITrace.getDefault().sessionStartTime = System.nanoTime();
        isConnected = true;
        return isConnected;
    }
    
    public boolean startSession(String directoryPath, String sessionId) {
    	if(isConnected == false) {
    		eventBroker.post("iTrace/error", "Cannot Start Session. Plugin is not Connected to Core.");
    		return false;
    	}
    	
    	if(isRecording) {
    		eventBroker.post("iTrace/error", "Session already started");
    		return true;
    	}

		String filename = directoryPath + "/itrace_eclipse-" + System.currentTimeMillis() + ".xml";
		xmlSolver.config(filename, sessionId);
		xmlSolver.init();
		isRecording = true;
		
		Thread processThread = new Thread() {
			@Override
			public void run() {
				processData();
			}
		};
		processThread.start();
		
		return true;
    }
    
    public boolean disconnectFromServer() {
        if (!isConnected) {
        	eventBroker.post("iTrace/error", "Tracking is not in progress.");
            return false;
        }
        connectionManager.endSocketConnection();
        if (isRecording) {
        	xmlSolver.dispose();
        }
        statusLineManager.setMessage("");
        isConnected = false;
        isRecording = false;
        return true;
    }
    
    public boolean endSession()
    {
    	if(!isRecording) {
    		eventBroker.post("iTrace/error", "No Session has started");
    		return false;
    	}
    	
    	xmlSolver.dispose();
    	isRecording = false;
    	return true;
    }

    public boolean toggleTracking(){
    	if(isConnected) return disconnectFromServer();
    	else return connectToServer();
    }
    
    public void setActiveEditor(IEditorPart editorPart){
    	activeEditor = editorPart;
    	
    	if(activeEditor != null) {        	
        	if(!tokenHighlighters.containsKey(editorPart)){
        		tokenHighlighters.put(editorPart, new TokenHighlighter(editorPart,showTokenHighlights));
        	}
        	if(!editorHandlers.containsKey(editorPart)) {
    	    	StyledText styledText = (StyledText) editorPart.getAdapter(Control.class);
    	    	if(styledText != null){
    	    		editorHandlers.put(editorPart, new StyledTextGazeHandler(styledText, editorPart));
    			}
        	}    	
    	}
    }

    public void removeEditor(IEditorPart editorPart){
    	tokenHighlighters.remove(editorPart);
    	editorHandlers.remove(editorPart);
    }
    
    public void activateHighlights(){
    	showTokenHighlights = !showTokenHighlights;
		if(activeEditor != null && !tokenHighlighters.containsKey(activeEditor)){
			tokenHighlighters.put(activeEditor, new TokenHighlighter(activeEditor, showTokenHighlights));
			if(activeEditor == null) return;
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
    	Rectangle monitorBounds = rootShell.getMonitor().getBounds();
    	
    	// Look at all editors and find an active one that contains the point
    	// Returns the result of the editor's handleGaze method
    	for(IEditorPart editor : editorHandlers.keySet()) {
    		Control editorControl = editor.getAdapter(Control.class);
    		Rectangle editorScreenBounds = editorControl.getBounds();
            Point screenPos = editorControl.toDisplay(0, 0);
            editorScreenBounds.x = screenPos.x - monitorBounds.x;
            editorScreenBounds.y = screenPos.y - monitorBounds.y;
            
    		if(editorControl.isVisible() && editorScreenBounds.contains(screenX, screenY)){ 
    			IGazeHandler handler = editorHandlers.get(editor);
    			return handler.handleGaze(screenX, screenY, screenX - editorScreenBounds.x,
    					screenY - editorScreenBounds.y, gaze);
    		}
    	}
    	return null;
    }
    
    public void processData() {
		while(isRecording) {
			Thread.yield();
			if(connectionManager.isDataReady()) {	
				Gaze g = connectionManager.popCurrentGaze();
				
				// Have to run code that interfaces with UI in SWT thread
				// Running this thread in SWT thread causes UI to freeze
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
			    		if(isConnected && g != null){
							statusLineManager.setMessage(String.valueOf(g.getEventTime()));
							statusLineManager.update(true);
							registerTime = System.currentTimeMillis();
						} else {
							if((System.currentTimeMillis()-registerTime) > 2000){
					 			statusLineManager.setMessage("");
							}
						}						
					}					
				});
				if (g != null) {
					if(!rootShell.isDisposed()){
						int screenX = (int) (g.getX());
						int screenY = (int) (g.getY());
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								IGazeResponse response;
								response = handleGaze(screenX, screenY, g);
								if (response != null) {
									if(xmlOutput) {
										xmlSolver.process(response);
				   	  				}
				   	  				
									if(response instanceof IStyledTextGazeResponse && response != null && showTokenHighlights){
										IStyledTextGazeResponse styledTextResponse = (IStyledTextGazeResponse)response;
										// Change to function call
										eventBroker.post("iTrace/newstresponse", styledTextResponse);
									}
								}
							}
						});		            	 
					}
				}
			}
		}    	
    }
    
    @Override
   	public void handleEvent(Event event) {
    	if(event.getTopic() == "iTrace/sessionstart") {
   			String[] propertyNames = event.getPropertyNames();
   			String[] eventData = (String[])event.getProperty(propertyNames[0]);
   			startSession(eventData[2], eventData[0]);
   		}
   		else if(event.getTopic() == "iTrace/sessionend") {
   			endSession();
   		}
   	}
}