package org.itrace.gaze.handlers;

import org.itrace.Gaze;
import org.itrace.gaze.IGazeResponse;



/**
 * Defines an object that can receive a point of gaze and respond in a
 * specialized way.
 */
public interface IGazeHandler {
	
	public boolean containsGaze(int absoluteX, int absoluteY);
	
    /**
     * Handles the specified gaze at the specified x and y coordinates relative
     * to the target object. Return value may be null if the gaze is not
     * meaningful to the target.
     */
    public IGazeResponse handleGaze(int absoluteX, int absoluteY, Gaze gaze);
    
}
