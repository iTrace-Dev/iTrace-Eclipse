/********************************************************************************************************************************************************
* @file ISolver.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
package org.itrace.solvers;

import org.itrace.gaze.IGazeResponse;

/**
 * Defines a minimal interface for passing data to a solver.
 */
public interface ISolver {
    // This interface is intended to provide only general
    // functionality needed in the event loop. Additional
    // features, such as retrieving results, should be
    // defined in another, more specific, interface.

    /**
     * A name of the solver suitable to display to the user.
     */
    public String friendlyName();

    /**
     * Configure the export filename.
     */
    public void config(String filename, String sessionId);
    
    /**
     * Launch dialog to display the export filename.
     */
    public void displayExportFile();

    /**
     * Any initialization work with side effects, such as opening files. This
     * method should very probably be called before calling process or dispose.
     */
    public void init();

    /**
     * Called to process new gazes.
     */
    public void process(IGazeResponse response);

    /**
     * Frees any resources. It is very likely a bad idea to process new data
     * after calling dispose. Not sure if we need this, either.
     */
    public void dispose();
}
