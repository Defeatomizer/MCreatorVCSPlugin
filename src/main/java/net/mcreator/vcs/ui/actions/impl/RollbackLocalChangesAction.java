/*
 * MCreator VCS plugin
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

import net.mcreator.generator.Generator;
import net.mcreator.generator.setup.WorkspaceGeneratorSetup;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.action.ActionRegistry;
import net.mcreator.ui.action.impl.workspace.WorkspaceSettingsAction;
import net.mcreator.ui.dialogs.workspace.WorkspaceGeneratorSetupDialog;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.vcs.util.GSONClone;
import net.mcreator.vcs.workspace.WorkspaceVCS;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.settings.WorkspaceSettings;
import net.mcreator.workspace.settings.WorkspaceSettingsChange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import javax.swing.*;

public class RollbackLocalChangesAction extends VCSAction {

	private static final Logger LOG = LogManager.getLogger("Changes rollback");

	public RollbackLocalChangesAction(ActionRegistry actionRegistry) {
		super(actionRegistry, L10N.t("action.vcs.rollback_unsynced_changes"), e -> {
			MCreator mcreator = actionRegistry.getMCreator();
			if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(mcreator,
					L10N.t("dialog.vcs.rollback_changes.message"), L10N.t("dialog.vcs.rollback_changes.title"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
				WorkspaceVCS workspaceVCS = WorkspaceVCS.getVCSWorkspace(mcreator.getWorkspace());

				try {
					Workspace localWorkspace = mcreator.getWorkspace();
					WorkspaceSettings preResetSettings = GSONClone.deepClone(localWorkspace.getWorkspaceSettings(),
							WorkspaceSettings.class);

					workspaceVCS.getGit().reset().setMode(ResetCommand.ResetType.HARD).call();

					// possible refactor after sync start
					mcreator.getWorkspace().reloadFromFS();

					// if version changed, switch the generator
					String currentGenerator = localWorkspace.getWorkspaceSettings().getCurrentGenerator();
					if (!currentGenerator.equals(preResetSettings.getCurrentGenerator())) {
						LOG.debug("Switching local workspace generator to " + currentGenerator);
						WorkspaceGeneratorSetup.cleanupGeneratorForSwitchTo(localWorkspace,
								Generator.GENERATOR_CACHE.get(currentGenerator));
						localWorkspace.switchGenerator(currentGenerator);
						WorkspaceGeneratorSetupDialog.runSetup(mcreator, false);
					}
					WorkspaceSettingsChange workspaceSettingsChange = new WorkspaceSettingsChange(preResetSettings,
							localWorkspace.getWorkspaceSettings());
					if (workspaceSettingsChange.refactorNeeded()) // possible refactor after sync end
						WorkspaceSettingsAction.refactorWorkspace(mcreator, workspaceSettingsChange);

					mcreator.statusBar.setPersistentMessage(L10N.t("statusbar.vcs.rolled_back_local_changes"));

					mcreator.mv.updateMods();
				} catch (GitAPIException ex) {
					LOG.error("Failed to rollback changes!", ex);
					JOptionPane.showMessageDialog(mcreator,
							L10N.t("dialog.vcs.rollback_changes.fail.message", ex.getMessage()),
							L10N.t("dialog.vcs.rollback_changes.fail.title"), JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		setIcon(UIRES.get("16px.vcs_rollback"));
	}

}
