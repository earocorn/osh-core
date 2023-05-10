/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.h2.mvstore;

import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.h2.mvstore.type.DataType;
import org.h2.mvstore.type.ObjectDataType;


/**
 * <p>
 * Modified version of H2 MVMap to optimize certain operations
 * </p>
 *
 * @author Alex Robin
 * @param <K> Key type
 * @param <V> Value type
 * @date Apr 19, 2018
 */
public class MVBTreeMap<K, V> extends MVMap<K, V>
{

    protected MVBTreeMap(DataType keyType, DataType valueType)
    {
        super(keyType, valueType);
    }


    public K getFullKey(Object key)
    {
        return getFullKey(root, key);
    }


    /*
     * Same code as binarySearch method but returning full key.
     * This can be used when a partial key is used for retrieval
     */
    @SuppressWarnings({ "unchecked" })
    private K getFullKey(Page p, Object key)
    {
        int x = p.binarySearch(key);
        if (!p.isLeaf()) {
            if (x < 0) {
                x = -x - 1;
            } else {
                x++;
            }
            p = p.getChildPage(x);
            return getFullKey(p, key);
        }
        if (x >= 0) {
            return (K)p.getKey(x);
        }
        return null;
    }


    public Entry<K, V> getEntry(Object key)
    {
        return getEntry(root, key);
    }


    /*
     * Same code as binarySearch method but returning full entry
     */
    @SuppressWarnings({ "unchecked" })
    private Entry<K, V> getEntry(Page p, Object key)
    {
        int x = p.binarySearch(key);
        if (!p.isLeaf()) {
            if (x < 0) {
                x = -x - 1;
            } else {
                x++;
            }
            p = p.getChildPage(x);
            return getEntry(p, key);
        }
        if (x >= 0) {
            return new DataUtils.MapEntry<>((K)p.getKey(x), (V)p.getValue(x));
        }
        return null;
    }


    public Entry<K, V> ceilingEntry(K key)
    {
        return getMinMaxEntry(root, key, false, false);
    }


    public Entry<K, V> higherEntry(K key)
    {
        return getMinMaxEntry(root, key, false, true);
    }


    public Entry<K, V> floorEntry(K key)
    {
        return getMinMaxEntry(root, key, true, false);
    }


    public Entry<K, V> lowerEntry(K key)
    {
        return getMinMaxEntry(root, key, true, true);
    }
    
    
    /*
     * Same code as getMinMax method but returning full entry
     */
    @SuppressWarnings("unchecked")
    private Entry<K, V> getMinMaxEntry(Page p, K key, boolean min, boolean excluding) {
        if (p.isLeaf()) {
            int x = p.binarySearch(key);
            if (x < 0) {
                x = -x - (min ? 2 : 1);
            } else if (excluding) {
                x += min ? -1 : 1;
            }
            if (x < 0 || x >= p.getKeyCount()) {
                return null;
            }
            return new DataUtils.MapEntry<>((K)p.getKey(x), (V)p.getValue(x));
        }
        int x = p.binarySearch(key);
        if (x < 0) {
            x = -x - 1;
        } else {
            x++;
        }
        while (true) {
            if (x < 0 || x >= getChildPageCount(p)) {
                return null;
            }
            Entry<K, V> entry = getMinMaxEntry(p.getChildPage(x), key, min, excluding);
            if (entry != null) {
                return entry;
            }
            x += min ? -1 : 1;
        }
    }
    
    
    protected boolean containsKey(Page p, Object key) {
        int x = p.binarySearch(key);
        if (!p.isLeaf()) {
            if (x < 0) {
                x = -x - 1;
            } else {
                x++;
            }
            p = p.getChildPage(x);
            return containsKey(p, key);
        }
        if (x >= 0) {
            return true;
        }
        return false;
    }
    

    @Override
    public boolean containsKey(Object key) {
        return containsKey(root, key);
    }
    
    
    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (!containsKey(key))
            return null;
        
