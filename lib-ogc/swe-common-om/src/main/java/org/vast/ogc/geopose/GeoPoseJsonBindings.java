/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2018, Sensia Software LLC
 All Rights Reserved. This software is the property of Sensia Software LLC.
 It cannot be duplicated, used, or distributed without the express written
 consent of Sensia Software LLC.
 
******************************* END LICENSE BLOCK ***************************/

package org.vast.ogc.geopose;

import java.io.IOException;
import org.vast.swe.SWEConstants;
import org.vast.util.Asserts;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;


/**
 * <p>
 * JSON bindings for GeoPose objects using Gson JsonWriter/JsonReader.<br/>
 * This class is NOT threadsafe.
 * </p>
 *
 * @author Alex Robin
 * @date Sep 27, 2023
 */
public class GeoPoseJsonBindings
{
    public static final String ERROR_INVALID_COORDINATES = "Invalid coordinate array";
    
    
    /////////////////////
    // Writing methods //
    /////////////////////
    
    public void writeGeoPose(JsonWriter writer, Pose pose) throws IOException
    {
        Asserts.checkArgument(isGeoPose(pose), "Not a GeoPose");
        writePose(writer, pose);
    }
    
    
    public void writePose(JsonWriter writer, Pose pose) throws IOException
    {
        if (isGeoPose(pose))
            writePose(writer, pose, "GeoPose");
        else
            writePose(writer, pose, "RelativePose");
    }
    
    
    protected void writePose(JsonWriter writer, Pose pose, String typeString) throws IOException
    {
        writer.beginObject();
        
        if (typeString != null)
            writer.name("type").value(typeString);
        
        if (isGeoPose(pose))
        {
            var ltp = pose.getLTPReferenceFrame();
            if (ltp != null && !ltp.equals(SWEConstants.REF_FRAME_ENU))
                writer.name("ltpReferenceFrame").value(pose.getLTPReferenceFrame());
        }
        else
        {
            if (pose.getReferenceFrame() != null)
                writer.name("referenceFrame").value(pose.getReferenceFrame());
        }
        
        if (pose.getLocalFrame() != null)
            writer.name("localFrame").value(pose.getReferenceFrame());
        
        if (pose.getPosition() != null)
        {
            var coords = pose.getPosition();
            writer.name("position").beginObject();
            
            if (SWEConstants.REF_FRAME_CRS84.equals(pose.getReferenceFrame()))
            {
                writer.name("lat").value(coords[1]);
                writer.name("lon").value(coords[0]);
            }
            else if (SWEConstants.REF_FRAME_4326.equals(pose.getReferenceFrame()))
            {
                writer.name("lat").value(coords[0]);
                writer.name("lon").value(coords[1]);
            }
            else if (SWEConstants.REF_FRAME_CRS84h.equals(pose.getReferenceFrame()))
            {
                writer.name("lat").value(coords[1]);
                writer.name("lon").value(coords[0]);
                writer.name("h").value(coords[2]);
            }
            else if (SWEConstants.REF_FRAME_4979.equals(pose.getReferenceFrame()))
            {
                writer.name("lat").value(coords[0]);
                writer.name("lon").value(coords[1]);
                writer.name("h").value(coords[2]);
            }
            else
            {
                writer.name("x").value(coords[0]);
                writer.name("y").value(coords[1]);
                writer.name("z").value(coords[2]);
            }
            
            writer.endObject();
        }
        
        if (pose.getOrientation() != null)
        {
            var coords = pose.getOrientation();
            if (coords.length == 3)
            {
                writer.name("angles").beginObject();
                writer.name("yaw").value(coords[0]);
                writer.name("pitch").value(coords[1]);
                writer.name("roll").value(coords[2]);
                writer.endObject();
            }
            else if (coords.length == 4)
            {
                writer.name("quaternion").beginObject();
                writer.name("x").value(coords[0]);
                writer.name("y").value(coords[1]);
                writer.name("z").value(coords[2]);
                writer.name("w").value(coords[3]);
                writer.endObject();
            }
        }
        
        writer.endObject();
    }
    
    
    public Pose readPose(JsonReader reader) throws IOException
    {
        var pose = new PoseImpl();
        boolean isGeo = false;
        
        if (reader.peek() == JsonToken.BEGIN_OBJECT)
            reader.beginObject();
        
        while (reader.hasNext())
        {
            String name = reader.nextName();
            
            if ("referenceFrame".equals(name))
                pose.setReferenceFrame(reader.nextString());
            
            else if ("ltpReferenceFrame".equals(name))
                pose.setLTPReferenceFrame(reader.nextString());
            
            else if ("localFrame".equals(name))
                pose.setLocalFrame(reader.nextString());
            
            else if ("position".equals(name))
            {
                var coords = new double[3];
                
                reader.beginObject();
                while (reader.hasNext())
                {
                    name = reader.nextName();
                    if ("lat".equals(name))
                    {
                        coords[1] = reader.nextDouble();
                        isGeo = true;
                    }
                    else if ("lon".equals(name))
                        coords[0] = reader.nextDouble();
                    else if ("h".equals(name))
                        coords[2] = reader.nextDouble();
                    else if ("x".equals(name))
                        coords[0] = reader.nextDouble();
                    else if ("y".equals(name))
                        coords[1] = reader.nextDouble();
                    else if ("z".equals(name))
                        coords[2] = reader.nextDouble();
                    else
                        reader.skipValue();
                }
                reader.endObject();
                
                pose.setPosition(coords);
            }
            
            else if ("angles".equals(name))
            {
                var coords = new double[3];
                
                reader.beginObject();
                while (reader.hasNext())
                {
                    name = reader.nextName();
                    if ("yaw".equals(name))
                        coords[0] = reader.nextDouble();
                    else if ("pitch".equals(name))
                        coords[1] = reader.nextDouble();
                    else if ("roll".equals(name))
                        coords[2] = reader.nextDouble();
                    else
                        reader.skipValue();
                }
                reader.endObject();
                
                pose.setOrientation(coords);
            }
            
            else if ("quaternion".equals(name))
            {
                var coords = new double[4];
                
                reader.beginObject();
                while (reader.hasNext())
                {
                    name = reader.nextName();
                    if ("x".equals(name))
                        coords[0] = reader.nextDouble();
                    else if ("y".equals(name))
                        coords[1] = reader.nextDouble();
                    else if ("z".equals(name))
                        coords[2] = reader.nextDouble();
                    else if ("w".equals(name))
                        coords[3] = reader.nextDouble();
                    else
                        reader.skipValue();
                }
                reader.endObject();
                
                pose.setOrientation(coords);
            }
            
            else
                reader.skipValue();
        }
        
        reader.endObject();
        
        // set default CRS
        if (isGeo)
        {
            int numDims = pose.getPosition().length;
            if (numDims == 2)
                pose.setReferenceFrame(SWEConstants.REF_FRAME_CRS84);
            else
                pose.setReferenceFrame(SWEConstants.REF_FRAME_CRS84h);
        }
        
        return pose;
    }
    
    
    public boolean isGeoPose(Pose pose)
    {
        var crs = pose.getReferenceFrame();
        var isCompatibleCrs = SWEConstants.REF_FRAME_CRS84.equals(crs) || 
                              SWEConstants.REF_FRAME_CRS84h.equals(crs) ||
                              SWEConstants.REF_FRAME_4326.equals(crs) ||
                              SWEConstants.REF_FRAME_4979.equals(crs);
        
        //var ltp = pose.getLTPReferenceFrame();
        return isCompatibleCrs;// && SWEConstants.REF_FRAME_ENU.equals(ltp);
    }
}
