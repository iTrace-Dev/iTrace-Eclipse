package org.itrace;

import java.util.HashMap;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import org.itrace.gaze.IGazeResponse;
import org.itrace.gaze.handlers.WorkbenchGazeHandler;
import org.itrace.solvers.XMLGazeExportSolver;

/**
 * The activator class controls the plug-in life cycle
 */
public class ITrace extends AbstractUIPlugin implements EventHandler {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.itrace"; //$NON-NLS-1$
	public long sessionStartTime;
	public Rectangle monitorBounds;

	// The shared instance
	private static ITrace plugin;
	private IEditorPart activeEditor;
	private HashMap<IEditorPart, TokenHighlighter> tokenHighlighters = new HashMap<IEditorPart, TokenHighlighter>();
	private WorkbenchGazeHandler workbenchGazeHandler;
	private boolean showTokenHighlights = false;

	private volatile boolean isConnected;
	private boolean isRecording;
	private XMLGazeExportSolver xmlSolver;
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
		if (editorPart != null) {
			StyledText styledText = (StyledText) editorPart.getAdapter(Control.class);
			if (styledText != null) {
				tokenHighlighters.put(editorPart, new TokenHighlighter(editorPart, showTokenHighlights));
			}
		}
		
		connectionManager = new ConnectionManager();
		// iTrace invokes the events to the Eventbroker and these events are subscribed
		// in an order.
		eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
		eventBroker.subscribe("iTrace/newgaze", this);
		eventBroker.subscribe("iTrace/sessionstart", this);
		eventBroker.subscribe("iTrace/sessionend", this);
		xmlSolver = new XMLGazeExportSolver();
		eventBroker.subscribe("iTrace/xmlOutput", xmlSolver);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
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

	public Shell getRootShell() {
		return rootShell;
	}

	public void setRootShell(Shell shell) {
		rootShell = shell;
		if(workbenchGazeHandler == null) {
			workbenchGazeHandler = new WorkbenchGazeHandler(PlatformUI.getWorkbench());			
		}
	}

	public void setActionBars(IActionBars bars) {
		actionBars = bars;
		statusLineManager = actionBars.getStatusLineManager();
	}

	public void setLineManager(IStatusLineManager manager) {
		statusLineManager = manager;
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
		if (isConnected == false) {
			eventBroker.post("iTrace/error", "Cannot Start Session. Plugin is not Connected to Core.");
			return false;
		}

		if (isRecording) {
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
			endSession();
		}
		
		isConnected = false;
		return true;
	}

	public boolean endSession() {
		if (!isRecording) {
			eventBroker.post("iTrace/error", "No Session has started");
			return false;
		}
		
		isRecording = false;
		
		if(showTokenHighlights) {
			clearTokenHighlights();	
		}

		xmlSolver.dispose();
		statusLineManager.setMessage("");
		return true;
	}

	public boolean toggleTracking() {
		if (isConnected)
			return disconnectFromServer();
		else
			return connectToServer();
	}

	public void setActiveEditor(IEditorPart editorPart) {
		if(activeEditor == editorPart) {
			return;
		}
		
		activeEditor = editorPart;

		if(showTokenHighlights) {
			clearTokenHighlights();			
		}
		
		if (activeEditor != null) {
			if (!tokenHighlighters.containsKey(editorPart)) {
				tokenHighlighters.put(editorPart, new TokenHighlighter(editorPart, showTokenHighlights));
			}
			setLineManager(editorPart.getEditorSite().getActionBars().getStatusLineManager());
			workbenchGazeHandler.addEditor(editorPart);
		}
	}
	
	public void setActiveViewPart(IViewPart viewPart) {
		setLineManager(viewPart.getViewSite().getActionBars().getStatusLineManager());
		workbenchGazeHandler.addViewPart(viewPart);
	}

	public void removeEditor(IEditorPart editorPart) {
		tokenHighlighters.remove(editorPart);
		workbenchGazeHandler.removeEditor(editorPart);
	}
	
	public void removeViewPart(IViewPart viewPart) {
		workbenchGazeHandler.removeViewPart(viewPart);
		
	}


	public void activateHighlights() {
		showTokenHighlights = !showTokenHighlights;
		if (activeEditor != null && !tokenHighlighters.containsKey(activeEditor)) {
			tokenHighlighters.put(activeEditor, new TokenHighlighter(activeEditor, showTokenHighlights));
			if (activeEditor == null)
				return;
		}
		for (TokenHighlighter tokenHighlighter : tokenHighlighters.values()) {
			tokenHighlighter.setShow(showTokenHighlights);
		}
	}
	
	public void clearTokenHighlights() {
		if (activeEditor != null && tokenHighlighters.containsKey(activeEditor)) {
			tokenHighlighters.get(activeEditor).clearHighlights();			
		}
	}

	/**
	 * Finds the control under the specified screen coordinates and calls its gaze
	 * handler on the localized point. Returns the gaze response or null if the gaze
	 * is not handled.
	 */
	private IGazeResponse handleGaze(int screenX, int screenY, Gaze gaze) {
		if(workbenchGazeHandler.containsGaze(screenX, screenY)) {
			return workbenchGazeHandler.handleGaze(screenX, screenY, gaze);
		}
		// Look at all editors and find an active one that contains the point
		// Returns the result of the editor's handleGaze method
		return null;
	}

	public void processData() {
		while (isRecording) {
			if (connectionManager.isDataReady()) {
				Gaze g = connectionManager.popCurrentGaze();

				// Have to run code that interfaces with UI in SWT thread
				// Running this thread in SWT thread causes UI to freeze
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						if (isConnected && g != null) {
							statusLineManager.setMessage(String.valueOf(g.getEventTime()));
							statusLineManager.update(true);
							registerTime = System.currentTimeMillis();
						} else {
							if ((System.currentTimeMillis() - registerTime) > 2000) {
								statusLineManager.setMessage("");
							}
						}
					}
				});
				if (g != null) {
					if (!rootShell.isDisposed()) {
						int screenX = (int) (g.getX());
						int screenY = (int) (g.getY());
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								IGazeResponse response;
								response = handleGaze(screenX, screenY, g);
								if (response != null) {
									xmlSolver.process(response);
								}

								if (showTokenHighlights) {
									if (activeEditor != null && tokenHighlighters.containsKey(activeEditor)) {
										tokenHighlighters.get(activeEditor).handleGaze(response);
									}
								}
							}
						});
					}
				}
			}
			Thread.yield();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (event.getTopic() == "iTrace/sessionstart") {
			String[] propertyNames = event.getPropertyNames();
			String[] eventData = (String[]) event.getProperty(propertyNames[0]);
			startSession(eventData[2], eventData[0]);
		} else if (event.getTopic() == "iTrace/sessionend") {
			endSession();
		}
	}
}