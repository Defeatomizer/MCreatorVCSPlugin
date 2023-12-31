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

package net.mcreator.vcs.util;

import net.mcreator.ui.dialogs.ProgressDialog;
import net.mcreator.ui.init.L10N;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.awt.*;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public class DialogProgressMonitor implements ProgressMonitor {
	private final String title;
	private final ProgressDialog dialog;
	private final Stack<ProgressDialog.ProgressUnit> units = new Stack<>();
	private int tasksTotal = -1, doneTotal;
	private int tasksCurrent, doneCurrent;

	public static <T> T runTask(DialogProgressMonitor monitor, String threadName, Callable<T> task) throws Exception {
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Exception> exception = new AtomicReference<>();

		Runnable runnable = () -> {
			try {
				result.set(task.call());
			} catch (Exception ex) {
				monitor.fail();
				exception.set(ex);
			} finally {
				monitor.setVisible(false);
			}
		};
		(threadName != null ? new Thread(runnable, threadName) : new Thread(runnable)).start();
		monitor.setVisible(true);

		if (exception.get() != null)
			throw exception.get();

		return result.get();
	}

	public DialogProgressMonitor(Window parent, String title) {
		dialog = new ProgressDialog(parent, this.title = title);
	}

	public void setVisible(boolean visible) {
		if (visible)
			dialog.setVisible(true);
		else
			dialog.hideDialog();
	}

	public void fail() {
		if (!units.isEmpty())
			units.pop().markStateError();
	}

	@Override public void start(int totalTasks) {
		tasksTotal = totalTasks;
		dialog.setTitle(
				tasksTotal == -1 ? title : L10N.t("dialog.vcs.progress.title", title, doneTotal = 0, tasksTotal));
	}

	@Override public void beginTask(String title, int totalWork) {
		dialog.addProgressUnit(units.push(new ProgressDialog.ProgressUnit(title)));
		tasksCurrent = totalWork;
		doneCurrent = 0;
	}

	@Override public void update(int completed) {
		if (!units.isEmpty())
			units.peek().setPercent((int) (tasksCurrent / 100d * (doneCurrent += completed)));
	}

	@Override public void endTask() {
		if (!units.isEmpty())
			units.pop().markStateOk();
		dialog.setTitle(tasksTotal == -1 ? title : L10N.t("dialog.vcs.progress.title", title, ++doneTotal, tasksTotal));
	}

	@Override public boolean isCancelled() {
		return false;
	}

	@Override public void showDuration(boolean b) {
	}

}
