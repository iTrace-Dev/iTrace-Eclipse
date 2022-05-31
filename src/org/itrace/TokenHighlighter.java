/********************************************************************************************************************************************************
* @file TokenHighlighter.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
package org.itrace;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import org.eclipse.ui.IEditorPart;
import org.itrace.gaze.IGazeResponse;
import org.itrace.gaze.IStyledTextGazeResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class TokenHighlighter implements PaintListener {
	
	private IEditorPart editorPart;
	private StyledText styledText;
	private Rectangle boundingBox;
	private int numberOfPoints;
	private int validPointThreshold;
	private boolean show;
	private Semaphore semaphore;
	/*additional information to:
 	 * avoid performance issues by handling every other gaze
	 * keep a rolling average of screen position to mitigate effects of small eye movements
	 */
	List<Integer> xPoints = new ArrayList<>();
    List<Integer> yPoints = new ArrayList<>();
    int totalX = 0;
    int totalY = 0;
    
    int pointCount = 0;	
	
	@Override
	public void paintControl(PaintEvent pe) {
		if(boundingBox != null && show && pointCount > validPointThreshold){
			pe.gc.setBackground(new Color(pe.gc.getDevice(),255,215,0));
			pe.gc.setForeground(new Color(pe.gc.getDevice(),0,0,0));
			pe.gc.drawRectangle(boundingBox);
			pe.gc.setAlpha(125);
			pe.gc.fillRectangle(boundingBox);
		}else if(boundingBox == null){
			boundingBox = new Rectangle(-1,-1,0,0);
			pe.gc.drawRectangle(boundingBox);
			pe.gc.setAlpha(125);
			pe.gc.fillRectangle(boundingBox);
		}
	}	
	
	public void redraw(){
		styledText.redraw();
	}
	
	public void update(int x, int y){
		int foldedLineIndex = styledText.getLineIndex(y);
		int lineOffset = styledText.getOffsetAtLine(foldedLineIndex);
		String lineContent = styledText.getLine(foldedLineIndex);
		boundingBox = getBoundingBox(lineOffset,lineContent,x,y);
		
		styledText.redraw();
	}
		
	public boolean boundingBoxContains(int x,int y){
		if(boundingBox != null) return boundingBox.contains(x,y);
		else return false;
	}
	
	public void clearHighlights() {
		try {
			semaphore.acquire();
			this.boundingBox = null;
			this.totalX = 0;
			this.totalY = 0;
			this.xPoints.clear();
			this.yPoints.clear();
			this.pointCount = 0;
			semaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		styledText.redraw();
	}
	
	public void setShow(boolean show){
		this.show = show;
	}	
	
	private Rectangle getBoundingBox(int lineOffset, String lineContent, int x, int y){
		Rectangle box = null;
		if(boundingBox != null && boundingBox.contains(x, y)) return boundingBox;
		int startOffset = 0;
		int endOffset;
		while(startOffset < lineContent.length()){
			while(startOffset < lineContent.length() && checkChar(lineContent.charAt(startOffset))) 
				startOffset++;
			endOffset = startOffset;
			while(endOffset < lineContent.length()-1 && !checkChar(lineContent.charAt(endOffset+1))) 
				endOffset++;
			box = styledText.getTextBounds(lineOffset+startOffset, lineOffset+endOffset);
			
			if(box.contains(x, y)) break;
			startOffset = endOffset+1;
		}
		if(box != null && !box.contains(x, y)){
			box = null;
		}
		return box;
	}
	
	private boolean checkChar(char c){
		char[] delimeters = {' ', '\t','(',')','[',']','{','}','.',','};
		for(char delimeter: delimeters){
			if(c == delimeter) return true;
		}
		return false;
	}
	
	public TokenHighlighter(IEditorPart editorPart, boolean show){
		this.editorPart = editorPart;
		this.styledText = (StyledText)this.editorPart.getAdapter(Control.class);
		this.styledText.addPaintListener(this);
		this.show = show;
		this.numberOfPoints = 10;
		this.validPointThreshold = 3;
		this.semaphore = new Semaphore(1);
	}

	public void handleGaze(IGazeResponse rawResponse) {
		try {
			semaphore.acquire();
			if(rawResponse != null && rawResponse instanceof IStyledTextGazeResponse) {
				IStyledTextGazeResponse response = (IStyledTextGazeResponse)rawResponse;
				xPoints.add((int)(response.getGaze().getX()));
				yPoints.add((int)(response.getGaze().getY()));
				totalX += (int) (response.getGaze().getX());
				totalY += (int) (response.getGaze().getY());
				
				pointCount += 1;
			} else {
				xPoints.add(null);
				yPoints.add(null);
			}
			
			if(xPoints.size() > this.numberOfPoints) {
				Integer x = xPoints.get(0);
				totalX -= x == null ? 0 : x;
				Integer y = yPoints.get(0);
				totalY -= y == null ? 0 : y;
				xPoints.remove(0);
				yPoints.remove(0);
				
				if(x != null) {
					pointCount -= 1;
				}
			}
			semaphore.release();
			
	        if(styledText.isDisposed()) return;
	        Rectangle editorBounds = styledText.getBounds();
	        Point screenPos = styledText.toDisplay(0, 0);
	        editorBounds.x = screenPos.x;
	        editorBounds.y = screenPos.y;
	        int screenX = 0;
	        int screenY = 0;
	        if(pointCount > 0) {
		        screenX = totalX / pointCount;
		        screenY = totalY / pointCount;
	        }
	        if(editorBounds.contains(screenX, screenY)){
	        	int relativeX = screenX-editorBounds.x;
	        	int relativeY = screenY-editorBounds.y;
	        	update(relativeX, relativeY);
	        }
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
}
