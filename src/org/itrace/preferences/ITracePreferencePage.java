package org.itrace.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.itrace.ITrace;

public class ITracePreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	
	public ITracePreferencePage() {
		super(FLAT);
		setPreferenceStore(ITrace.getDefault().getPreferenceStore());
	}
	

	@Override
	protected void createFieldEditors() {

		Composite parent = getFieldEditorParent();
		
		addField(new IntegerFieldEditor(ITracePreferenceConstants.PREF_SOCKET_PORT_NUMBER, "&Socket Port Number", parent));
	}


	@Override
	public void init(IWorkbench arg0) {
		// TODO Auto-generated method stub
		
	}
}
