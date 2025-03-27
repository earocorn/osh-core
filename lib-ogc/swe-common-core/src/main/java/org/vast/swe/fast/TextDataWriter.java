/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.vast.swe.fast;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.vast.swe.SWEDataTypeUtils;
import org.vast.util.DateTimeFormat;
import org.vast.util.WriterException;
import net.opengis.swe.v20.Boolean;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Text;
import net.opengis.swe.v20.TextEncoding;
import net.opengis.swe.v20.Time;


/**
 * <p>
 * New implementation of text data writer with better efficiency since the 
 * write tree is pre-computed during init instead of being re-evaluated
 * while iterating through the component tree.
 * </p>
 *
 * @author Alex Robin
 * @since Dec 7, 2016
 */
public class TextDataWriter extends AbstractDataWriter
{
    protected Writer writer;
    protected String tokenSep = ",";
    protected String blockSep = "\n";
    protected boolean collapseWhiteSpaces = true;
    protected boolean firstToken;
    protected Map<String, IntegerWriter> countWriters = new HashMap<>();

    
    protected abstract class ValueWriter extends BaseProcessor
    {
        public abstract void writeValue(DataBlock data, int index) throws IOException;

        @Override
        public int process(DataBlock data, int index) throws IOException
        {
            if (enabled)
            {
                writeSeparator();
                writeValue(data, index);
            }
            
            return ++index;
        }
    }
    
    
    protected class BooleanWriter extends ValueWriter
    {
        @Override
        public void writeValue(DataBlock data, int index) throws IOException
        {
            boolean val = data.getBooleanValue(index);
            writer.write(java.lang.Boolean.toString(val));
        }
    }
    
    
    protected class IntegerWriter extends ValueWriter
    {
        int val; // store val if used as array size
        
        @Override
        public void writeValue(DataBlock data, int index) throws IOException
        {
            val = data.getIntValue(index);
            writer.write(Integer.toString(val));
        }
    }
    
    
    protected class DoubleWriter extends ValueWriter
    {
        @Override
        public void writeValue(DataBlock data, int index) throws IOException
        {
            double val = data.getDoubleValue(index);
            writer.write(SWEDataTypeUtils.getDoubleOrInfAsString(val));
        }
    }
    
    
    protected class FloatWriter extends ValueWriter
    {
        @Override
        public void writeValue(DataBlock data, int index) throws IOException
        {
            float val = data.getFloatValue(index);
            
            if (Float.isNaN(val))
                writer.write("NaN");
            else if (val == Float.POSITIVE_INFINITY)
                writer.write("+INF");
            else if (val == Float.NEGATIVE_INFINITY)
                writer.write("-INF");
            else
                writer.write(Float.toString(val));
        }
    }
    
    
    protected class RoundingDecimalWriter extends ValueWriter
    {
        NumberFormat df;
        
        public RoundingDecimalWriter(int numDecimalPlaces)
        {
            this.df = DecimalFormat.getNumberInstance(Locale.US);
            df.setGroupingUsed(false);
            df.setMinimumFractionDigits(1);
            df.setMaximumFractionDigits(numDecimalPlaces);
        }
        
        @Override
        public void writeValue(DataBlock data, int index) throws IOException
        {
            double val = data.getDoubleValue(index);
            
            if (Double.isNaN(val))
                writer.write("NaN");
            else if (val == Double.POSITIVE_INFINITY)
                writer.write("+INF");
            else if (val == Double.NEGATIVE_INFINITY)
                writer.write("-INF");
            else
                writer.write(df.format(val));
        }
    }
    
    
    protected class IsoDateTimeWriter extends ValueWriter
    {
        DateTimeFormat timeFormat = new DateTimeFormat();
        
        @Override
        public void writeValue(DataBlock data, int index) throws IOException
        {
            double val = data.getDoubleValue(index);
            writer.write(timeFormat.formatIso(val, 0));
        }
    }
    
    
    protected class StringWriter extends ValueWriter
    {
        @Override
        public void writeValue(DataBlock data, int index) throws IOException
        {
            String val = data.getStringValue(index);
            if (val != null)
            {
                if (collapseWhiteSpaces)
                    val = val.trim();
                writer.write(val);
            }
        }
    }
    
    
    protected class ImplicitSizeWriter extends ImplicitSizeProcessor
    {
        @Override
        public int process(DataBlock data, int index) throws IOException
        {
            super.process(data, index);
            writeSeparator();
            writer.write(Integer.toString(arraySize));
            return index;
        }
    }
    
    
    protected class ChoiceTokenWriter extends ChoiceProcessor
    {
        ArrayList<String> choiceTokens;
        
