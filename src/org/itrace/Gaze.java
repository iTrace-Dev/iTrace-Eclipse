package org.itrace;

import java.sql.Timestamp;
import java.util.Calendar;

public class Gaze {
    private double x;
    private double y;

    private long eventTime;
    private Calendar calendar;
    private long systemTime;
    private Timestamp timestamp;
    private String timestampString;

    public Gaze(double x, double y, long eventTime) {
    	this.x = x;
    	this.y = y;
    	this.eventTime = eventTime;
    	
	    this.calendar = Calendar.getInstance();
	    this.systemTime = System.currentTimeMillis();

		calendar.setTimeInMillis(systemTime);

		timestamp = new Timestamp(systemTime);

		timestampString = timestamp.toString();
		timestampString = timestampString.substring(0, 10) + "T" + timestampString.substring(11);
		if(calendar.get(Calendar.ZONE_OFFSET) < 0) {
			timestampString += "-";
		}
		else {
			timestampString += "+";
		}
		
		if(Math.abs((calendar.get(Calendar.ZONE_OFFSET)/3600000)) < 10) {
			timestampString += "0";
		}
		
		timestampString += Math.abs((calendar.get(Calendar.ZONE_OFFSET)/3600000)) + ":00";     
    }

    public double getX() {
        return x;
    }


    public double getY() {
        return y;
    }

    public long getEventTime() {
        return eventTime;
    }
    
    public String getTimestamp(){
    	return timestampString;
    }    
}
