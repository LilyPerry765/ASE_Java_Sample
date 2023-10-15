/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/workplace/administration/CmsAdminMenu.java,v $
 * Date   : $Date: 2006/03/27 14:52:20 $
 * Version: $Revision: 1.13 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.administration;

import org.opencms.jsp.CmsJspActionElement;
import org.opencms.main.OpenCms;
import org.opencms.util.CmsIdentifiableObjectContainer;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.I_CmsIdentifiableObjectContainer;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.CmsWorkplaceSettings;
import org.opencms.workplace.tools.CmsTool;
import org.opencms.workplace.tools.CmsToolDialog;
import org.opencms.workplace.tools.CmsToolMacroResolver;
import org.opencms.workplace.tools.CmsToolManager;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

/**
 * Implementation of the administration view leftside's menu.<p>
 * 
 * @author Michael Moossen  
 * 
 * @version $Revision: 1.13 $ 
 * 
 * @since 6.0.0 
 */
public class CmsAdminMenu extends CmsToolDialog {

    /** Default link target constant. */
    public static final String DEFAULT_TARGET = "admin_content";

    /** Group container. */
    private I_CmsIdentifiableObjectContainer m_groupContainer = new CmsIdentifiableObjectContainer(true, true);

    /**
     * Default Constructor.<p>
     * 
     * @param jsp the jsp context
     */
    public CmsAdminMenu(CmsJspActionElement jsp) {

        super(jsp);
        initAdminTool();
        installMenu();
    }

    /**
     * Adds a group.<p>
     * 
     * @param group the group
     * 
     * @see I_CmsIdentifiableObjectContainer#addIdentifiableObject(String, Object)
     */
    public void addGroup(CmsAdminMenuGroup group) {

        m_groupContainer.addIdentifiableObject(group.getName(), group);
    }

    /**
     * Adds a menu item at the given position.<p>
     * 
     * @param group the group
     * @param position the position
     * 
     * @see I_CmsIdentifiableObjectContainer#addIdentifiableObject(String, Object, float)
     */
    public void addGroup(CmsAdminMenuGroup group, float position) {

        m_groupContainer.addIdentifiableObject(group.getName(), group, position);
    }

    /**
     * Adds a new item to the specified menu.<p>
     * 
     * If the menu does not exist, it will be created.<p>
     * 
     * @param groupName the name of the group
     * @param name the name of the item
     * @param icon the icon to display
     * @param link the link to open when selected
     * @param helpText the help text to display
     * @param enabled if enabled or not
     * @param position the relative position to install the item
     * @param target the target frame to open the link into
     * 
     * @return the new item
     */
    public CmsAdminMenuItem addItem(
        String groupName,
        String name,
        String icon,
        String link,
        String helpText,
        boolean enabled,
        float position,
        String target) {

        groupName = CmsToolMacroResolver.resolveMacros(groupName, this);
        CmsAdminMenuGroup group = getGroup(groupName);
        if (group == null) {
            String gid = "group" + m_groupContainer.elementList().size();
            group = new CmsAdminMenuGroup(gid, groupName);
            addGroup(group, position);
        }
        String id = "item" + group.getId() + group.getMenuItems().size();
        CmsAdminMenuItem item = new CmsAdminMenuItem(id, name, icon, link, helpText, enabled, target);
        group.addMenuItem(item, position);
        return item;
    }

    /**
     * Returns the requested group.<p>
     * 
     * @param name the name of the group
     * 
     * @return the group
     * 
     * @see I_CmsIdentifiableObjectContainer#getObject(String)
     */
    public CmsAdminMenuGroup getGroup(String name) {

        return (CmsAdminMenuGroup)m_groupContainer.getObject(name);
    }

    /**
     * Returns the admin manager.<p>
     * 
     * @return the admin manager
     */
    public CmsToolManager getToolManager() {

        return OpenCms.getWorkplaceManager().getToolManager();
    }

    /**
     * Generates the necesary html code for the groups.<p>
     * 
     * @param wp the page for which the code is generated
     * 
     * @return html code
     */
    public String groupHtml(CmsWorkplace wp) {

        StringBuffer html = new StringBuffer(2048);
        Iterator itHtml = m_groupContainer.elementList().iterator();
        while (itHtml.hasNext()) {
            CmsAdminMenuGroup group = (CmsAdminMenuGroup)itHtml.next();
            html.append(group.groupHtml(wp));
        }
        return html.toString();
    }

    /**
     * Creates the default menu as the root tool structure.<p>
     */
    public void installMenu() {

        // initialize the menu groups
        m_groupContainer.clear();

        // creates the context help menu
        CmsAdminMenuGroup helpMenu = new CmsAdminMenuGroup("help", Messages.get().getBundle(getLocale()).key(
            Messages.GUI_ADMIN_MENU_HELP_GROUP_0));
        helpMenu.addMenuItem(new CmsAdminContextHelpMenuItem());
        addGroup(helpMenu);

        Iterator itElems = getToolManager().getToolsForPath(this, getToolManager().getBaseToolPath(this), false).iterator();
        while (itElems.hasNext()) {
            CmsTool tool = (CmsTool)itElems.next();
            // check visibility
            String link = tool.getHandler().getLink();
            if (link.indexOf("?") > 0) {
                link = link.substring(0, link.indexOf("?"));
            }
            if (!getCms().existsResource(link) || !tool.getHandler().isVisible(getCms())) {
                continue;
            }

            // cut out the base
            String path = tool.getHandler().getPath().substring(getToolManager().getBaseToolPath(this).length());
            // special case of the base tool
            if (CmsStringUtil.isEmpty(path)) {
                continue;
            }
            // skip initial '/'
            int pos = tool.getHandler().getPath().indexOf(CmsToolManager.TOOLPATH_SEPARATOR);
            // only install if at first level
            if (path.indexOf(CmsToolManager.TOOLPATH_SEPARATOR, pos + 1) < 0) {

                addItem(
                    tool.getHandler().getGroup(),
                    tool.getHandler().getShortName(),
                    tool.getHandler().getSmallIconPath(),
                    CmsToolManager.linkForToolPath(getJsp(), tool.getHandler().getPath()),
                    tool.getHandler().isEnabled(getCms()) ? tool.getHandler().getHelpText()
                    : tool.getHandler().getDisabledHelpText(),
                    tool.getHandler().isEnabled(getCms()),
                    tool.getHandler().getPosition(),
                    CmsAdminMenu.DEFAULT_TARGET);
            }
        }
    }

    /**
     * @see org.opencms.workplace.CmsWorkplace#initWorkplaceRequestValues(org.opencms.workplace.CmsWorkplaceSettings, javax.servlet.http.HttpServletRequest)
     */
    protected void initWorkplaceRequestValues(CmsWorkplaceSettings settings, HttpServletRequest request) {

        fillParamValues(request);
    }
}