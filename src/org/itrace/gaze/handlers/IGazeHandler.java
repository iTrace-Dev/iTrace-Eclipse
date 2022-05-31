/********************************************************************************************************************************************************
* @file IGazeHandler.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
package org.itrace.gaze.handlers;

import org.itrace.Gaze;
import org.itrace.gaze.IGazeResponse;



/**
 * Defines an object that can receive a point of gaze and respond in a
 * specialized way.
 */
public interface IGazeHandler {

    /**
     * Handles the specified gaze at the specified x and y coordinates relative
     * to the target object. Return value may be null if the gaze is not
     * meaningful to the target.
     */
    public IGazeResponse handleGaze(int absoluteX, int absoluteY,
            int relativeX, int relativeY, Gaze gaze);
    
}
