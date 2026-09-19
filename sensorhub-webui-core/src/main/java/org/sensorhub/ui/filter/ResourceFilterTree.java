/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.filter;

import static org.sensorhub.ui.AdminI18n.tr;

import java.util.List;
import org.sensorhub.api.resource.ResourceFilter;
import org.sensorhub.api.resource.ResourceFilter.ResourceFilterBuilder;
import com.vaadin.event.Action;
import com.vaadin.server.ThemeResource;
import com.vaadin.v7.ui.TreeTable;


@SuppressWarnings({"deprecation"})
public abstract class ResourceFilterTree<T extends ResourceFilter<?>, B extends ResourceFilterBuilder<B,?,T>> extends FilterTree<T, B>
{
    private final String internalIdsProperty = tr("filter.property.internalIds");
    private final String fullTextProperty = tr("filter.property.keywords");
    
    
    static void getActions(Class<?> filterClass, List<Action> actions)
    {
        actions.add(new Action(tr("action.addResourceIds"), new ThemeResource("icons/add.gif")));
        actions.add(new Action(tr("action.addKeywords"), new ThemeResource("icons/add.gif")));
    }
    
    
    @Override
    protected Object renderFilterAsTree(TreeTable tree, Object parentId, T filter)
    {
        //toTreeItem(tree, parentId, internalIdsProperty, filter.getInternalIDs());
        toTreeItem(tree, parentId, fullTextProperty, filter.getFullTextFilter());
        return null;
    }
    
    
    @Override
    protected void fromTreeItem(TreeTable tree, Object itemId, String itemName, String itemValue, B builder)
    {
        if (internalIdsProperty.equals(itemName))
        {
            var ids = readIdList(itemValue);
            builder.withInternalIDs(ids);
        }
        else if (fullTextProperty.equals(itemName))
        {
            var kw = readStringList(itemValue);
            builder.withKeywords(kw);
        }
    }

}
