package org.itrace.gaze.handlers;

import java.io.IOException;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
//import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.itrace.Gaze;
import org.itrace.gaze.IStyledTextGazeResponse;

/**
 * Implements the gaze handler interface for a StyledText widget.
 */
public class StyledTextGazeHandler implements IGazeHandler {
    private StyledText targetStyledText;
    private IEditorPart editor;
    private ProjectionViewer projectionViewer;

    /**
     * Constructs a new gaze handler for the target StyledText object
     */
    public StyledTextGazeHandler(Object target, IEditorPart editor) {
        this.targetStyledText = (StyledText) target;
        this.editor = editor;
        projectionViewer = (ProjectionViewer) editor.getAdapter(ITextOperationTarget.class);
    }
    
    @Override
    public boolean containsGaze(int absoluteX, int absoluteY) {
    	Rectangle viewScreenBounds = targetStyledText.getBounds();
		Point screenPos = targetStyledText.toDisplay(0, 0);
		viewScreenBounds.x = screenPos.x;
		viewScreenBounds.y = screenPos.y;
	
		if (targetStyledText.isVisible() && viewScreenBounds.contains(absoluteX, absoluteY)) {
			return true;
	    }
	    else {
	    	return false;
	    }	    	
    }

    @Override
    public IStyledTextGazeResponse handleGaze(int absoluteX, int absoluteY, final Gaze gaze) {
        final int lineIndex;
        final int col;
        final Point absoluteLineAnchorPosition;
        final String name;
        final int lineHeight;
        final int fontHeight;
        final String path;

        try {
        	int relativeX = absoluteX - targetStyledText.toDisplay(0, 0).x;
        	int relativeY = absoluteY - targetStyledText.toDisplay(0, 0).y;
        	
            // Get the actual offset of the current line from the top
            // Allows code folding to be taken into account
            int foldedLineIndex = targetStyledText.getLineIndex(relativeY);
            int lineOffset = targetStyledText.getOffsetAtLine(foldedLineIndex);   
            
            int offset = targetStyledText.getOffsetAtPoint(new Point(relativeX, relativeY));
            if(offset == -1) {
            	return null;
            }

            col = offset - lineOffset + 1;
            lineIndex = projectionViewer.widgetLine2ModelLine(foldedLineIndex);
            
            // (0, 0) relative to the control in absolute screen
            // coordinates.
            Point relativeRoot = new Point(absoluteX - relativeX, absoluteY - relativeY);
            
            // Top-left position of the first character on the line in
            // relative coordinates.
            Point lineAnchorPosition = targetStyledText.getLocationAtOffset(lineOffset);
            
            // To absolute.
            absoluteLineAnchorPosition = new Point(lineAnchorPosition.x + relativeRoot.x, lineAnchorPosition.y + relativeRoot.y);

            lineHeight = targetStyledText.getLineHeight();
            fontHeight = targetStyledText.getFont().getFontData()[0].getHeight();
            
            path =  ((IFileEditorInput) editor.getEditorInput()).getFile().getFullPath().toFile().getCanonicalPath();
            int splitLength = path.split("\\\\").length;
            name = path.split("\\\\")[splitLength-1];
        
        } catch (IOException e) {
			// Getting file path from editor failed
			e.printStackTrace();
			return null;
		}

        /*
         * This anonymous class just grabs the variables marked final
         * in the enclosing method and returns them.
         */
        return new IStyledTextGazeResponse() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getGazeType() {
            	String type = path;
            	int dotIndex;
            	for(dotIndex=0; dotIndex<type.length();dotIndex++) {
            		if(path.charAt(dotIndex) == '.') {
            			break;
            		}
            	}            	
            	if(dotIndex+1 == type.length())
            	{
            		return "text";
            	}
            	type = type.substring(dotIndex+1);
            	return type;
            }

            @Override
            public int getLineHeight() {
                return lineHeight;
            }

            @Override
            public int getFontHeight() {
                return fontHeight;
            }

            @Override
            public Gaze getGaze() {
                return gaze;
            }

            public IGazeHandler getGazeHandler() {
                return StyledTextGazeHandler.this;
            }

            @Override
            public int getLine() {
                return lineIndex + 1;
            }

            @Override
            public int getCol() {
                return col;
            }

            // Write out the position at the top-left of the first
            // character in absolute screen coordinates.
            @Override
            public int getLineBaseX() {
                return absoluteLineAnchorPosition.x;
            }

            @Override
            public int getLineBaseY() {
                return absoluteLineAnchorPosition.y;
            }

            @Override
            public String getPath() {
                return path;
            }

        };
    }
}