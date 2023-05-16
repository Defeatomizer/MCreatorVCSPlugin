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

import net.mcreator.vcs.util.diff.MergeHandle;
import net.mcreator.vcs.util.diff.ResultSide;
import net.mcreator.workspace.Workspace;
import org.eclipse.jgit.diff.DiffEntry;

import java.io.File;

public class FileSyncHandle {

	private byte[] remoteBytes, localBytes, baseBytes;
	private String remotePath;
	private String localPath;
	private final String basePath;
	private boolean unmerged;

	FileSyncHandle(String basePath) {
		this.basePath = basePath;
	}

	void setRemoteBytes(byte[] remoteBytes) {
		this.remoteBytes = remoteBytes;
	}

	void setLocalBytes(byte[] localBytes) {
		this.localBytes = localBytes;
	}

	void setBaseBytes(byte[] baseBytes) {
		this.baseBytes = baseBytes;
	}

	void setUnmerged(boolean unmerged) {
		this.unmerged = unmerged;
	}

	void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public byte[] getRemoteBytes() {
		return remoteBytes;
	}

	public byte[] getLocalBytes() {
		return localBytes;
	}

	public byte[] getBaseBytes() {
		return baseBytes;
	}

	public String getBasePath() {
		return basePath;
	}

	public String getRemotePath() {
		return remotePath;
	}

	public String getLocalPath() {
		return localPath;
	}

	public String getPath(ResultSide resultSide) {
		return switch (resultSide) {
			case LOCAL -> getLocalPath();
			case REMOTE -> getRemotePath();
			default -> getBasePath();
		};
	}

	public byte[] getBytes(ResultSide resultSide) {
		return switch (resultSide) {
			case LOCAL -> getLocalBytes();
			case REMOTE -> getRemoteBytes();
			default -> getBaseBytes();
		};
	}

	public DiffEntry.ChangeType getChangeTypeRelativeTo(ResultSide resultSide) {
		return switch (resultSide) {
			case LOCAL -> getChangeTypeRelativeToLocal();
			case REMOTE -> getChangeTypeRelativeToRemote();
			default -> null;
		};
	}

	public DiffEntry.ChangeType getChangeTypeRelativeToLocal() {
		if (baseBytes == null)
			if (localBytes != null)
				return DiffEntry.ChangeType.ADD;

		if (baseBytes != null)
			if (localBytes == null)
				return DiffEntry.ChangeType.DELETE;

		if (baseBytes != null)
			return DiffEntry.ChangeType.MODIFY;

		return null;
	}

	public DiffEntry.ChangeType getChangeTypeRelativeToRemote() {
		if (baseBytes == null)
			if (localBytes != null)
				return DiffEntry.ChangeType.ADD;

		if (baseBytes != null)
			if (localBytes == null)
				return DiffEntry.ChangeType.DELETE;

		if (baseBytes != null)
			return DiffEntry.ChangeType.MODIFY;

		return null;
	}

	public boolean isUnmerged() {
		return unmerged;
	}

	public MergeHandle<FileSyncHandle> toPathMergeHandle() {
		return new MergeHandle<>(this, this, getChangeTypeRelativeToLocal(), getChangeTypeRelativeToRemote());
	}

	public File toFileInWorkspace(Workspace workspace, ResultSide resultSide) {
		return new File(workspace.getWorkspaceFolder(), getPath(resultSide));
	}

	@Override public boolean equals(Object o) {
		return o instanceof FileSyncHandle other && other.basePath.equals(basePath);
	}

	@Override public int hashCode() {
		return basePath.hashCode();
	}

	@Override public String toString() {
		return basePath + (unmerged ? " (unmerged)" : "") + " - " + (remoteBytes != null ? "R" : "") + (
				localBytes != null ? "L" : "") + (baseBytes != null ? "B" : "");
	}
}
