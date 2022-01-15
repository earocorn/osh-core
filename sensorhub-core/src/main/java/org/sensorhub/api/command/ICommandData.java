/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.command;

import java.math.BigInteger;
import java.time.Instant;
import org.sensorhub.api.data.IObsData;
import net.opengis.swe.v20.DataBlock;


/**
 * <p>
 * Represents command data sent to a control interface
 * </p>
 *
 * @author Alex Robin
 * @date Mar 11, 2021
 */
public interface ICommandData
{
    
    /**
     * @return The ID of the command or null if not yet assigned.
     */
    BigInteger getID();
    
    
    /**
     * Assign the command ID. This property is immutable once it is set.
     * @param id
     */
    void assignID(BigInteger id);
    
    
    /**
     * @return The ID of the command stream that this command is part of.
     */
    long getCommandStreamID();
    
    
    /**
     * @return The feature of interest (can be the receiver system itself,
     * a subsystem, or another feature) that the command will have an impact on.
     */
    long getFoiID();


    public default boolean hasFoi()
    {
        return getFoiID() != IObsData.NO_FOI;
    }
    
    
    /**
     * @return The ID of the sender
     */
    String getSenderID();
    
    
    /**
     * @return Time the command was issued
     */
    Instant getIssueTime();

    
    /**
     * @return The command parameters (i.e. command data)
     */
    DataBlock getParams();

}