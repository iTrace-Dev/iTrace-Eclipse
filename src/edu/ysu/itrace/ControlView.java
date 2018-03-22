package edu.ysu.itrace;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;


/**
 * ViewPart for managing and controlling the plugin.
 */
public class ControlView extends ViewPart implements IPartListener2, EventHandler{
    public static final String KEY_AST = "itraceAST";
    public static final String KEY_SO_DOM = "itraceSO";
    public static final String KEY_BR_DOM = "itraceBR";
    public static final String FATAL_ERROR_MSG = "A fatal error occurred. "
            + "Restart the plugin and try again. If "
            + "the problem persists, submit a bug report.";

    private Shell rootShell;

    private CopyOnWriteArrayList<Control> grayedControls =
            new CopyOnWriteArrayList<Control>();
    
    private ArrayList<IEditorReference> setupEditors = new ArrayList<IEditorReference>();
    
    private Spinner xDrift;
    private Spinner yDrift;
    

    private IEventBroker eventBroker;

    @Override
    public void createPartControl(Composite parent) {
    	eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
    	eventBroker.subscribe("iTrace/error", this);
        // find root shell
        rootShell = parent.getShell();
        while (rootShell.getParent() != null) {
            rootShell = rootShell.getParent().getShell();
        }

        ITrace.getDefault().setRootShell(rootShell);
        ITrace.getDefault().monitorBounds = rootShell.getMonitor().getBounds();

        // add listener for determining part visibility
        getSite().getWorkbenchWindow().getPartService().addPartListener(this);
        
        final String DONT_DO_THAT_MSG =
                "You can't do that until you've "
                        + "selected a tracker in preferences.";

        // set up UI
        parent.setLayout(new RowLayout());
        
        //Button Composite start.
        final Composite buttonComposite = new Composite(parent, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, false));
        
