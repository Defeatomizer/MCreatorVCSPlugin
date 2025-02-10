/*
 * MCreator VCS plugin
 * Copyright (C) 2020 Pylo and contributors
 * Copyright (C) 2023, Defeatomizer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.mcreator.vcs.ui.actions.impl;

import net.mcreator.ui.MCreatorTabs;
import net.mcreator.ui.action.ActionRegistry;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.variants.modmaker.ModMaker;
import net.mcreator.vcs.ui.component.ViewWrapper;
import net.mcreator.vcs.ui.workspace.WorkspacePanelVCS;
import net.mcreator.vcs.workspace.WorkspaceVCS;

import javax.swing.*;

public class UnlinkVCSAction extends VCSAction {

	public UnlinkVCSAction(ActionRegistry actionRegistry) {
		super(actionRegistry, L10N.t("action.vcs.unlink"), e -> {
			int n = JOptionPane.showConfirmDialog(actionRegistry.getMCreator(), L10N.t("dialog.vcs.unlink.message"),
					L10N.t("common.confirmation"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (n == JOptionPane.YES_OPTION) {
				WorkspaceVCS.removeVCSWorkspace(actionRegistry.getMCreator().getWorkspace());
				actionRegistry.getActions().stream().filter(action -> action instanceof VCSAction)
						.forEach(action -> ((VCSAction) action).vcsStateChanged());
				if (actionRegistry.getMCreator() instanceof ModMaker mod) {
					mod.getWorkspacePanel().switchToVerticalTab("mods");
				} else {
					for (MCreatorTabs.Tab tab : actionRegistry.getMCreator().getTabs().getTabs().stream().toList()) {
						if (tab.getContent() instanceof ViewWrapper wrap
								&& wrap.getViewContents() instanceof WorkspacePanelVCS) {
							wrap.setCloseable(true);
							actionRegistry.getMCreator().getTabs().closeTab(tab);
						}
					}
				}
			}
		});
		setIcon(UIRES.get("16px.vcs_unlink"));
	}

}
