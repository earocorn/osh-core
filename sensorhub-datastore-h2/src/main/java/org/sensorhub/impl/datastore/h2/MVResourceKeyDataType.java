/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.h2;

import java.nio.ByteBuffer;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.resource.ResourceKey;


/**
 * <p>
 * H2 DataType implementation for internal resource key objects
 * </p>
 *
 * @author Alex Robin
 * @date Oct 8, 2023
 */
class MVResourceKeyDataType implements DataType
{
    final int idScope;
    
    
    MVResourceKeyDataType(int idScope)
    {
        this.idScope = idScope;
    }
    
    
    @Override
    public int compare(Object objA, Object objB)
    {
        var a = (ResourceKey<?>)objA;
        var b = (ResourceKey<?>)objB;
        
        return Long.compare(
            a.getInternalID().getIdAsLong(),
            b.getInternalID().getIdAsLong());
    }
    

    @Override
    public int getMemory(Object obj)
    {
        var id = ((DataStreamKey)obj).getInternalID().getIdAsLong();
        return DataUtils.getVarLongLen(id);
    }
    

    @Override
    public void write(WriteBuffer wbuf, Object obj)
    {
        DataStreamKey key = (DataStreamKey)obj;
        wbuf.putVarLong(key.getInternalID().getIdAsLong());
    }
    

    @Override
    public void write(WriteBuffer wbuf, Object[] obj, int len, boolean key)
    {
        for (int i=0; i<len; i++)
            write(wbuf, obj[i]);
    }
    

    @Override
    public Object read(ByteBuffer buff)
    {
        long internalID = DataUtils.readVarLong(buff);
        return new DataStreamKey(idScope, internalID);
    }
    

    @Override
    public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
    {
        for (int i=0; i<len; i++)
            obj[i] = read(buff);
    }

}
