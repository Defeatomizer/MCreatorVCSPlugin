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

package net.mcreator.vcs.ui.component;

import net.mcreator.minecraft.RegistryNameFixer;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.init.L10N;
import net.mcreator.vcs.ui.actions.impl.BranchSwitchAction;
import net.mcreator.vcs.workspace.WorkspaceVCS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class BranchesPopup extends JPopupMenu {

	private static final Logger LOG = LogManager.getLogger("Branches Popup");

	public BranchesPopup(WorkspaceVCS workspaceVCS, MCreator mcreator, Consumer<Ref> refHandler) {
		try {
			Git git = workspaceVCS.getGit();

			if (refHandler == null) {
				JMenuItem newBranch = new JMenuItem(L10N.t("dialog.vcs.branches_popup.new_branch"));
				add(newBranch);
				newBranch.addActionListener(e -> {
					String newBranchName = JOptionPane.showInputDialog(mcreator,
							L10N.t("dialog.vcs.branches_popup.new_branch.message"),
							L10N.t("dialog.vcs.branches_popup.new_branch.title"), JOptionPane.QUESTION_MESSAGE);
					if (newBranchName != null) {
						newBranchName = RegistryNameFixer.fix(newBranchName);
						if (!newBranchName.isEmpty()) {
							try {
								git.branchCreate().setName(newBranchName)
										.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
										.setStartPoint(git.getRepository().getFullBranch()).call();
								git.checkout().setName(newBranchName).call();

								mcreator.reloadWorkspaceTabContents();

								try { // try to push if remote exists
									git.push().setRemote("origin")
											.setRefSpecs(new RefSpec(newBranchName + ":" + newBranchName)).call();
								} catch (Exception ignored) {
								}
							} catch (GitAPIException | IOException er) {
								LOG.error("Failed to create branch", er);
							}
						} else {
							JOptionPane.showMessageDialog(mcreator,
									L10N.t("dialog.vcs.branches_popup.new_branch.invalid.message"),
									L10N.t("dialog.vcs.branches_popup.new_branch.invalid.title"),
									JOptionPane.ERROR_MESSAGE);
						}
					}
				});
				addSeparator();

				JMenuItem rename = new JMenuItem(L10N.t("dialog.vcs.branches_popup.rename_branch"));
				add(rename);
				rename.addActionListener(e -> {
					String newName = JOptionPane.showInputDialog(mcreator,
							L10N.t("dialog.vcs.branches_popup.rename_branch.message"),
							L10N.t("dialog.vcs.branches_popup.rename_branch.title"), JOptionPane.QUESTION_MESSAGE);
					if (newName != null) {
						newName = RegistryNameFixer.fix(newName);
						if (!newName.isEmpty()) {
							try {
								git.branchRename().setNewName(newName).call();
								mcreator.reloadWorkspaceTabContents();
							} catch (GitAPIException er) {
								LOG.error("Failed to rename branch", er);
							}
						} else {
							JOptionPane.showMessageDialog(mcreator,
									L10N.t("dialog.vcs.branches_popup.rename_branch.invalid.message"),
									L10N.t("dialog.vcs.branches_popup.rename_branch.invalid.title"),
									JOptionPane.ERROR_MESSAGE);
						}
					}
				});
				addSeparator();
			}

			List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

			for (Ref ref : refs) {
				if (refHandler != null && git.getRepository().getFullBranch().equals(ref.getName()))
					continue;

				JMenuItem menuItem;
				if (refHandler != null) {
					menuItem = new JMenuItem(ref.getName());
					menuItem.addActionListener(e -> refHandler.accept(ref));
				} else {
					menuItem = new JRadioButtonMenuItem(ref.getName());
					if (git.getRepository().getFullBranch().equals(ref.getName())) {
						menuItem.setSelected(true);
					} else {
						menuItem.addActionListener(
								e -> BranchSwitchAction.switchBranch(mcreator, workspaceVCS, ref.getName()));
					}
				}
				add(menuItem);
			}
		} catch (Exception ignored) {
		}
	}

}
