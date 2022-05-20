/********************************************************************************************************************************************************
* @file Gaze.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
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