        public ChoiceTokenWriter(DataChoice choice)
        {
            choiceTokens = new ArrayList<String>(choice.getNumItems());
            for (DataComponent item: choice.getItemList())
                choiceTokens.add(item.getName());
        }
        
        @Override
        public int process(DataBlock data, int index) throws IOException
        {
            int selectedIndex = data.getIntValue(index);
            if (selectedIndex < 0 || selectedIndex >= choiceTokens.size())
                throw new WriterException(AbstractDataParser.INVALID_CHOICE_MSG + selectedIndex);
            
            if (enabled)
            {
                writeSeparator();
                writer.write(choiceTokens.get(selectedIndex));
            }
            
            return super.process(data, ++index, selectedIndex);
        }
    }
    
    
    protected void writeSeparator() throws IOException
    {
        if (!firstToken)
            writer.write(tokenSep);
        else
            firstToken = false;
    }
    
    
    @Override
    protected void init()
    {
        if (dataEncoding != null)
        {
            this.tokenSep = ((TextEncoding)dataEncoding).getTokenSeparator();
            this.blockSep = ((TextEncoding)dataEncoding).getBlockSeparator();
            //this.decimalSep = ((TextEncoding)dataEncoding).getDecimalSeparator().charAt(0);
            this.collapseWhiteSpaces = ((TextEncoding)dataEncoding).getCollapseWhiteSpaces();
        }
    }
    
    
    @Override
    public void setOutput(OutputStream os)
    {
        this.writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
    }
    
    
    @Override
    public void write(DataBlock data) throws IOException
    {
        firstToken = true;
        super.write(data);
        if (!lastArrayElt)
            writer.write(blockSep);
    }
    

    @Override
    public void flush() throws IOException
    {
        if (writer != null)
            writer.flush();
    }
    

    @Override
    public void close() throws IOException
    {
        if (writer != null)
            writer.close();
    }
    
    
    @Override
    public void visit(Boolean comp)
    {
        addToProcessorTree(new BooleanWriter());
    }
    
    
    @Override
    public void visit(Count comp)
    {
        IntegerWriter writer = new IntegerWriter();
        if (comp.isSetId())
            countWriters.put(comp.getId(), writer);
        addToProcessorTree(writer);
    }
    
    
    @Override
    public void visit(Quantity comp)
    {
        if (comp.getConstraint() != null && comp.getConstraint().isSetSignificantFigures())
        {
            int sigFigures = comp.getConstraint().getSignificantFigures(); 
            addToProcessorTree(new RoundingDecimalWriter(sigFigures));
        }
        else if (comp.getDataType() == DataType.FLOAT)
            addToProcessorTree(new FloatWriter());
        else
            addToProcessorTree(new DoubleWriter());
    }
    
    
    @Override
    public void visit(Time comp)
    {
        if (!comp.isIsoTime())
        {
            if (comp.getConstraint() != null && comp.getConstraint().isSetSignificantFigures())
            {
                int sigFigures = comp.getConstraint().getSignificantFigures(); 
                addToProcessorTree(new RoundingDecimalWriter(sigFigures));
            }
            else
                addToProcessorTree(new DoubleWriter());
        }
        else
            addToProcessorTree(new IsoDateTimeWriter());
    }
    
    
    @Override
    public void visit(Category comp)
    {
        addToProcessorTree(new StringWriter());
    }
    
    
    @Override
    public void visit(Text comp)
    {
        addToProcessorTree(new StringWriter());
    }


    @Override
    protected ChoiceProcessor getChoiceProcessor(DataChoice choice)
    {
        return new ChoiceTokenWriter(choice);
    }
    
    
    @Override
    protected ImplicitSizeProcessor getImplicitSizeProcessor(DataArray array)
    {
        return new ImplicitSizeWriter();
    }
    
    
    @Override
    protected ArraySizeSupplier getArraySizeSupplier(String refId)
    {
        IntegerWriter sizeWriter = countWriters.get(refId);
        return () -> sizeWriter.val;
    }
}
