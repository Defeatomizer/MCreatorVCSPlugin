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

import net.mcreator.ui.component.util.ComponentUtils;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.init.L10N;
import net.mcreator.vcs.workspace.VCSInfo;
import org.eclipse.jgit.api.errors.TransportException;

import javax.swing.*;
import java.awt.*;

public class VCSSetupDialogs {

	public static VCSInfo getVCSInfoDialog(Window parent, String text) {
		return getVCSInfoDialog(parent, text, null, null, false, true);
	}

	public static VCSInfo getVCSInfoDialog(Window parent, String text, String r, String u, boolean p,
			boolean enableRemote) {
		JPanel main = new JPanel(new BorderLayout(0, 20));

		JTextField remote = new JTextField(34);
		JTextField username = new JTextField(25);
		JPasswordField password = new JPasswordField(25);
		JCheckBox savePassword = L10N.checkbox("dialog.vcs.setup_save_password");

		remote.setEditable(enableRemote);
		if (!enableRemote)
			remote.setForeground(remote.getDisabledTextColor());

		remote.setText(r);
		username.setText(u);
		savePassword.setSelected(!p);

		remote.setPreferredSize(new Dimension(300, 15));

		JPanel form = new JPanel(new GridLayout(4, 1, 0, 5));
		form.add(PanelUtils.westAndEastElement(L10N.label("dialog.vcs.setup_remove_repository_url"), remote, 5, 0));
		form.add(PanelUtils.westAndEastElement(L10N.label("dialog.vcs.setup_git_username"), username, 5, 0));
		form.add(PanelUtils.westAndEastElement(L10N.label("dialog.vcs.setup_git_password"), password, 5, 0));
		form.add(savePassword);

		main.add("Center", form);

		main.add("South", ComponentUtils.setForeground(L10N.label("dialog.vcs.setup_store_password"),
				(Color) UIManager.get("MCreatorLAF.GRAY_COLOR")));

		main.add("North", new JLabel(text));

		int option = JOptionPane.showOptionDialog(parent, main, L10N.t("dialog.vcs.setup_remote_workspace_details"),
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				new String[] { L10N.t("dialog.vcs.setup_proceed"), UIManager.getString("OptionPane.cancelButtonText") },
				L10N.t("dialog.vcs.setup_proceed"));

		if (option == 0) {
			VCSInfo info = new VCSInfo(enableRemote ? remote.getText() : r, username.getText(),
					new String(password.getPassword()), !savePassword.isSelected());
			parent.setCursor(new Cursor(Cursor.WAIT_CURSOR));
			boolean valid;
			try {
				valid = info.isValid();
			} catch (Exception e) {
				if (e instanceof TransportException && e.getMessage().contains("not authorized")) {
					JOptionPane.showMessageDialog(parent, L10N.t("dialog.vcs.setup_incorrect_username_password"),
							L10N.t("dialog.vcs.setup_invalid_parameters"), JOptionPane.WARNING_MESSAGE);
				} else if (e instanceof TransportException && e.getMessage().contains("not found")) {
					JOptionPane.showMessageDialog(parent, L10N.t("dialog.vcs.setup_url_inaccessible"),
							L10N.t("dialog.vcs.setup_invalid_parameters"), JOptionPane.WARNING_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(parent, L10N.t("dialog.vcs.setup_one_invalid_parameter"),
							L10N.t("dialog.vcs.setup_invalid_parameters"), JOptionPane.WARNING_MESSAGE);
				}
				parent.setCursor(Cursor.getDefaultCursor());
				return getVCSInfoDialog(parent, text, remote.getText(), username.getText(), !savePassword.isSelected(),
						enableRemote);
			}
			parent.setCursor(Cursor.getDefaultCursor());
			if (valid)
				return info;
		}
		return null;
	}

}
