package org.itrace.solvers.externallauncher;

import org.itrace.solvers.ISolver;
import java.util.List;

public interface IExternalLauncher extends ISolver {
    void start();

	void config(String sessionId);
}

