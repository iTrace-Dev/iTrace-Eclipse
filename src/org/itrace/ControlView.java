package org.itrace;

import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * ViewPart for managing and controlling the plugin.
 */
public class ControlView extends ViewPart implements EventHandler {
    private Shell rootShell;

    private CopyOnWriteArrayList<Control> grayedControls = new CopyOnWriteArrayList<Control>();
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

        rootShell.getDisplay().addFilter(SWT.Show, new Listener() {
			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event e) {
				if(e.widget instanceof Shell) {
					System.out.println("Show");
					System.out.println(e.widget);
					ITrace.getDefault().addShell((Shell)e.widget);
					
					((Shell)e.widget).addDisposeListener(new DisposeListener() {

						@Override
						public void widgetDisposed(DisposeEvent arg0) {
							// TODO Auto-generated method stub
							ITrace.getDefault().removeShell((Shell)e.widget);
						}
						
					});
				}
			}
        });
        

        ITrace.getDefault().setRootShell(rootShell);
 
        // set up UI
        parent.setLayout(new RowLayout());
        
        //Button Composite start.
        final Composite buttonComposite = new Composite(parent, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(2, false));
        
        //Tracking start and stop button.
        final Button trackingButton = new Button(buttonComposite, SWT.PUSH);
        GridData trackingButtonGridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        trackingButtonGridData.widthHint = 100;
        trackingButton.setLayoutData(trackingButtonGridData);
        trackingButton.setText("Connect to Core");
        trackingButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	ITrace.getDefault().setActionBars(getViewSite().getActionBars());
            	if(ITrace.getDefault().toggleTracking()){
            		if(trackingButton.getText() == "Connect to Core"){
            			trackingButton.setText("Disconnect");
            			for (Control c : grayedControls) {
                            c.setEnabled(false);
                        }
            		}
                	else{
                		trackingButton.setText("Connect to Core");
                		for (Control c : grayedControls) {
                            c.setEnabled(true);
                        }
                	}
            	}
            	
            }
        });
        
        //Tuning Composite Start.
        final Button highlight_tokens = new Button(buttonComposite, SWT.CHECK);
        highlight_tokens.setText("Highlight Tokens");
        highlight_tokens.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        highlight_tokens.addSelectionListener(new SelectionAdapter(){
        	@Override
            public void widgetSelected(SelectionEvent e) {
        		ITrace.getDefault().activateHighlights();
        	}
        });
        //Tuning composite end.
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void setFocus() {
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
