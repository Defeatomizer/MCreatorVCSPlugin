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
import net.mcreator.ui.laf.themes.Theme;
import net.mcreator.util.DesktopUtils;
import net.mcreator.vcs.workspace.VCSInfo;
import org.eclipse.jgit.api.errors.TransportException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class VCSSetupDialogs {

	private static final Pattern GITHUB_URL = Pattern.compile("^(\\w+://)?github\\.com(/.*)?$");

	public static VCSInfo getVCSInfoDialog(Window parent, String text) {
		return getVCSInfoDialog(parent, text, null, null, false, true);
	}

	public static VCSInfo getVCSInfoDialog(Window parent, String text, String r, String u, boolean p,
			boolean enableRemote) {
		JPanel main = new JPanel(new BorderLayout(0, 10));

		JTextField remote = new JTextField(34);
		JTextField username = new JTextField(25);
		JPasswordField password = new JPasswordField(25);
		JCheckBox savePassword = L10N.checkbox("dialog.vcs.setup_save_password");
		JButton newToken = L10N.button("dialog.vcs.setup_git_access_token.create");
		JLabel pwLabel = L10N.label("dialog.vcs.setup_git_password");

		AtomicBoolean validUri = new AtomicBoolean(false);
		try {
			URI helpURI = new URI("https://github.com/settings/tokens/new");
			newToken.addActionListener(e -> DesktopUtils.browse(helpURI));
			validUri.set(true);
		} catch (URISyntaxException ignored) {
		}

		newToken.setVisible(false);
		remote.getDocument().addDocumentListener(new DocumentListener() {

			@Override public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}

			@Override public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}

			@Override public void changedUpdate(DocumentEvent e) {
				boolean githubRepo = GITHUB_URL.matcher(remote.getText()).find();
				pwLabel.setText(githubRepo ?
						L10N.t("dialog.vcs.setup_git_access_token") :
						L10N.t("dialog.vcs.setup_git_password"));
				newToken.setVisible(validUri.get() && githubRepo);
			}

		});

		remote.setEditable(enableRemote);
		if (!enableRemote)
			remote.setForeground(remote.getDisabledTextColor());

		remote.setText(r);
		username.setText(u);
		savePassword.setSelected(!p);

		remote.setPreferredSize(new Dimension(300, 15));

		JPanel form = new JPanel(new GridLayout(0, 1, 0, 5));
		form.add(PanelUtils.westAndEastElement(L10N.label("dialog.vcs.setup_remove_repository_url"), remote, 5, 0));
		form.add(PanelUtils.westAndEastElement(L10N.label("dialog.vcs.setup_git_username"), username, 5, 0));
		form.add(PanelUtils.westAndEastElement(pwLabel, password, 5, 0));
		form.add(PanelUtils.westAndEastElement(newToken, Box.createHorizontalGlue(), 5, 0));
		form.add(savePassword);

		main.add("Center", form);

		main.add("South", ComponentUtils.setForeground(L10N.label("dialog.vcs.setup_store_password"),
				Theme.current().getAltForegroundColor()));

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
