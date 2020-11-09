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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.h2.mvstore.MVRadixTreeMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.type.DataType;
import org.sensorhub.api.datastore.FullTextFilter;
import org.vast.util.IResource;
import com.google.common.base.Strings;


/**
 * <p>
 * Implementation of a full-text index based on a radix tree
 * @param <T> Resource type
 * @param <K> Key reference type
 * </p>
 *
 * @author Alex Robin
 * @since Oct 30, 2020
 */
public class FullTextIndex<T extends IResource, K extends Comparable<?>> 
{
    protected static final Pattern NUMBER_PATTERN = Pattern.compile("^\\.?\\d+(\\.\\d+)?");
        
    MVRadixTreeMap<byte[], K> radixTreeMap;
    
    /* Default analyzer */
    EnglishAnalyzer analyzer = new EnglishAnalyzer();
        
    
    public FullTextIndex(MVStore mvStore, String mapName, DataType valueType)
    {
        this.radixTreeMap = mvStore.openMap(mapName, new MVRadixTreeMap.Builder<byte[], K>()
            .keyType(new ResourceRadixKeyDataType())
            .valueType(valueType));
    }
        
    
    public void add(K key, T res)
    {
        // we first create a token set to remove duplicates
        HashSet<String> tokenSet = new HashSet<>();
        addToTokenSet(res, tokenSet);
        for (String token: tokenSet)
            radixTreeMap.put(token.getBytes(), key);
    }
    
        
    public void update(K key, T old, T new_)
    {
        remove(key, old);
        add(key, new_);
    }
    
    
    public void remove(K key, T res)
    {
        HashSet<String> tokenSet = new HashSet<>();
        addToTokenSet(res, tokenSet);
        for (String token: tokenSet)
            radixTreeMap.remove(token.getBytes(), key);
    }
    
    
    public Stream<K> selectKeys(FullTextFilter filter)
    {
        return getKeywordStream(filter)
            .flatMap(w -> radixTreeMap.entryCursor(w.getBytes()).valueStream())
            .distinct(); // remove duplicate IDs
    }
    
    
    protected void addToTokenSet(T resource, Set<String> tokenSet)
    {
        if (!Strings.isNullOrEmpty(resource.getName()))
            addToTokenSet(resource.getName(), analyzer, tokenSet);
        
        if (!Strings.isNullOrEmpty(resource.getDescription()))
            addToTokenSet(resource.getDescription(), analyzer, tokenSet);
    }
    
    
    protected void addToTokenSet(String fieldValue, Analyzer analyzer, Set<String> tokenSet)
    {
        try (TokenStream tokenStream = analyzer.tokenStream(null, fieldValue))
        {
            CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken())
            {
                String token = NUMBER_PATTERN.matcher(attr).replaceAll("");
                if (!token.isEmpty())
                    tokenSet.add(token);
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Error analyzing text", e);
        }
    }
    
    
    protected Stream<String> getKeywordStream(FullTextFilter filter)
    {
        Set<String> tokens = new HashSet<>();
        for (String keyword: filter.getKeywords())
            addToTokenSet(keyword, analyzer, tokens);
        return tokens.stream();
    }
    
    
    protected Stream<K> addFullTextPostFilter(Stream<K> pkStream, FullTextFilter filter)
    {
        Set<byte[]> keywordSet = getKeywordStream(filter)
            .map(String::getBytes)
            .collect(Collectors.toSet());
        
        return pkStream.filter(k -> {
            if (k == null)
                return false;
            for (byte[] w: keywordSet)
            {
                if (radixTreeMap.containsValue(w, k))
                    return true;
            }
            return false;
        });
    }
    
    
    public void clear()
    {
        radixTreeMap.clear();
    }
    
}