        //Tracking start and stop button.
        final Button trackingButton = new Button(buttonComposite, SWT.PUSH);
        trackingButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
                1, 1));
        trackingButton.setText("Connect to server");
        Point size = trackingButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        trackingButton.setSize(200, 50);
        trackingButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	ITrace.getDefault().setActionBars(getViewSite().getActionBars());
            	if(ITrace.getDefault().toggleTracking()){
            		if(trackingButton.getText() == "Connect to server"){
            			trackingButton.setText("Disconnect");
            			for (Control c : grayedControls) {
                            c.setEnabled(false);
                        }
            		}
                	else{
                		trackingButton.setText("Connect to server");
                		for (Control c : grayedControls) {
                            c.setEnabled(true);
                        }
                	}
            	}
            	
            }
        });
        
        //Eye Status Button
        final Button statusButton = new Button(buttonComposite, SWT.PUSH);
        statusButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,
                1, 1));
        statusButton.setText("Eye Status");
        statusButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	ITrace.getDefault().displayEyeStatus();
            }
        });
        //Button Composite End.
        
        final String DONT_CHANGE_THAT_MSG =
                "Don't change this value until "
                        + "you've selected a tracker in preferences.";
        
        //Tuning Composite Start.
        final Composite tuningComposite = new Composite(parent, SWT.NONE);
        tuningComposite.setLayout(new RowLayout(SWT.VERTICAL));

        final Button highlight_tokens = new Button(tuningComposite, SWT.CHECK);
        highlight_tokens.setText("Highlight Tokens");
        highlight_tokens.addSelectionListener(new SelectionAdapter(){
        	@Override
            public void widgetSelected(SelectionEvent e) {
        		ITrace.getDefault().activateHighlights();
        	}
        });
        
        final Button displayCrosshair = new Button(tuningComposite, SWT.CHECK);
        displayCrosshair.setText("Display Crosshair");
        displayCrosshair.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	boolean success = ITrace.getDefault().displayCrosshair(displayCrosshair.getSelection());
            	if(success != displayCrosshair.getSelection()) displayCrosshair.setSelection(false);
            }
        });

        //Tuning composite end.
        
        //Solvers composite begin.
        final Composite solversComposite = new Composite(parent, SWT.NONE);
        solversComposite.setLayout(new GridLayout(2, false));
        // Configure solvers here.
        
        final Button jsonSolverEnabled =
                    new Button(solversComposite, SWT.CHECK);
        jsonSolverEnabled.setText("JSON Export");
        jsonSolverEnabled.setSelection(true);
        jsonSolverEnabled.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               		if (jsonSolverEnabled.getSelection()) {
               			ITrace.getDefault().setJsonOutput(true);
               		} else {
               			ITrace.getDefault().setJsonOutput(false);
               		}
            }
        });
        grayedControls.addIfAbsent(jsonSolverEnabled);
        final Button jsonSolverConfig = new Button(solversComposite, SWT.PUSH);
        jsonSolverConfig.setText("...");
        jsonSolverConfig.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               		ITrace.getDefault().displayJsonExportFile();
            }
        });
        grayedControls.addIfAbsent(jsonSolverConfig);
            
       final Button xmlSolverEnabled =
    		   new Button(solversComposite, SWT.CHECK);
       xmlSolverEnabled.setText("XML Export");
       xmlSolverEnabled.setSelection(true);
       xmlSolverEnabled.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               		if (xmlSolverEnabled.getSelection()) {
               			ITrace.getDefault().setXmlOutput(true);
               		} else {
               			ITrace.getDefault().setXmlOutput(false);
               		}
           }
       });
       grayedControls.addIfAbsent(xmlSolverEnabled);
       final Button xmlSolverConfig = new Button(solversComposite, SWT.PUSH);
       xmlSolverConfig.setText("...");
       xmlSolverConfig.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
               		ITrace.getDefault().displayXmlExportFile();
            }
       });
       grayedControls.addIfAbsent(xmlSolverConfig);
       //Solver Composite end. 
        
    
        
        //Filter composite begin.
        final Composite filterComposite = new Composite(parent, SWT.NONE);
        filterComposite.setLayout(new GridLayout(2, false));
        
        //Filter composite end.
    }

    @Override
    public void dispose() {
        getSite().getWorkbenchWindow().getPartService()
                .removePartListener(this);
        super.dispose();
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
    	if(partRef.getPart(false) instanceof IEditorPart) {
    		ITrace.getDefault().setActiveEditor((IEditorPart)partRef.getPart(false));
    		IEditorPart ep = (IEditorPart)partRef.getPart(true);
    		ITrace.getDefault().setLineManager(ep.getEditorSite().getActionBars().getStatusLineManager());
    	}
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    	if(partRef.getPart(false) instanceof IEditorPart) {
    		ITrace.getDefault().setActiveEditor((IEditorPart)partRef.getPart(false));
    		IEditorPart ep = (IEditorPart)partRef.getPart(true);
    		ITrace.getDefault().setLineManager(ep.getEditorSite().getActionBars().getStatusLineManager());;
    	}
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
    	if(partRef instanceof IEditorReference){
    		setupEditors.remove(partRef);
    		ITrace.getDefault().setActionBars(getViewSite().getActionBars());
        	IEditorPart ep = (IEditorPart)partRef.getPart(true);
        	ITrace.getDefault().removeHighlighter(ep);
        	ITrace.getDefault().setActiveEditor(
        			PlatformUI.getWorkbench().getActiveWorkbenchWindow()
        			.getActivePage().getActiveEditor()
        	);
    	}
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
        setupControls(partRef);
        HandlerBindManager.bind(partRef);
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
        HandlerBindManager.unbind(partRef);
    }


    /**
     * Find controls within a part, set it up to be used by iTrace,
     * and extract meta-data from it.
     * 
     * @param partRef partRef that just became visible.
     */
    private void setupControls(IWorkbenchPartReference partRef) {
    	IWorkbenchPart part = partRef.getPart(true);
        Control control = part.getAdapter(Control.class);
        //set up manager for control and managers for each child control if necessary
        if (control != null) {
        	setupControls(part, control);
        } else {
        	//Browser - always set up browser managers, no matter the partRef that
        	//has become visible
        	//not possible to get Browser control from a partRef
        	Shell workbenchShell = partRef.getPage().getWorkbenchWindow().getShell();
        	for (Control ctrl: workbenchShell.getChildren()) {
        		setupBrowsers(ctrl);
        	}
        }
    }
    
    /**
     * Recursive helper function to find and set up Browser control managers
     * @param control
     */
    private void setupBrowsers(Control control) {
    
    	if (control instanceof Browser) {
    		setupControls(null, control);
    	}
    	
    	//If composite, look through children.
        if (control instanceof Composite) {
            Composite composite = (Composite) control;

            Control[] children = composite.getChildren();
            if (children.length > 0 && children[0] != null) {
               for (Control curControl : children)
                   setupBrowsers(curControl);
            }
        }
    }
    
    /**
     * Recursive function for setting up children controls for a control if it is
     * a composite and setting up the main control's manager.
     * @param part
     * @param control
     */
    private void setupControls(IWorkbenchPart part, Control control) {
    	//If composite, setup children controls.
        if (control instanceof Composite) {
            Composite composite = (Composite) control;

            Control[] children = composite.getChildren();
            if (children.length > 0 && children[0] != null) {
               for (Control curControl : children)
                   setupControls(part, curControl);
            }
        }
        
        if (control instanceof StyledText) {
        	//set up styled text manager if there is one
        	setupStyledText((IEditorPart) part, (StyledText) control);
        	
        } 
        //TODO: no control set up for a ProjectExplorer, since there isn't an need for 
        //a Manager right now, might be needed in the future
    }
    
    /**
     * Recursive helper method for setupControls(IWorkbenchPartReference).
     * 
     * @param editor IEditorPart which owns the StyledText in the next
     *               parameter.
     * @param styledText StyledText to set up.
     */
    private void setupStyledText(IEditorPart editor, StyledText styledText) {
        if (styledText.getData(KEY_AST) == null)
            styledText.setData(KEY_AST, new AstManager(editor, styledText));
    }

    

    private void displayError(String message) {
        MessageBox error_box = new MessageBox(rootShell, SWT.ICON_ERROR);
        error_box.setMessage(message);
        error_box.open();
    }

	@Override
	public void handleEvent(Event event) {
		String[] propertyNames = event.getPropertyNames();
		String message = (String)event.getProperty(propertyNames[0]);
		displayError(message);
	}

}
