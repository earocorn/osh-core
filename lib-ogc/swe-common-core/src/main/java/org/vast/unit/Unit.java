/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is the "SensorML DataProcessing Engine".
 
 The Initial Developer of the Original Code is the VAST team at the University of Alabama in Huntsville (UAH). <http://vast.uah.edu> Portions created by the Initial Developer are Copyright (C) 2007 the Initial Developer. All Rights Reserved. Please Contact Mike Botts <mike.botts@uah.edu> for more information.
 
 Contributor(s): 
 Alexandre Robin <robin@nsstc.uah.edu>
 
 ******************************* END LICENSE BLOCK ***************************/

package org.vast.unit;

import java.io.Serializable;
import java.util.Objects;
import org.vast.util.NumberUtils;

/**
 * <p>
 * Unit object containing relationship to SI base units.
 * </p>
 *
 * @author Alex Robin
 * @since May 4, 2006
 * */
public class Unit implements Serializable
{
    private static final long serialVersionUID = -5219489147804299847L;
    
    protected String name;
    protected String code;
    protected String printSymbol;
    protected String property;
    protected String description;
    protected String expression;
    
    // base unit powers
    protected double meter = 0.0;
    protected double kilogram = 0.0;
    protected double second = 0.0;
    protected double ampere = 0.0;
    protected double kelvin = 0.0;
    protected double mole = 0.0;
    protected double candela = 0.0;
    protected double radian = 0.0;
    protected double pi = 0.0;
    
    // function for special units
    protected double scaleToSI = 1.0;
    protected boolean metric = false;
    protected UnitFunction function;
    

    /**
     * Copies this unit integrally
     * @return a copy of this unit
     */
    public Unit copy()
    {
        Unit newUnit = new Unit();
        
        newUnit.name = this.name;
        newUnit.code = this.code;        
        newUnit.printSymbol = this.printSymbol;
        newUnit.property = this.property;
        newUnit.description = this.description;
        newUnit.expression = this.expression;
        
        newUnit.meter = this.meter;
        newUnit.kilogram = this.kilogram;
        newUnit.second = this.second;
        newUnit.radian = this.radian;
        newUnit.ampere = this.ampere;
        newUnit.kelvin = this.kelvin;
        newUnit.mole = this.mole;
        newUnit.candela = this.candela;
        newUnit.pi = this.pi;
        newUnit.scaleToSI = this.scaleToSI;
        newUnit.metric = this.metric;
        newUnit.function = this.function;
        
        return newUnit;
    }
    
    
    /**
     * Checks that this unit is physically compatible with the
     * given unit, which also means that it is possible to
     * convert from one to the other.
     * @param unit
     * @return true if unit is compatible with this unit
     */
    public boolean isCompatible(Unit unit)
    {
        // check that powers are equal for all SI base units
        return (NumberUtils.ulpEquals(this.meter, unit.meter) &&
                NumberUtils.ulpEquals(this.kilogram, unit.kilogram) &&
                NumberUtils.ulpEquals(this.second, unit.second) &&
                NumberUtils.ulpEquals(this.radian, unit.radian) &&
                NumberUtils.ulpEquals(this.ampere, unit.ampere) &&
                NumberUtils.ulpEquals(this.kelvin, unit.kelvin) &&
                NumberUtils.ulpEquals(this.mole, unit.mole) &&
                NumberUtils.ulpEquals(this.candela, unit.candela));
    }
    
    
    /**
     * @param unit
     * @return true if this unit is equivalent to this unit
     */
    public boolean isEquivalent(Unit unit)
    {
        return (isCompatible(unit) &&
                NumberUtils.ulpEquals(this.scaleToSI, unit.scaleToSI) &&
                NumberUtils.ulpEquals(this.pi, unit.pi) &&
                Objects.equals(this.function, unit.function));
    }
    
    
    /**
     * Raises this unit to the given power, thus modifying
     * all powers of metric base SI coefs.
     * @param power
     */
    public void power(double power)
    {
        this.meter *= power;
        this.kilogram *= power;
        this.second *= power;
        this.radian *= power;
        this.ampere *= power;
        this.kelvin *= power;
        this.mole *= power;
        this.candela *= power;
        this.pi *= power;        
        this.scaleToSI = Math.pow(this.scaleToSI, power);
    }
    
    
    /**
     * Multiply this unit by another unit to generate a complex unit
     * @param unit
     */
    public void multiply(Unit unit)
    {
        this.meter += unit.meter;
        this.kilogram += unit.kilogram;
        this.second += unit.second;
        this.radian += unit.radian;
        this.ampere += unit.ampere;
        this.kelvin += unit.kelvin;
        this.mole += unit.mole;
        this.candela += unit.candela;
        this.pi += unit.pi;
        this.scaleToSI *= unit.scaleToSI;
        if (unit.function != null)
            this.function = unit.function;
    }
    
    
    public Unit getCompatibleSIUnit()
    {
        Unit siUnit = this.copy();
        siUnit.scaleToSI = 1.0;
        siUnit.code = null;
        siUnit.expression = null;
        siUnit.name = null;
        siUnit.description = null;
        siUnit.function = null;
        return siUnit;
    }
    
    
    public void multiply(double scale)
    {
        this.scaleToSI *= scale;
    }
    
    
    public String getName()
    {
        return name;
    }


    public Unit setName(String name)
    {
        this.name = name;
        return this;
    }
    
    
    public String getCode()
    {
        return code;
    }


