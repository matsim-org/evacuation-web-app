/*
 * Copyright (c) 2016 Gregor Lämmel
 * This file is part of evacuation-web-app.
 * evacuation-web-app is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * See also LICENSE and WARRANTY file
 */

package org.matsim.contrib.evacuationwebapp.utils;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;
import de.westnordost.osmapi.map.data.LatLon;
import org.geojson.LngLatAlt;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

/**
 * Created by laemmel on 01/11/2016.
 */
public class Transformer {


    private final MathTransform transformation;
    private final MathTransform inverseTransformation;

    public Transformer(String epsg) {
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326", true);
            CoordinateReferenceSystem targetCRS = CRS.decode(epsg, true);
            this.transformation = CRS.findMathTransform(sourceCRS, targetCRS, true);
            this.inverseTransformation = this.transformation.inverse();
        } catch (FactoryException | NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }


    public LngLatAlt toGeographic(double utmX, double utmY) {
        try {
            Coordinate transformed = JTS.transform(new Coordinate(utmX, utmY), null, this.inverseTransformation);
            return new LngLatAlt(transformed.x, transformed.y, transformed.z);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }

    public Envelope toUTM(Envelope e) {
        try {
            return JTS.transform(e, this.transformation);
        } catch (TransformException e1) {
            throw new RuntimeException(e1);
        }
    }

    public Envelope toGeographic(Envelope e) {
        try {
            return JTS.transform(e, this.inverseTransformation);
        } catch (TransformException e1) {
            throw new RuntimeException(e1);
        }
    }

    public Coord toUTM(LatLon pos) {
        try {
            Coordinate transformed = JTS.transform(new Coordinate(pos.getLongitude(), pos.getLatitude()), null, this.transformation);
            return CoordUtils.createCoord(transformed.x, transformed.y);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }


    public Polygon toUTM(Polygon area) {
        try {
            return (Polygon) JTS.transform(area, this.transformation);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }


    public Coord toUTM(LngLatAlt c) {
        try {
            Coordinate transformed = JTS.transform(new Coordinate(c.getLongitude(), c.getLatitude()), null, this.transformation);
            return CoordUtils.createCoord(transformed.x, transformed.y);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }


}
