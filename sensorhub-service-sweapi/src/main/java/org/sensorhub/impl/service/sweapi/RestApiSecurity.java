/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi;

import java.io.IOException;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.api.security.IUserInfo;


public interface RestApiSecurity
{
    
    public void setCurrentUser(String userID);
    
    
    public IUserInfo getCurrentUser();
    
    
    public void checkPermission(IPermission perm);
    
    
    public void checkResourcePermission(IPermission perm, String id) throws IOException;
    
    
    public void checkParentPermission(IPermission perm, String parentId) throws IOException;
    
}
