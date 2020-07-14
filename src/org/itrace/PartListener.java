package org.itrace;

import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPartReference;
import org.itrace.gaze.handlers.WorkbenchGazeHandler;

public class PartListener extends ControlAdapter implements IPartListener2  {
	WorkbenchGazeHandler gazeHandler;
	
	public PartListener(IWorkbench workbench, WorkbenchGazeHandler workbenchGazeHandler) {
		gazeHandler = workbenchGazeHandler;
		workbench.getActiveWorkbenchWindow().getPartService().addPartListener(this);
	}
	
	@Override
	public void controlResized(ControlEvent e) {
	}

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
    	if(partRef.getPart(false) instanceof IEditorPart) {
    		gazeHandler.addEditor((IEditorPart)partRef.getPart(true));    		
    	}
    	else if(partRef.getPart(false) instanceof IViewPart) {
    		gazeHandler.addView((IViewPart)partRef.getPart(true));
    	}
    	else {
    		System.out.println(partRef);
    	}
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    	/*
    	if(partRef.getPart(false) instanceof IEditorPart) {
    		ITrace.getDefault().setActiveEditor((IEditorPart)partRef.getPart(false));
    		IEditorPart ep = (IEditorPart)partRef.getPart(true);
        	ITrace.getDefault().setLineManager(ep.getEditorSite().getActionBars().getStatusLineManager());
    	} else {
			IWorkbenchPart ep = partRef.getPart(true);
	    	ITrace.getDefault().setLineManager(((IViewSite) ep.getSite()).getActionBars().getStatusLineManager());
    	}
    	*/
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
    	if(partRef instanceof IEditorReference){    		
        	IEditorPart editorPart = (IEditorPart)partRef.getPart(true);
        	gazeHandler.removeEditor(editorPart);
    	}
    	else if(partRef instanceof IViewReference) {
    		IViewPart viewPart = (IViewPart)partRef.getPart(true);
    		gazeHandler.removeViewPart(viewPart);
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

}
