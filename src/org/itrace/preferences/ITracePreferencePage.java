/********************************************************************************************************************************************************
* @file ITracePreferencePage.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
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
