package edu.ysu.itrace;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import edu.ysu.itrace.gaze.IStyledTextGazeResponse;
import edu.ysu.itrace.gaze.handlers.StyledTextGazeHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.projection.ProjectionViewer;

public class TokenHighlighter implements PaintListener, EventHandler {
	
	private IEditorPart editorPart;
	private StyledText styledText;
	private ProjectionViewer projectionViewer;
	private Rectangle boundingBox;
	private LinkedBlockingQueue<Gaze> gazeQueue;
	private StyledTextGazeHandler gazeHandler;
	private Point[] points;
	private int pointIndex;
	private int numberOfPoints;
	private int nulls;
	private boolean show;
	private IEventBroker eventBroker;
	/*additional information to:
 	 * avoid performance issues by handling every other gaze
	 * keep a rolling average of screen position to mitigate effects of small eye movements
	 */
	boolean alternate = false;
	List<Integer> xPoints = new ArrayList<>();
    List<Integer> yPoints = new ArrayList<>();
    int totalX = 0;
    int totalY = 0;
	
	
	@Override
	public void paintControl(PaintEvent pe) {
		if(boundingBox != null && show){
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
	
	public void update(int lineIndex, int column, int x, int y){
        int lineOffset = styledText.getOffsetAtLine(lineIndex);
		String lineContent = styledText.getLine(lineIndex);
		boundingBox = getBoundingBox(lineOffset,lineContent,x,y);
		
		styledText.redraw();
	}
		
	public boolean boundingBoxContains(int x,int y){
		if(boundingBox != null) return boundingBox.contains(x,y);
		else return false;
	}
	
	public int getOffsetAtPoint(Point point){
		try{
			int offset = styledText.getOffsetAtLocation(point);
			return offset;
		}
		catch(Exception e){
			return -1;
		}
	}
		
	
	public void setShow(boolean show){
		this.show = show;
		//if(show) this.start();
	}
	
	
	private Rectangle getBoundingBox(int lineOffset, String lineContent, int x, int y){
		Rectangle box = null;
		points[pointIndex] = new Point(x,y);
		pointIndex++;
		if(pointIndex > numberOfPoints-1) pointIndex = pointIndex%numberOfPoints;
		if(boundingBox != null && containsPoints(boundingBox)) return boundingBox;
		int startOffset = 0;
		int endOffset;
		while(startOffset < lineContent.length()){
			while(startOffset < lineContent.length() && checkChar(lineContent.charAt(startOffset))) 
				startOffset++;
			endOffset = startOffset;
			while(endOffset < lineContent.length()-1 && !checkChar(lineContent.charAt(endOffset+1))) 
				endOffset++;
			box = styledText.getTextBounds(lineOffset+startOffset, lineOffset+endOffset);
			
			if(containsPoints(box)) break;
			startOffset = endOffset+1;
		}
		if(box != null && !containsPoints(box)){
			box = null;
		}
		return box;
	}
	
	private boolean containsPoints(Rectangle box){
		for(Point p: points){
			if(p != null) { 
				if(!box.contains(p)) {
					return false;
				}
			}
		}
		return true;
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
		ITextOperationTarget t = (ITextOperationTarget) editorPart.getAdapter(ITextOperationTarget.class);
		if(t instanceof ProjectionViewer) projectionViewer = (ProjectionViewer) t;
		this.styledText.addPaintListener(this);
		this.gazeHandler = new StyledTextGazeHandler(styledText);
		this.show = show;
		this.numberOfPoints = 10;
		this.points = new Point[numberOfPoints];
		this.pointIndex = 0;
		this.nulls = 0;
		this.eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
		this.eventBroker.subscribe("iTrace/newstresponse", this);
	}



	@Override
	public void handleEvent(Event event) {
		String[] propertyNames = event.getPropertyNames();
		IStyledTextGazeResponse response = (IStyledTextGazeResponse)event.getProperty(propertyNames[0]);
		xPoints.add((int)(response.getGaze().getX()));
		yPoints.add((int)(response.getGaze().getY()));
		totalX += (int) (response.getGaze().getX());
		totalY += (int) (response.getGaze().getY());
		
		if(xPoints.size() > this.numberOfPoints) {
			totalX -= xPoints.get(0);
			totalY -= yPoints.get(0);
			xPoints.remove(0);
			yPoints.remove(0);
		}
		
        if(styledText.isDisposed()) return;
        Rectangle editorBounds = styledText.getBounds();
        Point screenPos = styledText.toDisplay(0, 0);
        editorBounds.x = screenPos.x;
        editorBounds.y = screenPos.y;
        int screenX = totalX / xPoints.size();
        int screenY = totalY / yPoints.size();
        alternate = !alternate;
        if(editorBounds.contains(screenX, screenY) && alternate){
        	int relativeX = screenX-editorBounds.x;
        	int relativeY = screenY-editorBounds.y;
        	IStyledTextGazeResponse response_new = gazeHandler.handleGaze(screenX, screenY, relativeX, relativeY, response.getGaze());
            	if(response_new != null && !boundingBoxContains(relativeX,relativeY)){
            		update(response_new.getLine()-1,response_new.getCol(), relativeX, relativeY);
            	}
        }
		
	}
}
