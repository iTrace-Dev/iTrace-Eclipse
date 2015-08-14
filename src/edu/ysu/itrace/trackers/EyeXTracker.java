package edu.ysu.itrace.trackers;

import java.awt.Toolkit;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import edu.ysu.itrace.Gaze;
import edu.ysu.itrace.trackers.IEyeTracker;
import edu.ysu.itrace.exceptions.CalibrationException;

/**
 * Provides an interface for the EyeX eye tracker.
 *
 */
//NOTICED ISSUES TO ADDRESS
//possibly doesn't reset eyeTrackingStarted boolean when stoptracking is called/other things that need reset
public class EyeXTracker implements IEyeTracker {

	private double xDrift = 0;
    private double yDrift = 0;
    private LinkedBlockingQueue<Gaze> gaze_points = new LinkedBlockingQueue<Gaze>();
	private LinkedBlockingQueue<Gaze> recentGazes = new LinkedBlockingQueue<Gaze>();
	private Calibrator calibrator;
	private boolean eyeTrackingStarted = false;
    
    /**
     * Load the libraries that are necessary to communicate with the EyeX eye tracker.
     * It is important to first load the Tobii Client library and then the
     * EyeXTracker library.
     */
    static {
    	//System.loadLibrary("Tobii.EyeX.Client");
        System.loadLibrary("EyeXTracker");
    }

    /**
     * Define all the native methods
     */
    private native void disconnectEyeTracker();
    private native boolean connectEyeTracker();
    private native boolean register();
    
    /**
     * Test methods if this is started without the Eclipse plugin.
     * @param args
     * @throws InterruptedException
     * @throws IOException 
     * @throws CalibrationException 
     */
    public static void main(String... args) throws InterruptedException, IOException, CalibrationException {
    	EyeXTracker tracker = new EyeXTracker();
    	tracker.calibrate();
    	tracker.getCalibrator().displayCrosshair(true);
		tracker.startTracking();
		System.out.println("connected");	
    }
    
    @SuppressWarnings("serial")
	private static class Calibrator extends edu.ysu.itrace.Calibrator {

		public Calibrator() throws IOException {
			super();
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void startCalibration() throws Exception {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void stopCalibration() throws Exception {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected void useCalibrationPoint(double x, double y) throws Exception {
			// TODO Auto-generated method stub
			
		}
		
		protected void displayCalibrationStatus() throws Exception {
			// TODO Auto-generated method stub
        }
    	
    }
    
    /**
     * Called from the native C code whenever a new gaze point is received.
     * @param x
     * @param y
     * @param timestamp
     */
    public void callback(double x, double y, long timestamp) {
    	//Drift
        x += xDrift;
        y += yDrift;
        
        //Save original values
        double orig_x = x;
        double orig_y = y;
        
        //Clamp x values to [0.0, 1.0].
        double screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        double screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        
        x = x / screenWidth;
        y = y / screenHeight;
        
        if (x >= 1.0)
            x = 1.0;
        
        if (x >= 1.0)
           x = 1.0;
        
        double x_mod = x;
        double y_mod = y;
        
        try {
            Gaze gaze = new Gaze(x, x, y, y, Double.MIN_VALUE, Double.MIN_VALUE,
            		Double.MIN_VALUE, Double.MIN_VALUE, new Date(timestamp / 1000));
            if (recentGazes.size() >= 15)
                recentGazes.remove();
            recentGazes.add(gaze);

            for (Object curObj : recentGazes.toArray()) {
                Gaze curGaze = (Gaze) curObj;
                x_mod += curGaze.getLeftX();
                y_mod += curGaze.getRightY();
            }
            x_mod /= recentGazes.size() + 1;
            y_mod /= recentGazes.size() + 1;

            Gaze modifiedGaze = new Gaze(x_mod, x_mod, y_mod,
                                         y_mod, Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE,
                                         new Date(timestamp / 1000));

            if (eyeTrackingStarted)  {
            	gaze_points.put(modifiedGaze);
            }    
        } catch (InterruptedException e) {
            //Ignore this point.
        }

        //Update crosshair position
        int screen_x = (int) (orig_x);
        int screen_y = (int) (orig_y);
        calibrator.moveCrosshair(screen_x, screen_y);
    }
    
	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void clear() {
		gaze_points = new LinkedBlockingQueue<Gaze>();
	}
	
	@Override
	public void calibrate() throws CalibrationException {
		try {
			calibrator = new Calibrator();
			register();
			boolean success = connectEyeTracker();
			if (success) {
				System.out.println("Sucessfully connected");
			} else {
				System.out.println("Not yet connected");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void startTracking() throws IOException {
		eyeTrackingStarted = true;
	}
	
	@Override
	public void stopTracking() throws IOException {
		disconnectEyeTracker();
	}
	
	public Calibrator getCalibrator() {
		return calibrator;
	}
	
	@Override
	public Gaze getGaze() {
		return gaze_points.poll();
	}
	
	@Override
	public void displayCrosshair(boolean enabled) {
		calibrator.displayCrosshair(enabled);
	}
	
	@Override
	public void setXDrift(int drift) {
		xDrift = ((double) drift) / 100;
	}
	
	@Override
	public void setYDrift(int drift) {
		yDrift = ((double) drift) / 100;
	}
    
}