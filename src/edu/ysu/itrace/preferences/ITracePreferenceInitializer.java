package edu.ysu.itrace.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import edu.ysu.itrace.ITrace;

public class ITracePreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = ITrace.getDefault().getPreferenceStore();
		store.setDefault(ITracePreferenceConstants.PREF_SOCKET_PORT_NUMBER, 8008);
	}

}
