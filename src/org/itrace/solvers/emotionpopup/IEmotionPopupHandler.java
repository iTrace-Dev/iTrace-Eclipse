package org.itrace.solvers.emotionpopup;

import org.itrace.gaze.IGazeResponse;
import org.itrace.solvers.ISolver;

public interface IEmotionPopupHandler extends ISolver {
    void writeResponse(IGazeResponse response, String emotion, String[] options);
    
    void disable();
    
    void enable();

	void config(String sessionId);
}