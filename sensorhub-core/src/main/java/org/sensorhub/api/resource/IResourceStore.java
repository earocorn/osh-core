/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2018, Sensia Software LLC
 All Rights Reserved. This software is the property of Sensia Software LLC.
 It cannot be duplicated, used, or distributed without the express written
 consent of Sensia Software LLC.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.resource;

import org.sensorhub.api.datastore.IDataStore;
import org.sensorhub.api.datastore.ValueField;
import org.sensorhub.api.resource.ResourceFilter.ResourceFilterBuilder;
import org.vast.util.IResource;


/**
 * <p>
 * Generic interface for all resource stores
 * </p>
 * @param <K> Key type
 * @param <V> Value type 
 * @param <F> Filter type
 *
 * @author Alex Robin
 * @date Oct 8, 2018
 */
public interface IResourceStore<K extends IResourceKey, V extends IResource, VF extends ValueField, F extends ResourceFilter<V>> extends IDataStore<K, V, VF, F>
{

    public ResourceFilterBuilder<? extends ResourceFilterBuilder<?, V, F>, V, F> getFilterBuilder();
    
    
    /**
     * Add a new resource to the store, generating a new key for it
     * @param value new resource
     * @return The newly allocated key (internal ID)
     */
    K add(V value);
    
    
    K add(long parentId, V value);
    
}