        beforeWrite();
        V result = null;
        long v = writeVersion;
        synchronized (this) {
            Page p = root.copy(v);
            result = (V) remove(p, v, key);
            if (!p.isLeaf() && p.getTotalCount() == 0) {
                p.removePage();
                p = Page.createEmpty(this,  p.getVersion());
            }
            newRoot(p);
        }
        return result;
    }
    
    
    @Override
    protected Object put(Page p, long writeVersion, Object key, Object value) {
        int index = p.binarySearch(key);
        if (p.isLeaf()) {
            if (index < 0) {
                index = -index - 1;
                p.insertLeaf(index, key, value);
                return null;          
            }
            p.setKey(index, key); // updating key is needed in case parts are not used for comparison
            return p.setValue(index, value);
        }
        // p is a node
        if (index < 0) {
            index = -index - 1;
        } else {
            index++;
        }
        Page c = p.getChildPage(index).copy(writeVersion);
        if (c.getMemory() > store.getPageSplitSize() && c.getKeyCount() > 1) {
            // split on the way down
            int at = c.getKeyCount() / 2;
            Object k = c.getKey(at);
            Page split = c.split(at);
            p.setChild(index, split);
            p.insertNode(index, k, c);
            // now we are not sure where to add
            return put(p, writeVersion, key, value);
        }
        Object result = put(c, writeVersion, key, value);
        p.setChild(index, c);
        return result;
    }
    
    
    public Stream<K> keyStream()
    {
        Spliterator<K> it = Spliterators.spliteratorUnknownSize(keySet().iterator(), Spliterator.DISTINCT | Spliterator.ORDERED);
        return StreamSupport.stream(it, false);
    }
    
    
    /*
     * Need to override this so pages and correctly rewritten on autoCompact
     */
    @Override
    boolean rewrite(Set<Integer> set) {
        // read from old version, to avoid concurrent reads
        long previousVersion = store.getCurrentVersion() - 1;
        if (previousVersion < getCreateVersion()) {
            // a new map
            return true;
        }
        MVMap<K, V> readMap;
        try {
            readMap = openVersion(previousVersion);
        } catch (IllegalArgumentException e) {
            // unknown version: ok
            // TODO should not rely on exception handling
            return true;
        }
        try {
            synchronized (this)
            {
                beforeWrite();
                long v = writeVersion;
                var root = rewritePage(readMap.root, set, v);
                if (root != null)
                {
                    newRoot(root);
                    //System.out.println(getName() + ": Rewriting " + set.size() + " chunks: " + set);
                }
                return true;
            }
        } catch (IllegalStateException e) {
            // TODO should not rely on exception handling
            if (DataUtils.getErrorCode(e.getMessage()) == DataUtils.ERROR_CHUNK_NOT_FOUND) {
                // ignore
                return false;
            }
            throw e;
        }
    }

    /*
     * May rewrite the page if the page or one of its children is in one of the listed chunks.
     * If a new page is created, it is returned, otherwise null is returned
     * */
    private Page rewritePage(Page p, Set<Integer> set, long version) {
        long pos = p.getPos();
        int chunkId = DataUtils.getPageChunkId(pos);
        
        if (p.isLeaf()) {
            return set.contains(chunkId) ? p.copy(version) : null;
        }
        else
        {
            Page newPage = set.contains(chunkId) ? p.copy(version) : null;
            
            for (int i = 0; i < getChildPageCount(p); i++) {
                // skip child page before even loading it if not in one of the chunks
                long childPos = p.getChildPagePos(i);
                if (childPos != 0)
                {
                    if (DataUtils.getPageType(childPos) == DataUtils.PAGE_TYPE_LEAF) {
                        chunkId = DataUtils.getPageChunkId(childPos);
                        if (!set.contains(chunkId))
                            continue;
                    }
                    
                    // may rewrite child page
                    var newChild = rewritePage(p.getChildPage(i), set, version);
                    
                    // if child was rewritten, we also need to write this one
                    // and all of the parents will be rewritten recursively
                    if (newChild != null)
                    {
                        if (newPage == null)
                            newPage = p.copy(version);
                        newPage.setChild(i, newChild);
                    }
                }
            }
            return newPage;
        }
    }
    

    /**
     * A builder for this class.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    public static class Builder<K, V> implements MapBuilder<MVBTreeMap<K, V>, K, V>
    {
        protected DataType keyType;
        protected DataType valueType;


        public Builder<K, V> keyType(DataType keyType)
        {
            this.keyType = keyType;
            return this;
        }


        public Builder<K, V> valueType(DataType valueType)
        {
            this.valueType = valueType;
            return this;
        }


        @Override
        public MVBTreeMap<K, V> create()
        {
            if (keyType == null)
                keyType = new ObjectDataType();

            if (valueType == null)
                valueType = new ObjectDataType();

            return new MVBTreeMap<>(keyType, valueType);
        }
    }
}
