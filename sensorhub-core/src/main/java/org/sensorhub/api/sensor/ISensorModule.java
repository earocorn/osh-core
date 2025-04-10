/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import org.sensorhub.api.data.IDataProducerModule;

/**
 * <p>
 * Interface to be implemented by all sensor drivers connected to the system.<br/>
 * Inputs/Output should always be created in the init() method even if they are
 * further modified during or after startup.
 * </p>
 *
 * @author Alex Robin
 * @param <ConfigType> 
 * @since Nov 5, 2010
 */
public interface ISensorModule<ConfigType extends SensorConfig> extends IDataProducerModule<ConfigType>, ISensorDriver
{
    
}