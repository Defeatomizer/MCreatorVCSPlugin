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

import net.mcreator.ui.MCreator;
import net.mcreator.ui.action.ActionRegistry;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.variants.modmaker.ModMaker;
import net.mcreator.vcs.ui.dialogs.VCSSetupDialogs;
import net.mcreator.vcs.ui.workspace.WorkspacePanelVCS;
import net.mcreator.vcs.workspace.VCSInfo;
import net.mcreator.vcs.workspace.WorkspaceVCS;

public class SetupVCSAction extends VCSAction {

	public SetupVCSAction(ActionRegistry actionRegistry) {
		super(actionRegistry, L10N.t("action.vcs.setup"),
				e -> setupVCSForWorkspaceIfNotYet(actionRegistry.getMCreator()));
		setIcon(UIRES.get("16px.vcs_setup"));
	}

	@Override public void setEnabled(boolean b) {
		super.setEnabled(b);
		setTooltip(prevTooltip);
	}

	@Override public boolean isEnabled() {
		return WorkspaceVCS.getVCSWorkspace(actionRegistry.getMCreator().getWorkspace()) == null;
	}

	public static boolean setupVCSForWorkspaceIfNotYet(MCreator mcreator) {
		if (WorkspaceVCS.getVCSWorkspace(mcreator.getWorkspace()) == null) {
			VCSInfo vcsInfo = VCSSetupDialogs.getVCSInfoDialog(mcreator, L10N.t("dialog.vcs.setup.message"));
			if (vcsInfo != null) {
				if (WorkspaceVCS.initNewVCSWorkspace(mcreator.getWorkspace(), vcsInfo, mcreator)) {
					if (!(mcreator instanceof ModMaker))
						new WorkspacePanelVCS(mcreator);
					mcreator.getActionRegistry().getActions().stream()
							.filter(action -> action instanceof VCSStateChangeListener)
							.forEach(action -> ((VCSStateChangeListener) action).vcsStateChanged());
					return true;
				}
			}
		} else {
			return true;
		}
		return false;
	}

}
