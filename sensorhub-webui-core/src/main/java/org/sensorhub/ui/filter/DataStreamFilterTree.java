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
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import com.vaadin.event.Action;
import com.vaadin.v7.ui.TreeTable;


@SuppressWarnings({"deprecation"})
public class DataStreamFilterTree extends ResourceFilterTree<DataStreamFilter, DataStreamFilter.Builder>
{
    private final String outputNamesProperty = tr("filter.property.outputNames");
    private final String observedPropertiesProperty = tr("filter.property.observedProperties");
    private final String validTimeProperty = tr("filter.property.validTime");
    private final String systemFilterProperty = tr("filter.property.parentSystems");
    private final String observationFilterProperty = tr("filter.property.observations");
    
    
    static void getActions(Class<?> filterClass, List<Action> actions)
    {
        ResourceFilterTree.getActions(filterClass, actions);
    }
    
    
    static DataStreamFilter newFilter()
    {
        return new DataStreamFilter.Builder().build();
    }
    
    
    @Override
    protected Object renderFilterAsTree(TreeTable tree, Object parentId, DataStreamFilter filter)
    {
        tree.setPageLength(tree.getPageLength()+7);
        var id = tree.addItem(new Object[] {tr("filter.datastream"), null}, null);
        if (parentId != null)
            tree.setParent(id, parentId);
        
        super.renderFilterAsTree(tree, id, filter);
        toTreeItem(tree, id, outputNamesProperty, filter.getOutputNames());
        toTreeItem(tree, id, observedPropertiesProperty, filter.getObservedProperties());
        toTreeItem(tree, id, validTimeProperty, filter.getValidTimeFilter());
        toTreeItem(tree, id, systemFilterProperty, SystemFilterTree::newFilter, filter.getSystemFilter());
        toTreeItem(tree, id, observationFilterProperty, ObsFilterTree::newFilter, filter.getObservationFilter());
        
        return id;
    }
    

    @Override
    protected DataStreamFilter buildFilterFromTree(TreeTable tree, Object parentId)
    {
        var builder = new DataStreamFilter.Builder();
        super.buildFilterFromTree(tree, parentId, builder);
        return builder.build();
    }
    
    
    @Override
    protected void fromTreeItem(TreeTable tree, Object itemId, String itemName, String itemValue, DataStreamFilter.Builder builder)
    {
        super.fromTreeItem(tree, itemId, itemName, itemValue, builder);
        
        if (outputNamesProperty.equals(itemName))
        {
            var names = readStringList(itemValue);
            builder.withOutputNames(names);
        }
        else if (observedPropertiesProperty.equals(itemName))
        {
            var uris = readStringList(itemValue);
            builder.withObservedProperties(uris);
        }
        else if (validTimeProperty.equals(itemName))
        {
            var tf = readTemporalFilter(itemValue);
            builder.withValidTime(tf);
        }
        else if (systemFilterProperty.equals(itemName) && Boolean.parseBoolean(itemValue))
        {
            var subTree = new SystemFilterTree();
            var filter = subTree.buildFilterFromTree(tree, itemId);
            builder.withSystems(filter);
        }
        else if (observationFilterProperty.equals(itemName) && Boolean.parseBoolean(itemValue))
        {
            var subTree = new ObsFilterTree();
            var filter = subTree.buildFilterFromTree(tree, itemId);
            builder.withObservations(filter);
        }
    }

}
