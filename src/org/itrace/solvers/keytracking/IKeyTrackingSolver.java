package org.itrace.solvers.keytracking;

import org.itrace.solvers.ISolver;

public interface IKeyTrackingSolver extends ISolver {

	void config(String sessionId);
	// Marker
}