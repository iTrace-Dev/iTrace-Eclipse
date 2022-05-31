/********************************************************************************************************************************************************
* @file IStyledTextGazeResponse.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
package org.itrace.gaze;

/**
 * Defines an interface for gazes falling on StyledText widgets 
 */
public interface IStyledTextGazeResponse extends IGazeResponse {
    /**
     * Return the OS dependent path of the file in the editor
     */
    public String getPath();

    /**
     * Return the height (in pixels) of lines of text
     */
    public int getLineHeight();

    /**
     * Return the font size
     */
    public int getFontHeight();

    /**
     * Return the line where the gaze fell
     */
    public int getLine();

    /**
     * Return the column where the gaze fell
     */
    public int getCol();

    /**
     * Return the x position of the first character on line
     */
    public int getLineBaseX();

    /**
     * Return the y position of the first character on the line
     */
    public int getLineBaseY();
}
