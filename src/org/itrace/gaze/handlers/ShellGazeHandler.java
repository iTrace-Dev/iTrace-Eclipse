package org.itrace.gaze.handlers;

import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.itrace.Gaze;
import org.itrace.gaze.IGazeResponse;

public class ShellGazeHandler implements IGazeHandler {
	protected String name = "Anonymous Shell";
	protected Shell shell;


    public ShellGazeHandler(Shell shell) {
    	this.shell = shell;
    	
    	Queue<Control[]> controlQueue = new LinkedList<Control[]>();
    	controlQueue.add(this.shell.getChildren());
    	
    	// TODO: use this combined with being able to navigate workbench for parts and editors
    	// to get the initial things we care about and relate it to stuff like Markers, Editors, ect..
    	while(controlQueue.isEmpty() == false) {
    		Control[] controls = controlQueue.remove();
    		
    		for(Control control : controls) {
        		if(control instanceof Composite) {
        			Control[] children = ((Composite)control).getChildren();
        			if(children.length > 0) {
        				controlQueue.add(children);
        			}
        		}
        		
    		}
    	}
    }

	@Override
	public boolean containsGaze(int absoluteX, int absoluteY) {
		Rectangle viewScreenBounds = shell.getBounds();
		Point screenPos = shell.toDisplay(0, 0);
		viewScreenBounds.x = screenPos.x;
		viewScreenBounds.y = screenPos.y;
		
		if (shell.isVisible() && viewScreenBounds.contains(absoluteX, absoluteY)) {
			return true;
	    }
	    else {
	    	return false;
	    }
	}

    @Override	
    public IGazeResponse handleGaze(int absoluteX, int absoluteY, final Gaze gaze) {	
        return new IGazeResponse() {	
            @Override	
            public String getName() {	
                return name;	
            }	

            @Override	
            public Gaze getGaze() {	
                return gaze;	
            }	

            @Override	
            public IGazeHandler getGazeHandler() {	
                return ShellGazeHandler.this;	
            }	

            @Override	
            public String getGazeType() {	
                // TODO Auto-generated method stub	
                return "shell";	
            }	
        };	
    }
    

}
