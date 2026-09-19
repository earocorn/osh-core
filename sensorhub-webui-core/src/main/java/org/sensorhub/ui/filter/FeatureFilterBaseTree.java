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
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.datastore.feature.FeatureFilterBase.FeatureFilterBaseBuilder;
import com.vaadin.event.Action;
import com.vaadin.server.ThemeResource;
import com.vaadin.v7.ui.TreeTable;


@SuppressWarnings({"deprecation"})
public abstract class FeatureFilterBaseTree<T extends FeatureFilterBase<?>, B extends FeatureFilterBaseBuilder<B,?,T>> extends ResourceFilterTree<T,B>
{
    private final String uniqueIdsProperty = tr("filter.property.uniqueIds");
    private final String validTimeProperty = tr("filter.property.validTime");
    private final String locationProperty = tr("filter.property.location");
    
    

    static void getActions(Class<?> filterClass, List<Action> actions)
    {
        ResourceFilterTree.getActions(filterClass, actions);
        actions.add(new Action(tr("action.addSystemUids"), new ThemeResource("icons/add.gif")));
        actions.add(new Action(tr("action.addValidityRange"), new ThemeResource("icons/add.gif")));
    }
    

    @Override
    protected Object renderFilterAsTree(TreeTable tree, Object parentId, T filter)
    {
        super.renderFilterAsTree(tree, parentId, filter);
        toTreeItem(tree, parentId, uniqueIdsProperty, filter.getUniqueIDs());
        toTreeItem(tree, parentId, validTimeProperty, filter.getValidTime());
        toTreeItem(tree, parentId, locationProperty, filter.getLocationFilter());
        return null;
    }
    

    @Override
    protected void fromTreeItem(TreeTable tree, Object itemId, String itemName, String itemValue, B builder)
    {
        super.fromTreeItem(tree, itemId, itemName, itemValue, builder);
        
        if (uniqueIdsProperty.equals(itemName))
        {
            var uids = readStringList(itemValue);
            builder.withUniqueIDs(uids);
        }
        else if (validTimeProperty.equals(itemName))
        {
            var tf = readTemporalFilter(itemValue);
            builder.withValidTime(tf);
        }
        else if (locationProperty.equals(itemName))
        {
            var sf = readSpatialFilter(itemValue);
            builder.withLocation(sf);
        }
    }

}