    public Unit setCode(String code)
    {
        this.code = code;
        return this;
    }


	/**
	* @return the print symbol of this Unit. If the print symbol is not set 
	* (i.e. null or empty), then the expression will be returned (if that is
	* not null and not empty).
	*/
	public String getPrintSymbol()
	{
        // use printSymbol if set
		if (printSymbol != null)
			return printSymbol;
		
		// try to use code
		if (code != null)
		{
			// handle special case of weird unit codes
			if (code.equals("KiBy"))
				printSymbol = "KB";
			
			else if (code.equals("MiBy"))
				printSymbol = "MB";
			
			else if (code.equals("GiBy"))
				printSymbol = "GB";
		
			else if (code.equals("Cel"))
				printSymbol = "°C";
		}
        
		// try to use expression
		else if (expression != null)
			printSymbol = getExpression();
        
        return printSymbol;
    }


    public Unit setPrintSymbol(String symbol)
    {
        this.printSymbol = symbol;
        return this;
    }
    
    
    public String getExpression()
    {
        if (expression == null)
        	expression = getUCUMExpression();
        
    	return expression;
    }


    public Unit setExpression(String expression)
    {
        this.expression = expression;
        return this;
    }


    public String getProperty()
    {
        return property;
    }


    public Unit setProperty(String property)
    {
        this.property = property;
        return this;
    }
    
    
    public String getDescription()
    {
        return description;
    }


    public Unit setDescription(String description)
    {
        this.description = description;
        return this;
    }


    public boolean isMetric()
    {
        return metric;
    }


    public Unit setMetric(boolean metric)
    {
        this.metric = metric;
        return this;
    }


    public double getAmpere()
    {
        return ampere;
    }


    public Unit setAmpere(double ampere)
    {
        this.ampere = ampere;
        return this;
    }


    public double getCandela()
    {
        return candela;
    }


    public Unit setCandela(double candela)
    {
        this.candela = candela;
        return this;
    }


    public double getKelvin()
    {
        return kelvin;
    }


    public Unit setKelvin(double kelvin)
    {
        this.kelvin = kelvin;
        return this;
    }


    public double getKilogram()
    {
        return kilogram;
    }


    public Unit setKilogram(double kilogram)
    {
        this.kilogram = kilogram;
        return this;
    }


    public double getMeter()
    {
        return meter;
    }


    public Unit setMeter(double meter)
    {
        this.meter = meter;
        return this;
    }


    public double getMole()
    {
        return mole;
    }


    public Unit setMole(double mole)
    {
        this.mole = mole;
        return this;
    }


    public double getPi()
    {
        return pi;
    }


    public Unit setPi(double pi)
    {
        this.pi = pi;
        return this;
    }


    public double getRadian()
    {
        return radian;
    }


    public Unit setRadian(double radian)
    {
        this.radian = radian;
        return this;
    }


    public double getSecond()
    {
        return second;
    }


    public Unit setSecond(double second)
    {
        this.second = second;
        return this;
    }
    
    
    public double getScaleToSI()
    {
        return scaleToSI * Math.pow(Math.PI, pi);
    }


    public Unit setScaleToSI(double scaleToSI)
    {
        this.scaleToSI = scaleToSI;
        return this;
    }
    
    
    public UnitFunction getFunction()
    {
        return function;
    }


    public Unit setFunction(UnitFunction function)
    {
        this.function = function;
        return this;
    }
    
    
    public String getUCUMExpression()
    {
        if (code != null)
            return getCode();
        
        StringBuilder buf = new StringBuilder();
        
        addUnitString(buf, radian, "rad");
        addUnitString(buf, meter, "m");
        addUnitString(buf, kilogram, "kg");
        addUnitString(buf, second, "s");
        addUnitString(buf, ampere, "A");
        addUnitString(buf, kelvin, "K");
        addUnitString(buf, mole, "mol");
        addUnitString(buf, candela, "cd");
        
        if (!NumberUtils.ulpEquals(scaleToSI, 1.0))
            buf.insert(0, getScaleToSI() + "*");
        
        return buf.toString();
    }
    
    
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        
        addUnitString(buf, radian, "rad");
        addUnitString(buf, meter, "m");
        addUnitString(buf, kilogram, "kg");
        addUnitString(buf, second, "s");
        addUnitString(buf, ampere, "A");
        addUnitString(buf, kelvin, "K");
        addUnitString(buf, mole, "mol");
        addUnitString(buf, candela, "cd");
        
        // insert function symbol
        if (function != null)
        {
            var nestedUnit = buf.toString();
            buf.setLength(0);
            buf.append(function.toString(nestedUnit));
        }
        
        // insert scale factor
        if (!NumberUtils.ulpEquals(scaleToSI, 1.0))
            buf.insert(0, getScaleToSI() + "*");
        
        // insert code or expression
        if (getCode() != null)
            buf.insert(0, getCode() + " = ");            
        else
            buf.insert(0, getExpression() + " = ");
            
        // also append unit name and printSymbol
        buf.append("  (" + getName() + " - " + getPrintSymbol() + ")");
        
        return buf.toString();
    }
    
    
    private void addUnitString(StringBuilder buf, double val, String sym)
    {
        int ival = (int)val;
        
        if (ival != 0)
        {
            if (buf.length() > 0)
                buf.append('.');
            
            buf.append(sym);
            if (ival != 1)
                buf.append(ival);                        
        }
    }
}
