/********************************************************************************************************************************************************
* @file PerspectiveFactory.java
*
* @Copyright (C) 2022 i-trace.org
*
* This file is part of iTrace Infrastructure http://www.i-trace.org/.
* iTrace Infrastructure is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
* iTrace Infrastructure is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with iTrace Infrastructure. If not, see <https://www.gnu.org/licenses/>.
********************************************************************************************************************************************************/
package org.itrace;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * Manages the Eclipse perspective for this plugin.
 */
public class PerspectiveFactory implements IPerspectiveFactory {
    private static final float PROJECT_EXPOLORER_RATIO = 0.20f;
    private static final float BOTTOM_FOLDER_RATIO = 0.75f;

    private static final String BOTTOM_FOLDER_ID = "org.itrace.BottomFolder";
    private static final String CONTROL_VIEW = "org.itrace.controlview";


    @Override
    public void createInitialLayout(IPageLayout layout) {
        // set up package explorer
        layout.addView(IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.LEFT,
                PROJECT_EXPOLORER_RATIO, layout.getEditorArea());

        // set up bottom folder
        IFolderLayout bottomFolder = layout.createFolder(BOTTOM_FOLDER_ID,
                IPageLayout.BOTTOM, BOTTOM_FOLDER_RATIO,
                layout.getEditorArea());
        bottomFolder.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottomFolder.addView(IPageLayout.ID_PROP_SHEET);
        bottomFolder.addView(IPageLayout.ID_PROGRESS_VIEW);
        bottomFolder.addView(CONTROL_VIEW);
    }
}
