/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.sensorhub.api.module.IModule;


/**
 * <p>
 * Interface for all communication providers giving access to an input stream
 * for reading incoming data and an output stream for sending outgoing data.
 * </p>
 *
 * @author Alex Robin
 * @param <ConfigType> Comm module config type
 * @since Jun 19, 2015
 */
public interface ICommProvider<ConfigType extends CommProviderConfig<?>> extends IModule<ConfigType>
{
    
    public InputStream getInputStream() throws IOException;
    
    
    public OutputStream getOutputStream() throws IOException;
    
}
