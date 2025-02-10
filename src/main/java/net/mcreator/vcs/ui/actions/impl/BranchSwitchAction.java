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

import net.mcreator.generator.Generator;
import net.mcreator.generator.setup.WorkspaceGeneratorSetup;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.action.impl.workspace.WorkspaceSettingsAction;
import net.mcreator.ui.dialogs.workspace.WorkspaceGeneratorSetupDialog;
import net.mcreator.ui.init.L10N;
import net.mcreator.util.FilenameUtilsPatched;
import net.mcreator.vcs.util.DialogProgressMonitor;
import net.mcreator.vcs.util.GSONClone;
import net.mcreator.vcs.workspace.WorkspaceVCS;
import net.mcreator.workspace.TerribleWorkspaceHacks;
import net.mcreator.workspace.Workspace;
import net.mcreator.workspace.settings.WorkspaceSettings;
import net.mcreator.workspace.settings.WorkspaceSettingsChange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;

import javax.swing.*;
import java.io.IOException;

public class BranchSwitchAction {

	private static final Logger LOG = LogManager.getLogger("Branch Switch");

	public static void switchBranch(MCreator mcreator, WorkspaceVCS workspaceVCS, String branchToSwitchTo) {
		int n = JOptionPane.showConfirmDialog(mcreator, L10N.t("dialog.vcs.switch_branch.message", branchToSwitchTo),
				L10N.t("dialog.vcs.switch_branch.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (n == 0) {
			Git git = workspaceVCS.getGit();

			CredentialsProvider credentialsProvider = workspaceVCS.getCredentialsProvider(mcreator.getWorkspaceFolder(),
					mcreator);

			try {
				Workspace localWorkspace = mcreator.getWorkspace();
				WorkspaceSettings preSwitchSettings = GSONClone.deepClone(localWorkspace.getWorkspaceSettings(),
						WorkspaceSettings.class);

				try { // first try to fetch remote changes
					DialogProgressMonitor monitor = new DialogProgressMonitor(mcreator,
							L10N.t("dialog.vcs.switch_branch.title"));
					DialogProgressMonitor.runTask(monitor, "BranchSwitchAction-Fetch",
							() -> git.fetch().setRemote("origin").setCredentialsProvider(credentialsProvider)
									.setProgressMonitor(monitor).call());
				} catch (Exception ignored) {
				}

				git.reset().setMode(ResetCommand.ResetType.HARD).call();

				CheckoutCommand checkoutCommand = git.checkout();
				checkoutCommand.setName(FilenameUtilsPatched.getName(branchToSwitchTo));
				if (!git.branchList().call().stream().map(Ref::getName).toList()
						.contains(Constants.R_HEADS + FilenameUtilsPatched.getName(branchToSwitchTo))) {
					checkoutCommand.setCreateBranch(true);
					Ref fetchHead = git.getRepository().findRef(Constants.FETCH_HEAD);
					if (fetchHead != null && fetchHead.getObjectId() != null) {
						for (RevCommit commit : git.log().add(fetchHead.getObjectId()).call()) {
							checkoutCommand.setStartPoint(commit);
							break;
						}
					}
				}
				checkoutCommand.call();

				// possible refactor after sync start
				TerribleWorkspaceHacks.reloadFromFS(mcreator.getWorkspace());

				// if version changed, switch the generator
				if (!localWorkspace.getWorkspaceSettings().getCurrentGenerator()
						.equals(preSwitchSettings.getCurrentGenerator())) {
					LOG.debug("Switching local workspace generator to " + localWorkspace.getWorkspaceSettings()
							.getCurrentGenerator());
					WorkspaceGeneratorSetup.cleanupGeneratorForSwitchTo(localWorkspace,
							Generator.GENERATOR_CACHE.get(localWorkspace.getWorkspaceSettings().getCurrentGenerator()));
					localWorkspace.switchGenerator(localWorkspace.getWorkspaceSettings().getCurrentGenerator());
					WorkspaceGeneratorSetupDialog.runSetup(mcreator, false);
				}
				WorkspaceSettingsChange workspaceSettingsChange = new WorkspaceSettingsChange(preSwitchSettings,
						localWorkspace.getWorkspaceSettings());
				if (workspaceSettingsChange.refactorNeeded())
					WorkspaceSettingsAction.refactorWorkspace(mcreator, workspaceSettingsChange);
				// possible refactor after sync end

				mcreator.getStatusBar().setPersistentMessage(L10N.t("statusbar.vcs.switched_working_branch",
						FilenameUtilsPatched.getName(branchToSwitchTo)));

				mcreator.reloadWorkspaceTabContents();
			} catch (GitAPIException | IOException e) {
				LOG.error("Failed to switch branch!", e);
				JOptionPane.showMessageDialog(mcreator,
						L10N.t("dialog.vcs.switch_branch_fail.message", branchToSwitchTo, e.getMessage()),
						L10N.t("dialog.vcs.switch_branch_fail.push_fail.title"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

}
