/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import static org.sensorhub.ui.AdminI18n.tr;

import java.util.HashMap;
import java.util.Map;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.ui.api.UIConstants;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.v7.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;


@SuppressWarnings("serial")
public class ModuleInstanceSelectionPopup extends Window
{
        
    @SuppressWarnings("rawtypes")
    public interface ModuleInstanceSelectionCallback
    {
        public void onSelected(IModule module) throws SensorHubException;
    }
    
    
    public ModuleInstanceSelectionPopup(final Class<?> moduleType, final ModuleInstanceSelectionCallback callback)
    {
        super(tr("dialog.selectModule"));
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        
        // generate table with module list
        final Table table = new Table();
        table.setSizeFull();
        table.setSelectable(true);
        table.setColumnReorderingAllowed(true);        
        table.addContainerProperty(UIConstants.PROP_NAME, String.class, null);
        table.addContainerProperty(UIConstants.PROP_ID, String.class, null);
        table.setColumnHeaders(new String[] {tr("column.moduleName"), tr("column.id")});
        table.setPageLength(10);
        table.setMultiSelect(false);
        
        final Map<Object, IModule<?>> moduleMap = new HashMap<>();
        for (IModule<?> module: ((AdminUI)UI.getCurrent()).getParentHub().getModuleRegistry().getLoadedModules())
        {
            Class<?> moduleClass = module.getClass();
            if (moduleType.isAssignableFrom(moduleClass))
            {
                Object id = table.addItem(new Object[] {
                        module.getName(),
                        module.getLocalID()}, null);
                moduleMap.put(id, module);
            }
        }
        layout.addComponent(table);
        
        // add OK button
        Button okButton = new Button(tr("action.ok"));
        okButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event)
            {
                Object selectedItemId = table.getValue();
                
                if (selectedItemId != null)
                {
                    IModule<?> module = moduleMap.get(selectedItemId);
                    
                    try
                    {
                    if (module != null)
                        callback.onSelected(module);
                }
                    catch (Exception e)
                    {
                        DisplayUtils.showErrorPopup(tr("error.selectModule"), e);
                        return;
                    }
                }
                
                close();
            }
        });
        layout.addComponent(okButton);
        layout.setComponentAlignment(okButton, Alignment.MIDDLE_CENTER);
        
        setContent(layout);
        center();
    }
}
