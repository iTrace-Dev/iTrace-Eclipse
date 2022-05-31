/********************************************************************************************************************************************************
* @file IGazeResponse.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
package org.itrace.gaze;

import org.itrace.Gaze;
import org.itrace.gaze.handlers.IGazeHandler;

/**
 * Defines a response to a gaze event. Returned by objects implementing
 * IGazeHandler.
 */
public interface IGazeResponse {

    /**
     * Returns the name of the artifact under the gaze.
     */
    public String getName();

    /**
     * Returns the type of artifact.
     */
    public String getGazeType();

    /**
     * Returns the gaze object from which the response originated.
     */
    public Gaze getGaze();

    /**
     * Returns the gaze handler.
     */
    public IGazeHandler getGazeHandler();
}
