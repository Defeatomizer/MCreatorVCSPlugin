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

package net.mcreator.vcs.ui.dialogs;

import net.mcreator.ui.MCreator;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.laf.themes.Theme;
import net.mcreator.vcs.util.FileSyncHandle;
import net.mcreator.vcs.util.diff.MergeHandle;
import net.mcreator.vcs.ui.component.MergeHandleComponent;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class VCSFileMergeDialog {

	public static void show(MCreator mcreator, List<MergeHandle<FileSyncHandle>> unmergedPaths) {
		if (unmergedPaths.size() == 0)
			return;

		JPanel dialog = new JPanel(new BorderLayout());

		JPanel merges = new JPanel();
		merges.setLayout(new BoxLayout(merges, BoxLayout.Y_AXIS));
		merges.setBackground(Theme.current().getSecondAltBackgroundColor());
		merges.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		List<MergeHandleComponent> mergeHandleComponents = new ArrayList<>();

		JButton allLocal = L10N.button("dialog.vcs.file_merge_all_local");
		JButton allRemote = L10N.button("dialog.vcs.file_merge_all_remote");
		allLocal.addActionListener(e -> mergeHandleComponents.forEach(mhc -> mhc.local.setSelected(true)));
		allRemote.addActionListener(e -> mergeHandleComponents.forEach(mhc -> mhc.remote.setSelected(true)));
		merges.add(PanelUtils.westAndEastElement(allLocal, allRemote));

		for (MergeHandle<FileSyncHandle> modElementMergeHandle : unmergedPaths)
			merges.add(new MergeHandleComponent(mergeHandleComponents, modElementMergeHandle));

		JScrollPane scrollPane = new JScrollPane(PanelUtils.totalCenterInPanel(merges));
		scrollPane.getVerticalScrollBar().setUnitIncrement(15);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setMaximumSize(new Dimension(650, 350));

		dialog.add("Center", scrollPane);
		dialog.add("North", L10N.label("dialog.vcs.file_merge_manual_message"));

		JOptionPane.showOptionDialog(mcreator, dialog, L10N.t("dialog.vcs.file_merge_manual_required"),
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
				new String[] { L10N.t("dialog.vcs.file_merge_finish") }, null);
	}

}
