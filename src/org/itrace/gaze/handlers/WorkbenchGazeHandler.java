package org.itrace.gaze.handlers;

import java.util.HashMap;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.itrace.Gaze;
import org.itrace.ITrace;
import org.itrace.PartListener;
import org.itrace.gaze.IGazeResponse;

public class WorkbenchGazeHandler implements IGazeHandler {
	protected IWorkbench workbench;
	protected PartListener workbenchPartListener;

	protected HashMap<IWorkbenchPart, IGazeHandler> partHandlers = new HashMap<IWorkbenchPart, IGazeHandler>();

    public WorkbenchGazeHandler(IWorkbench workbench) {
    	this.workbench = workbench;
    	this.workbenchPartListener = new PartListener(workbench, this);
    	
    	IWorkbenchPage page = this.workbench.getActiveWorkbenchWindow().getActivePage();
    	for(IViewReference viewRef : page.getViewReferences()) {
    		IViewPart viewPart = (IViewPart)viewRef.getPart(true);
    		if(viewPart != null) {
    			this.addView(viewPart);
    		}
    	}
    	for(IEditorReference editorRef : page.getEditorReferences()) {
    		IEditorPart editorPart = (IEditorPart)editorRef.getPart(true);
    		if(editorPart != null) {
    			this.addEditor(editorPart);
    		}
    	}
    }

	@Override
	public boolean containsGaze(int absoluteX, int absoluteY) {
		Rectangle viewScreenBounds = workbench.getActiveWorkbenchWindow().getShell().getBounds();
		Point screenPos = workbench.getActiveWorkbenchWindow().getShell().toDisplay(0, 0);
		viewScreenBounds.x = screenPos.x;
		viewScreenBounds.y = screenPos.y;
		
		// TODO: inefficient code that loops through every workbench part. 
		// Needed to handle the case of a workbench part being in a different shell
		// A simple bounds check of the root shell fails to find it
		for(IWorkbenchPart part : this.partHandlers.keySet()) {
			if(this.partHandlers.get(part).containsGaze(absoluteX, absoluteY)) {
				return true;
			}
		}
		
		if (workbench.getActiveWorkbenchWindow().getShell().isVisible() && viewScreenBounds.contains(absoluteX, absoluteY)) {
			return true;
	    }
	    else {
	    	return false;
	    }
	}

    @Override	
    public IGazeResponse handleGaze(int absoluteX, int absoluteY, final Gaze gaze) {
    	for(IWorkbenchPart part : this.partHandlers.keySet()) {
			if(this.partHandlers.get(part).containsGaze(absoluteX, absoluteY)) {
				return this.partHandlers.get(part).handleGaze(absoluteX, absoluteY, gaze);
			}
		}
    	
        return new IGazeResponse() {	
            @Override	
            public String getName() {	
                return "Workbench";	
            }	

            @Override	
            public Gaze getGaze() {	
                return gaze;	
            }	

            @Override	
            public IGazeHandler getGazeHandler() {	
                return WorkbenchGazeHandler.this;	
            }	

            @Override	
            public String getGazeType() {	
                // TODO Auto-generated method stub	
                return "shell";	
            }	
        };	
    }
    

	public void addEditor(IEditorPart editorPart) {
		if (!partHandlers.containsKey(editorPart)) {
			StyledText styledText = (StyledText) editorPart.getAdapter(Control.class);
			if (styledText != null) {
				partHandlers.put(editorPart, new StyledTextGazeHandler(styledText, editorPart));
			}
			ITrace.getDefault().setActiveEditor(editorPart);
		}
	}

	public void removeEditor(IEditorPart editorPart) {
		partHandlers.remove(editorPart);
		ITrace.getDefault().removeEditor(editorPart);
	}
	
	public void addView(IViewPart viewPart) {
		if(!partHandlers.containsKey(viewPart)) {
			if(viewPart instanceof ProjectExplorer) {
				ProjectExplorer projectExplorerPart = (ProjectExplorer)viewPart;
				Tree target = (Tree) projectExplorerPart.getCommonViewer().getControl();
				partHandlers.put(viewPart, new ProjectExplorerGazeHandler(target, viewPart));
			}
		}
	}
	
	public void removeViewPart(IViewPart viewPart) {
		partHandlers.remove(viewPart);
	}
	
	public void dispose() {
		for(IWorkbenchPart part : partHandlers.keySet()) {			
			if(part instanceof IEditorPart) {
				ITrace.getDefault().removeEditor((IEditorPart)part);				
			}
		}
	}
}
