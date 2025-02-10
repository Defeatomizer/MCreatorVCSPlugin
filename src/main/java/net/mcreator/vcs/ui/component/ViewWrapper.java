/*
 * MCreator VCS plugin
 * Copyright (C) 2023-2025, Defeatomizer
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

import net.mcreator.ui.MCreator;
import net.mcreator.ui.MCreatorTabs;
import net.mcreator.ui.views.ViewBase;

import javax.swing.*;

public class ViewWrapper extends ViewBase
		implements MCreatorTabs.TabShownListener, MCreatorTabs.TabHiddenListener, MCreatorTabs.TabClosedListener,
		MCreatorTabs.TabClosingListener {

	private final JPanel viewContents;
	private final MCreatorTabs.Tab viewTab;

	private String viewName = "";
	private ImageIcon viewIcon;
	private boolean closeable = true, showNewTab = true;

	public ViewWrapper(MCreator mcreator, Object id, boolean uppercase, JPanel contents) {
		super(mcreator);
		add(viewContents = contents);

		viewTab = new MCreatorTabs.Tab(this, id, uppercase);
		viewTab.setTabClosedListener(this);
		viewTab.setTabClosingListener(this);
		viewTab.setTabShownListener(this);
		viewTab.setTabHiddenListener(this);
	}

	@Override public String getViewName() {
		return viewName;
	}

	public ViewWrapper setViewName(String viewName) {
		viewTab.setText(this.viewName = viewName);
		viewTab.updateSize();
		return this;
	}

	@Override public ImageIcon getViewIcon() {
		return viewIcon;
	}

	public ViewWrapper setViewIcon(ImageIcon viewIcon) {
		viewTab.setIcon(this.viewIcon = viewIcon);
		viewTab.updateSize();
		return this;
	}

	public JPanel getViewContents() {
		return viewContents;
	}

	public ViewWrapper setCloseable(boolean closeable) {
		this.closeable = closeable;
		return this;
	}

	public ViewWrapper setShowNewTab(boolean showNewTab) {
		this.showNewTab = showNewTab;
		return this;
	}

	@Override public ViewWrapper showView() {
		MCreatorTabs.Tab current = mcreator.getTabs().getCurrentTab();
		MCreatorTabs.Tab existing = mcreator.getTabs().showTabOrGetExisting(viewTab);
		if (existing != null)
			return (ViewWrapper) existing.getContent();

		mcreator.getTabs().addTab(viewTab);
		if (!showNewTab)
			mcreator.getTabs().showTab(current);

		return this;
	}

	@Override public void tabClosed(MCreatorTabs.Tab tab) {
	}

	@Override public boolean tabClosing(MCreatorTabs.Tab tab) {
		return closeable;
	}

	@Override public void tabShown(MCreatorTabs.Tab tab) {
	}

	@Override public void tabHidden(MCreatorTabs.Tab tab) {
	}

}
