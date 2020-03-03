package org.itrace.solvers;

import org.itrace.Gaze;
import org.itrace.gaze.IGazeResponse;

/**
 * Defines an interface for solvers that dump data to files.
 */
public interface IFileExportSolver extends ISolver {
    /**
     * Get the filename that would be used.
     * @return a string containing the export path
     */
    public String getFilename();

	void process(IGazeResponse response, Gaze gaze);
}
