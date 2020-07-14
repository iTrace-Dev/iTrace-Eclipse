package org.itrace.gaze.handlers;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IViewPart;
import org.itrace.Gaze;
import org.itrace.gaze.IGazeResponse;

public class ProjectExplorerGazeHandler implements IGazeHandler {
	private String name;	
    private Tree tree;

    public ProjectExplorerGazeHandler(Tree target, IViewPart partRef) {	
        this.name = partRef.getTitle();	
        this.tree = (Tree) target;	
    }
    
    @Override
    public boolean containsGaze(int absoluteX, int absoluteY) {
		Rectangle viewScreenBounds = tree.getBounds();
		Point screenPos = tree.toDisplay(0, 0);
		viewScreenBounds.x = screenPos.x;
		viewScreenBounds.y = screenPos.y;
		
		if (tree.isVisible() && viewScreenBounds.contains(absoluteX, absoluteY)) {
			return true;
	    }
	    else {
	    	return false;
	    }
    }

    @Override	
    public IGazeResponse handleGaze(int absoluteX, int absoluteY, final Gaze gaze) {
    	int relativeX = absoluteX - tree.toDisplay(0, 0).x;
    	int relativeY = absoluteY - tree.toDisplay(0, 0).y;
    	
    	tree.getItem(new Point(relativeX, relativeY));
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
                return ProjectExplorerGazeHandler.this;	
            }	

            @Override	
            public String getGazeType() {	
                // TODO Auto-generated method stub	
                return "view_part";	
            }	
        };	
    }
}
