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

package net.mcreator.vcs.util;

import net.mcreator.ui.init.L10N;
import net.mcreator.vcs.workspace.VCSInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.awt.*;
import java.io.File;

public class CloneWorkspace {

	public static void cloneWorkspace(Window parent, VCSInfo vcsInfo, File folderInto) throws Exception {
		DialogProgressMonitor monitor = new DialogProgressMonitor(parent,
				L10N.t("dialog.workspace_selector.clone.title"));
		DialogProgressMonitor.runTask(monitor, "CloneWorkspace", () -> {
			try (Git ignored = Git.cloneRepository().setURI(vcsInfo.getRemote()).setDirectory(folderInto)
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider(vcsInfo.getUsername(),
							vcsInfo.getPassword(folderInto, parent))).setProgressMonitor(monitor).call()) {
				VCSInfo.saveToFile(vcsInfo, new File(folderInto, "/.mcreator/vcsInfo"));
			}
			return null;
		});
	}

}
