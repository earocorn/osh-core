/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.sensorhub.api.datastore.IQueryFilter;
import org.sensorhub.api.datastore.EmptyFilterIntersection;
import org.sensorhub.api.datastore.TextFilter;
import org.sensorhub.api.feature.FeatureFilterBase;
import org.sensorhub.utils.FilterUtils;
import org.vast.util.Asserts;
import org.vast.util.BaseBuilder;
import org.vast.util.IResource;


/**
 * <p>
 * Immutable filter for any resource type.<br/>
 * It serves as a base for more advanced resource-specific filters.<br/>
 * There is an implicit AND between all filter parameters
 * </p>
 * 
 * @param <T> Type of resource supported by this filter
 *
 * @author Alex Robin
 * @date Oct 8, 2018
 */
public class ResourceFilter<T extends IResource> implements IQueryFilter, Predicate<T>
{
    protected SortedSet<Long> internalIDs;
    protected SortedSet<Long> parentIDs;
    protected TextFilter fullText;
    protected Predicate<T> valuePredicate;
    protected long limit = Long.MAX_VALUE;
    
    
    protected ResourceFilter() {}
    
    
    public SortedSet<Long> getInternalIDs()
    {
        return internalIDs;
    }


    public SortedSet<Long> getParentIDs()
    {
        return parentIDs;
    }


    public TextFilter getFullTextFilter()
    {
        return fullText;
    }


    public Predicate<T> getValuePredicate()
    {
        return valuePredicate;
    }


    @Override
    public long getLimit()
    {
        return limit;
    }
    
    
    public boolean testValuePredicate(T res)
    {
        return (valuePredicate == null ||
                valuePredicate.test(res));
    }


    @Override
    public boolean test(T res)
    {
        return testValuePredicate(res);
    }
    
    
    public void validate()
    {
    }
    
    
    /**
     * Computes a logical AND between this filter and another filter of the same kind
     * @param filter The other filter to AND with
     * @return The new composite filter
     * @throws EmptyFilterIntersection if the intersection doesn't exist
     */
    public ResourceFilter<T> and(ResourceFilter<T> filter) throws EmptyFilterIntersection
    {
        if (filter == null)
            return this;
        return and(filter, new Builder()).build();
    }
    
    
    protected <F extends ResourceFilter<T>, B extends ResourceFilterBuilder<B, T, F>> B and(F otherFilter, B builder) throws EmptyFilterIntersection
    {
        var internalIDs = FilterUtils.intersect(this.internalIDs, otherFilter.internalIDs);
        if (internalIDs != null)
            builder.withInternalIDs(internalIDs);
        
        var parentIDs = FilterUtils.intersect(this.parentIDs, otherFilter.parentIDs);
        if (parentIDs != null)
            builder.withParents(parentIDs);
        
        var fullTextFilter = this.fullText != null ? this.fullText.and(otherFilter.fullText) : otherFilter.fullText;
        if (fullTextFilter != null)
            builder.withFullText(fullTextFilter);
        
        return builder;
    }


    public class Builder extends ResourceFilterBuilder<Builder, T, ResourceFilter<T>>
    {
        public Builder()
        {
            super(new ResourceFilter<>());
        }
    }
    
    
    @SuppressWarnings("unchecked")
    public static class ResourceFilterBuilder<
            B extends ResourceFilterBuilder<B, T, F>,
            T extends IResource,
            F extends ResourceFilter<T>>
        extends BaseBuilder<F>
    {
        
        protected ResourceFilterBuilder(F instance)
        {
            super(instance);
        }
        
        
        protected B copyFrom(F base)
        {
            Asserts.checkNotNull(base, FeatureFilterBase.class);
            instance.internalIDs = base.getInternalIDs();
            instance.parentIDs = base.getParentIDs();
            instance.fullText = base.getFullTextFilter();
            instance.valuePredicate = base.getValuePredicate();
            instance.limit = base.getLimit();
            return (B)this;
        }
        
        
        /**
         * Keep only resources with specific internal IDs.
         * @param ids One or more internal IDs of resources to select
         * @return This builder for chaining
         */
        public B withInternalIDs(Long... ids)
        {
            return withInternalIDs(Arrays.asList(ids));
        }
        
        
        /**
         * Keep only resources with specific internal IDs.
         * @param ids Collection of internal IDs
         * @return This builder for chaining
         */
        public B withInternalIDs(Collection<Long> ids)
        {
            if (instance.internalIDs == null)
                instance.internalIDs = new TreeSet<>();
            instance.internalIDs.addAll(ids);
            return (B)this;
        }
        
        
        /**
         * Keep only resources with the specified parents
         * @param parentIDs One or more IDs of parent resources
         * @return This builder for chaining
         */
        public B withParents(Long... parentIDs)
        {
            return withParents(Arrays.asList(parentIDs));
        }
        
        
        /**
         * Keep only resources with the specified parents
         * @param parentIDs Collection of parent resource IDs
         * @return This builder for chaining
         */
        public B withParents(Collection<Long> parentIDs)
        {
            if (instance.parentIDs == null)
                instance.parentIDs = new TreeSet<>();
            instance.parentIDs.addAll(parentIDs);
            return (B)this;
        }


        /**
         * Keep only resources matching the given text filter
         * @param filter Full text filter
         * @return This builder for chaining
         */
        public B withFullText(TextFilter filter)
        {
            instance.fullText = filter;
            return (B)this;
        }

        /**
         * Keep only resources matching the nested text filter
         * @return This builder for chaining
         */
        public TextFilter.NestedBuilder<B> withFullText()
        {
            return new TextFilter.NestedBuilder<B>((B)this) {
                @Override
                public B done()
                {
                    ResourceFilterBuilder.this.instance.fullText = build();
                    return (B)ResourceFilterBuilder.this;
                }                
            };
        }


        /**
         * Keep only resources matching the provided predicate
         * @param valuePredicate
         * @return This builder for chaining
         */
        public B withValuePredicate(Predicate<T> valuePredicate)
        {
            instance.valuePredicate = valuePredicate;
            return (B)this;
        }
        
        
        public B withLimit(int limit)
        {
            instance.limit = limit;
            return (B)this;
        }
        
        
        @Override
        public F build()
        {
            instance.validate();
            return super.build();
        }
    }
}
