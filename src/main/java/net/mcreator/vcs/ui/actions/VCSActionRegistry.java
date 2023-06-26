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

package net.mcreator.vcs.ui.actions;

import net.mcreator.ui.MCreator;
import net.mcreator.ui.action.BasicAction;
import net.mcreator.vcs.ui.actions.impl.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class VCSActionRegistry {

	private static final Map<File, VCSActionRegistry> vcsActions = new HashMap<>();

	public final BasicAction setupVCS;
	public final BasicAction unlinkVCS;
	public final BasicAction setupVCSOrSettings;
	public final BasicAction syncToRemote;
	public final BasicAction syncFromRemote;
	public final BasicAction showUnsyncedChanges;
	public final BasicAction rollbackUnsyncedChanges;
	public final BasicAction remoteWorkspaceSettings;

	public VCSActionRegistry(MCreator mcreator) {
		vcsActions.put(mcreator.getWorkspaceFolder(), this);

		this.setupVCS = new SetupVCSAction(mcreator.getActionRegistry());
		this.unlinkVCS = new UnlinkVCSAction(mcreator.getActionRegistry());
		this.setupVCSOrSettings = new SetupOrSettingsVCSAction(mcreator.getActionRegistry());
		this.syncToRemote = new SyncLocalWithRemoteAction(mcreator.getActionRegistry());
		this.syncFromRemote = new SyncRemoteToLocalAction(mcreator.getActionRegistry());
		this.showUnsyncedChanges = new ShowLocalChangesAction(mcreator.getActionRegistry());
		this.rollbackUnsyncedChanges = new RollbackLocalChangesAction(mcreator.getActionRegistry());
		this.remoteWorkspaceSettings = new VCSInfoSettingsAction(mcreator.getActionRegistry());
	}

	public static VCSActionRegistry get(MCreator mcreator) {
		return vcsActions.get(mcreator.getWorkspaceFolder());
	}

}
