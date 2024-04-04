/********************************************************************************************************************************************************
* @file ControlView.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
package org.itrace;

import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.eclipse.swt.widgets.Label;

/**
 * ViewPart for managing and controlling the plugin.
 */
public class ControlView extends ViewPart implements IPartListener2, EventHandler{
    private Shell rootShell;

    private CopyOnWriteArrayList<Control> grayedControls = new CopyOnWriteArrayList<Control>();
    private IEventBroker eventBroker;

    private static final String AFFECTIVA_PREF_KEY = "AffectivaDir";
    private static final String EMPATICA_PREF_KEY = "EmpaticaDir";
    private static final String DEFAULT_TEXT = ""; // Default text if preference not found
    
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
        final Label blank = new Label(buttonComposite, SWT.NONE);
        blank.setText("");
        
        final Button highlight_tokens = new Button(buttonComposite, SWT.CHECK);
        highlight_tokens.setText("Highlight Tokens");
        highlight_tokens.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
        highlight_tokens.addSelectionListener(new SelectionAdapter(){
        	@Override
            public void widgetSelected(SelectionEvent e) {
        		ITrace.getDefault().activateHighlights();
        	}
        });
        //External Launcher
        Button external_launcher = new Button(buttonComposite, SWT.CHECK);
        external_launcher.setText("External Launcher");
        external_launcher.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		ITrace.getDefault().toggleExternalLauncher();
        	}
        });
        final Composite textComposite = new Composite(parent, SWT.NONE);
        textComposite.setLayout(new GridLayout(3, false));
        //Affectiva Directory
        Label affectiva_dir = new Label(textComposite, SWT.CHECK);
        affectiva_dir.setText("Affectiva Directory:");
        //TODO: use saved settings

        Text affectiva_directory = new Text(textComposite, SWT.BORDER);
        GridData textLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
        textLayout.widthHint = 200;
        affectiva_directory.setLayoutData(textLayout);        
        affectiva_directory.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                ITrace.getDefault().setAffectivaDirectory(affectiva_directory.getText());
            }
        });
        
        // file select button
        Button select_affectiva = new Button(textComposite, SWT.OPEN);
        select_affectiva.setText("...");
        select_affectiva.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(rootShell, SWT.OPEN);
                dialog.setText("Open");
                dialog.setFilterExtensions(new String[] { "*.exe", "*.*" }); // Filter extensions
                String affectivaSelectedFile = dialog.open();
                if (affectivaSelectedFile != null) {
                    System.out.println("Selected file: " + affectivaSelectedFile);
                    // Perform actions with the selected file path
                    affectiva_directory.setText(affectivaSelectedFile);
                } else {
                    System.out.println("No file selected");
                }
            }
        });
        //Empatica Directory
        Label empatica_dir = new Label(textComposite, SWT.CHECK);
        empatica_dir.setText("Empatica Directory:");
        // TODO: persistent directory value
//        directorySettings.setDefault(EMPATICA_PREF_KEY, DEFAULT_TEXT);
        
        Text empatica_directory = new Text(textComposite, SWT.BORDER);
        empatica_directory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        empatica_directory.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                ITrace.getDefault().setEmpaticaDirectory(empatica_directory.getText());
            }
        });
//        empatica_directory.setText(directorySettings.getString(EMPATICA_PREF_KEY));
        
        // file select button
        Button select_empatica = new Button(textComposite, SWT.OPEN);
        select_empatica.setText("...");
        select_empatica.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(rootShell, SWT.OPEN);
                dialog.setText("Open");
                dialog.setFilterExtensions(new String[] { "*.exe", "*.*" }); // Filter extensions
                String empaticaSelectedFile = dialog.open();
                if (empaticaSelectedFile != null) {
                    System.out.println("Selected file: " + empaticaSelectedFile);
                    // Perform actions with the selected file path
                    empatica_directory.setText(empaticaSelectedFile);
                } else {
                    System.out.println("No file selected");
                }
            }
        });
        //TODO: Key Tracking
//        Button key_tracking = new Button(buttonComposite, SWT.CHECK);
//        key_tracking.setText("Key Tracker");
//        key_tracking.addSelectionListener(new SelectionAdapter() {
//        	@Override
//        	public void widgetSelected(SelectionEvent e) {
//        		ITrace.getDefault().toggleKeyTracker();
//        	}
//        });
        new Label(buttonComposite, SWT.NONE);
        //Tuning composite end.
    }

    @Override
    public void dispose() {
        getSite().getWorkbenchWindow().getPartService().removePartListener(this);
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
    	} else {
			IWorkbenchPart ep = partRef.getPart(true);
	    	ITrace.getDefault().setLineManager(((IViewSite) ep.getSite()).getActionBars().getStatusLineManager());
    	}
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    	if(partRef.getPart(false) instanceof IEditorPart) {
    		ITrace.getDefault().setActiveEditor((IEditorPart)partRef.getPart(false));
    		IEditorPart ep = (IEditorPart)partRef.getPart(true);
        	ITrace.getDefault().setLineManager(ep.getEditorSite().getActionBars().getStatusLineManager());
    	} else {
			IWorkbenchPart ep = partRef.getPart(true);
	    	ITrace.getDefault().setLineManager(((IViewSite) ep.getSite()).getActionBars().getStatusLineManager());
    	}
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
    	if(partRef instanceof IEditorReference){
    		ITrace.getDefault().setActionBars(getViewSite().getActionBars());
        	IEditorPart editorPart = (IEditorPart)partRef.getPart(true);
        	ITrace.getDefault().removeEditor(editorPart);
        	
        	IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        	ITrace.getDefault().setActiveEditor(activeEditor);
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
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
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
