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

package net.mcreator.vcs.ui.workspace;

import net.mcreator.ui.component.TransparentToolBar;
import net.mcreator.ui.component.util.ComponentUtils;
import net.mcreator.ui.dialogs.ProgressDialog;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.laf.SlickDarkScrollBarUI;
import net.mcreator.ui.laf.themes.Theme;
import net.mcreator.ui.workspace.AbstractWorkspacePanel;
import net.mcreator.ui.workspace.WorkspacePanel;
import net.mcreator.util.FilenameUtilsPatched;
import net.mcreator.vcs.ui.actions.VCSActionRegistry;
import net.mcreator.vcs.ui.actions.impl.SetupVCSAction;
import net.mcreator.vcs.ui.component.BranchesPopup;
import net.mcreator.vcs.util.DialogProgressMonitor;
import net.mcreator.vcs.workspace.WorkspaceVCS;
import net.mcreator.workspace.TerribleWorkspaceHacks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorkspacePanelVCS extends AbstractWorkspacePanel {

	private static final Logger LOG = LogManager.getLogger("VCS Panel");

	private final List<RevCommit> cachedCommits = new ArrayList<>();
	private final JTable commits;
	private final TableRowSorter<TableModel> sorter;

	private final JButton switchBranch = new JButton(UIRES.get("16px.vcs"));

	public WorkspacePanelVCS(WorkspacePanel workspacePanel) {
		super(workspacePanel);
		setLayout(new BorderLayout(0, 5));

		TransparentToolBar bar = new TransparentToolBar();
		bar.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 0));

		JButton uncommitted = L10N.button("workspace.vcs.show_local_changes");
		uncommitted.setIcon(UIRES.get("16px.info"));
		uncommitted.setContentAreaFilled(false);
		uncommitted.setOpaque(false);
		ComponentUtils.deriveFont(uncommitted, 12);
		uncommitted.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		bar.add(uncommitted);

		uncommitted.addActionListener(
				e -> VCSActionRegistry.get(workspacePanel.getMCreator()).showUnsyncedChanges.doAction());

		JButton checkout = L10N.button("workspace.vcs.jump_to_selected_commit");
		checkout.setIcon(UIRES.get("16px.rwd"));
		checkout.setContentAreaFilled(false);
		checkout.setOpaque(false);
		ComponentUtils.deriveFont(checkout, 12);
		checkout.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		bar.add(checkout);

		checkout.addActionListener(e -> checkoutToSelectedCommit());

		switchBranch.setContentAreaFilled(false);
		switchBranch.setOpaque(false);
		ComponentUtils.deriveFont(switchBranch, 12);
		switchBranch.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		bar.add(switchBranch);

		JButton fetchBranches = L10N.button("dialog.vcs.branches_popup.fetch_branches");
		fetchBranches.setIcon(UIRES.get("16px.ext.gif"));
		fetchBranches.setContentAreaFilled(false);
		fetchBranches.setOpaque(false);
		ComponentUtils.deriveFont(fetchBranches, 12);
		fetchBranches.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		bar.add(fetchBranches);

		JButton deleteBranch = L10N.button("dialog.vcs.branches_popup.delete_branch");
		deleteBranch.setIcon(UIRES.get("16px.delete.gif"));
		deleteBranch.setContentAreaFilled(false);
		deleteBranch.setOpaque(false);
		ComponentUtils.deriveFont(deleteBranch, 12);
		deleteBranch.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		bar.add(deleteBranch);

		WorkspaceVCS workspaceVCS = WorkspaceVCS.getVCSWorkspace(workspacePanel.getMCreator().getWorkspace());
		switchBranch.addActionListener(
				e -> new BranchesPopup(workspaceVCS, workspacePanel.getMCreator(), null).show(switchBranch, 4, 20));
		fetchBranches.addActionListener(e -> {
			try {
				DialogProgressMonitor monitor = new DialogProgressMonitor(workspacePanel.getMCreator(),
						L10N.t("dialog.vcs.branches_popup.fetch_branches"));
				DialogProgressMonitor.runTask(monitor, "WorkspacePanelVCS-FetchBranches",
						() -> workspaceVCS.getGit().fetch().setRemote("origin").setRemoveDeletedRefs(true)
								.setCredentialsProvider(workspaceVCS.getCredentialsProvider(
										workspacePanel.getMCreator().getWorkspaceFolder(),
										workspacePanel.getMCreator())).setProgressMonitor(monitor).call());
			} catch (Exception ex) {
				LOG.error("Failed to fetch branches", ex);
			}
		});
		deleteBranch.addActionListener(e -> new BranchesPopup(workspaceVCS, workspacePanel.getMCreator(), ref -> {
			Git git = workspaceVCS.getGit();
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(workspacePanel.getMCreator(),
					L10N.t("dialog.vcs.branches_popup.delete_branch.message", ref.getName()),
					L10N.t("dialog.vcs.branches_popup.delete_branch.title"), JOptionPane.YES_NO_OPTION)) {
				try {
					CredentialsProvider credentialsProvider = workspaceVCS.getCredentialsProvider(
							workspacePanel.getMCreator().getWorkspaceFolder(), workspacePanel.getMCreator());
					DialogProgressMonitor monitor = new DialogProgressMonitor(workspacePanel.getMCreator(),
							L10N.t("dialog.vcs.branches_popup.delete_branch"));
					DialogProgressMonitor.runTask(monitor, "BranchesPopup-DeleteBranch", () -> {
						git.reset().setMode(ResetCommand.ResetType.HARD).call();
						git.branchDelete().setBranchNames(ref.getName()).setForce(true).call();
						if (ref.getName().startsWith(Constants.R_REMOTES)) {
							String dest = Constants.R_HEADS + FilenameUtilsPatched.getName(ref.getName());
							git.push().setRemote("origin").setRefSpecs(new RefSpec(":" + dest).setSource(null))
									.setCredentialsProvider(credentialsProvider).setProgressMonitor(monitor).call();
						}
						return git.fetch().setRemote("origin").setRemoveDeletedRefs(true)
								.setCredentialsProvider(credentialsProvider).setProgressMonitor(monitor).call();
					});
					workspacePanel.getMCreator().mv.reloadElementsInCurrentTab();
				} catch (Exception er) {
					LOG.error("Failed to delete branch", er);
				}
			}
		}).show(deleteBranch, 4, 20));

		add("North", bar);

		commits = new JTable(new DefaultTableModel(
				new Object[] { L10N.t("workspace.vcs.commit_list.id"), L10N.t("workspace.vcs.commit_list.message"),
						L10N.t("workspace.vcs.commit_list.author"), L10N.t("workspace.vcs.commit_list.date") }, 0) {
			@Override public boolean isCellEditable(int row, int column) {
				return false;
			}
		}) {
			@Override public String getToolTipText(@Nonnull MouseEvent event) {
				return switch (columnAtPoint(event.getPoint())) {
					case 0 -> cachedCommits.get(rowAtPoint(event.getPoint())).getName();
					case 1 -> "<html>" + cachedCommits.get(rowAtPoint(event.getPoint())).getFullMessage()
							.replaceAll("\\R", "<br>");
					default -> null;
				};
			}
		};

		sorter = new TableRowSorter<>(commits.getModel());
		commits.setRowSorter(sorter);
		commits.setBackground(Theme.current().getBackgroundColor());
		commits.setSelectionBackground(Theme.current().getAltBackgroundColor());
		commits.setForeground(Theme.current().getForegroundColor());
		commits.setSelectionForeground(Theme.current().getForegroundColor());
		commits.setBorder(BorderFactory.createEmptyBorder());
		commits.setGridColor(Theme.current().getAltBackgroundColor());
		commits.setRowHeight(38);
		ComponentUtils.deriveFont(commits, 13);

		commits.getColumnModel().getColumn(0).setMinWidth(58);
		commits.getColumnModel().getColumn(0).setMaxWidth(58);
		commits.getColumnModel().getColumn(0).setPreferredWidth(58);

		commits.getColumnModel().getColumn(1).setPreferredWidth(650);

		JTableHeader header = commits.getTableHeader();
		header.setBackground(Theme.current().getInterfaceAccentColor());
		header.setForeground(Theme.current().getBackgroundColor());

		JScrollPane sp = new JScrollPane(commits);
		sp.setBackground(Theme.current().getBackgroundColor());
		sp.getViewport().setOpaque(false);
		sp.getVerticalScrollBar().setUnitIncrement(11);
		sp.getVerticalScrollBar().setUI(new SlickDarkScrollBarUI(Theme.current().getBackgroundColor(),
				Theme.current().getAltBackgroundColor(), sp.getVerticalScrollBar()));
		sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

		sp.setColumnHeaderView(null);

		JPanel holder = new JPanel(new BorderLayout());
		holder.setOpaque(false);
		holder.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		holder.add(sp);

		add("Center", holder);

		commits.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() == 2) {
					checkoutToSelectedCommit();
				}
			}
		});
	}

	private void checkoutToSelectedCommit() {
		int row = commits.getSelectedRow();
		if (row == -1)
			return;

		WorkspaceVCS workspaceVCS = WorkspaceVCS.getVCSWorkspace(workspacePanel.getMCreator().getWorkspace());
		String shortCommitId = commits.getValueAt(row, 0).toString();
		if (shortCommitId == null || workspaceVCS == null)
			return;

		ProgressDialog pd = new ProgressDialog(workspacePanel.getMCreator(),
				L10N.t("workspace.vcs.jump_to_selected_commit"));
		new Thread(() -> {
			ProgressDialog.ProgressUnit pu = null;
			try {
				Git git = workspaceVCS.getGit();
				for (RevCommit commit : git.log().add(git.getRepository().resolve(git.getRepository().getFullBranch()))
						.call()) {
					if (commit.abbreviate(7).name().equals(shortCommitId)) {
						int option = JOptionPane.showOptionDialog(workspacePanel.getMCreator(),
								L10N.t("workspace.vcs.jump_commit_confirmation", commit.getShortMessage()),
								L10N.t("workspace.vcs.jump_commit_confirmation.title"), JOptionPane.DEFAULT_OPTION,
								JOptionPane.QUESTION_MESSAGE, null,
								new String[] { L10N.t("workspace.vcs.jump_to", commit.abbreviate(7).name()),
										UIManager.getString("OptionPane.cancelButtonText") }, null);

						if (option == 0) {
							// track all so they can be stashed properly
							pd.addProgressUnit(pu = new ProgressDialog.ProgressUnit(
									L10N.t("workspace.vcs.jump_to_selected_commit.stashing")));
							git.rm().addFilepattern(".").call();
							git.add().addFilepattern(".").call();

							// remove local changes attempt 1
							git.stashCreate().call();
							git.stashDrop().call();
							pu.markStateOk();

							ObjectId currentBranchHead = git.getRepository().resolve(Constants.HEAD);
							String oldBranch = git.getRepository().getFullBranch();

							pd.addProgressUnit(pu = new ProgressDialog.ProgressUnit(
									L10N.t("workspace.vcs.jump_to_selected_commit.jumping")));
							git.checkout().setName(commit.getName()).setStartPoint(commit.getName()).call();
							pu.setPercent(17);
							git.checkout().setName("tmpHistoryBranch" + commit.getName()).setCreateBranch(true).call();
							pu.setPercent(33);
							String branchName = git.getRepository().getFullBranch();
							git.merge().setStrategy(MergeStrategy.OURS).include(currentBranchHead)
									.setFastForward(MergeCommand.FastForwardMode.NO_FF)
									.setMessage("Jump back to commit " + commit.getName()).call();
							pu.setPercent(50);
							git.checkout().setName(oldBranch).call();
							pu.setPercent(67);
							git.merge().include(git.getRepository().resolve(branchName)).call();
							pu.setPercent(83);
							git.branchDelete().setBranchNames(branchName).call();
							pu.markStateOk();

							// we might need to make another commit to commit the merge changes
							try {
								pd.addProgressUnit(pu = new ProgressDialog.ProgressUnit(
										L10N.t("workspace.vcs.jump_to_selected_commit.cleaning")));
								git.rm().addFilepattern(".").call();
								git.add().addFilepattern(".").call();
								git.commit().setAll(true).setAllowEmpty(false).setMessage("Jump cleanup commit").call();
								pu.markStateOk();
							} catch (Exception ignored) {
							}

							TerribleWorkspaceHacks.reloadFromFS(workspacePanel.getMCreator().getWorkspace());
							workspacePanel.reloadElementsInCurrentTab();
							workspacePanel.getMCreator().actionRegistry.buildWorkspace.doAction();
						}

						break;
					}
				}
			} catch (GitAPIException | IOException e) {
				LOG.error("Checkout failed!", e);
				if (pu != null)
					pu.markStateError();
			}
			pd.hideDialog();
		}, "JumpToCommit").start();
		pd.setVisible(true);
	}

	@Override public boolean canSwitchToSection() {
		return SetupVCSAction.setupVCSForWorkspaceIfNotYet(workspacePanel.getMCreator());
	}

	@Override public void reloadElements() {
		WorkspaceVCS workspaceVCS = WorkspaceVCS.getVCSWorkspace(workspacePanel.getMCreator().getWorkspace());
		if (workspaceVCS != null) {
			int row = commits.getSelectedRow();

			DefaultTableModel model = (DefaultTableModel) commits.getModel();
			model.setRowCount(0);
			cachedCommits.clear();

			Git git = workspaceVCS.getGit();
			try {
				Repository repository = git.getRepository();

				switchBranch.setText(L10N.t("workspace.vcs.current_branch", repository.getBranch()));

				for (RevCommit commit : git.log().add(repository.resolve(repository.getFullBranch())).call()) {
					cachedCommits.add(commit);
					model.addRow(new Object[] { commit.abbreviate(7).name(), "<html><b>" + commit.getShortMessage(),
							commit.getAuthorIdent().getName(), commit.getAuthorIdent().getWhen() });
				}
			} catch (Exception ignored) {
			}

			refilterElements();

			try {
				commits.setRowSelectionInterval(row, row);
			} catch (Exception ignored) {
			}
		}
	}

	@Override public void refilterElements() {
		if (WorkspaceVCS.getVCSWorkspace(workspacePanel.getMCreator().getWorkspace()) != null)
			sorter.setRowFilter(RowFilter.regexFilter(workspacePanel.search.getText()));
	}

}
