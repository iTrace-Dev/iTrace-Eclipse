package edu.ysu.itrace;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.*;

import javax.swing.JPanel;
import javax.swing.JWindow;

import org.eclipse.swt.graphics.Point;
public class GazeCursorWindow extends JWindow{
	private class GazeCursorPanel extends JPanel{
		public GazeCursorPanel() {
			setSize(16,16);
		}
		@Override
		public void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.setStroke(new BasicStroke(3));
			super.paintComponent(g);
			g2d.setColor(new Color(0,0,255,255));
			g2d.drawOval(getX()+3, getY()+3, 12, 12);
		}
	}
		
	    private Point centre = null;
	    private int MAX = 15;
	    private List xPoints = new ArrayList<>();
	    private List yPoints = new ArrayList<>();
	    double totalX;
	    double totalY;
	    private int counter;

	    public GazeCursorWindow() {
	        super.setLocation(345, 332);
	        setSize(16,16);
	        setBackground(new Color(0,0,255,0));
	        JPanel gazeCursorPanel = new GazeCursorPanel();
	        gazeCursorPanel.setOpaque(false);
	        add(gazeCursorPanel);
	        centre = new Point(8,8);
	        setAlwaysOnTop(true);
	        totalX = 0;
	        totalY = 0;
	        counter = 0;
	    }

	    public void setLocation(int x, int y) {
	    	
	    	/* Need to think of a better way to smoothen the crosshair. It works for now 
	    	 * 
	    	 * if(x < 0 || y < 0) return;
	    	double avgX = 0;
	    	double avgY= 0;
	    	totalX += x;
	    	totalY += y;
	    	counter++;
	    	if(counter == 15) {
	    		avgX = totalX/15;
	    		avgY = totalY/15;
	    		counter = 0;
	    	}
	    	super.setLocation((int)avgX - centre.x, (int)avgY-centre.y);
	    	xPoints.add(x);
	    	yPoints.add(y);
	    	
	    	if(xPoints.size() == MAX) {
	    		double avgX = totalX/MAX;
	    		double avgY = totalY/MAX;
	    		super.setLocation((int)avgX, (int)avgY);
	    		totalX -= (int)xPoints.get(xPoints.size()-1);
	    		totalY -= (int)yPoints.get(yPoints.size()-1);
	    		xPoints.remove(xPoints.size()-1);
	    		yPoints.remove(yPoints.size()-1);
	    	}
	    	*/
	    	super.setLocation(x - centre.x, y - centre.y);
	        
	    }
		
	}
