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

import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.init.L10N;
import net.mcreator.ui.laf.themes.Theme;
import net.mcreator.vcs.util.FileSyncHandle;
import net.mcreator.vcs.util.diff.MergeHandle;
import net.mcreator.vcs.util.diff.ResultSide;
import net.mcreator.workspace.settings.WorkspaceSettings;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Locale;

public class MergeHandleComponent extends JPanel {

	public final JRadioButton local, remote;

	public MergeHandleComponent(List<MergeHandleComponent> mergeHandleComponents, MergeHandle<?> mergeHandle) {
		super(new BorderLayout(40, 2));
		setOpaque(false);
		setMinimumSize(new Dimension(400, 10));
		local = L10N.radiobutton("dialog.vcs.merge_handle_accept_mine",
				L10N.t("dialog.vcs.diff_change_type." + mergeHandle.getLocalChange().name()
						.toLowerCase(Locale.ENGLISH)));
		remote = L10N.radiobutton("dialog.vcs.merge_handle_accept_theirs",
				L10N.t("dialog.vcs.diff_change_type." + mergeHandle.getRemoteChange().name()
						.toLowerCase(Locale.ENGLISH)));

		JLabel label;
		if (mergeHandle.getLocal() instanceof FileSyncHandle fsh)
			label = new JLabel(fsh.getLocalPath());
		else if (mergeHandle.getLocal() instanceof WorkspaceSettings)
			label = L10N.label("dialog.vcs.merge_handle_workspace_settings");
		else
			label = new JLabel(mergeHandle.getLocal().toString());
		add("Center", PanelUtils.centerInPanel(label));

		local.setOpaque(false);
		remote.setOpaque(false);

		add("West", local);
		add("East", remote);

		local.addActionListener(e -> mergeHandle.selectResultSide(ResultSide.LOCAL));
		remote.addActionListener(e -> mergeHandle.selectResultSide(ResultSide.REMOTE));

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(local);
		buttonGroup.add(remote);

		local.setSelected(true);

		setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.current().getAltBackgroundColor()));

		mergeHandleComponents.add(this);
	}
}
