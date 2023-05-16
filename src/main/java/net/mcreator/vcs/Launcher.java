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

package net.mcreator.vcs;

import net.mcreator.plugin.JavaPlugin;
import net.mcreator.plugin.Plugin;
import net.mcreator.plugin.events.WorkspaceSelectorLoadedEvent;
import net.mcreator.plugin.events.workspace.MCreatorLoadedEvent;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.dialogs.file.FileDialogs;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.workspace.selector.WorkspaceSelector;
import net.mcreator.vcs.ui.actions.VCSActionRegistry;
import net.mcreator.vcs.ui.dialogs.VCSSetupDialogs;
import net.mcreator.vcs.ui.workspace.WorkspacePanelVCS;
import net.mcreator.vcs.util.CloneWorkspace;
import net.mcreator.vcs.workspace.VCSInfo;
import net.mcreator.vcs.workspace.WorkspaceVCS;
import net.mcreator.workspace.WorkspaceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;

@SuppressWarnings("unused") public class Launcher extends JavaPlugin {
	private static final Logger LOG = LogManager.getLogger("MCreator VCS");

	public Launcher(Plugin plugin) {
		super(plugin);
		addListener(WorkspaceSelectorLoadedEvent.class, event -> SwingUtilities.invokeLater(() -> {
			WorkspaceSelector selector = event.getWorkspaceSelector();
			selector.addWorkspaceButton(L10N.t("dialog.workspace_selector.clone"), UIRES.get("vcsclone"),
					e -> cloneRemote(selector));
		}));
		addListener(MCreatorLoadedEvent.class, event -> {
			if (WorkspaceVCS.loadVCSWorkspace(event.getMCreator().getWorkspace()))
				LOG.info("Loaded VCS for current workspace");

			event.getMCreator().mv.addVerticalTab("vcs", L10N.t("workspace.category.remote_workspace"),
					new WorkspacePanelVCS(event.getMCreator().mv));
			initActions(event.getMCreator());
		});
	}

	private static void cloneRemote(WorkspaceSelector selector) {
		VCSInfo vcsInfo = VCSSetupDialogs.getVCSInfoDialog(selector, L10N.t("dialog.workspace_selector.vcs_info"));
		if (vcsInfo != null) {
			File workspaceFolder = FileDialogs.getWorkspaceDirectorySelectDialog(selector, null);
			if (workspaceFolder != null) {
				try {
					selector.setCursor(new Cursor(Cursor.WAIT_CURSOR));
					CloneWorkspace.cloneWorkspace(selector, vcsInfo, workspaceFolder);
					try {
						File workspaceFile = WorkspaceUtils.getWorkspaceFileForWorkspaceFolder(workspaceFolder);
						if (selector.getApplication() != null)
							selector.getApplication().openWorkspaceInMCreator(workspaceFile);
					} catch (Exception ex) {
						throw new Exception("The remote repository is not a MCreator workspace or is corrupted");
					}
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(selector,
							L10N.t("dialog.workspace_selector.clone.setup_failed", ex.getMessage()),
							L10N.t("dialog.workspace_selector.clone.setup_failed.title"), JOptionPane.ERROR_MESSAGE);
				} finally {
					selector.setCursor(Cursor.getDefaultCursor());
				}
			}
		}
	}

	private static void initActions(MCreator mcreator) {
		VCSActionRegistry actionRegistry = new VCSActionRegistry(mcreator);

		JMenu vcs = L10N.menu("menubar.vcs");
		vcs.setMnemonic('V');
		vcs.add(actionRegistry.setupVCS);
		vcs.addSeparator();
		vcs.add(actionRegistry.showUnsyncedChanges);
		vcs.addSeparator();
		vcs.add(actionRegistry.syncFromRemote);
		vcs.add(actionRegistry.syncToRemote);
		vcs.addSeparator();
		vcs.add(actionRegistry.unlinkVCS);
		vcs.add(actionRegistry.remoteWorkspaceSettings);
		mcreator.getMainMenuBar().add(vcs);

		mcreator.getToolBar().addToRightToolbar(actionRegistry.setupVCSOrSettings);
		mcreator.getToolBar().addToRightToolbar(actionRegistry.syncFromRemote);
		mcreator.getToolBar().addToRightToolbar(actionRegistry.syncToRemote);
	}
}
