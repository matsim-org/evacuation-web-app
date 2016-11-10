/*
 * Copyright (c) 2016 Gregor Lämmel
 * This file is part of evacuation.
 * evacuation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * See also LICENSE and WARRANTY file
 */

package org.matsim.contrib.evacuationwebapp.utils;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import org.apache.log4j.Logger;
import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;

import java.util.Iterator;


/**
 * Created by laemmel on 02/11/2016.
 */

public abstract class Geometries {

    private static final Logger log = Logger.getLogger(Geometries.class);

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public static com.vividsolutions.jts.geom.Polygon toJTS(Polygon geoJSON) {

        if (geoJSON.getInteriorRings().size() > 0) {
            log.warn("Conversion of polygons w/ holes is not supported! Holes are ignored.");
        }

        Coordinate[] coordinates = new Coordinate[geoJSON.getExteriorRing().size()];
        int idx = 0;
        for (LngLatAlt lngLatAlt : geoJSON.getExteriorRing()) {
            coordinates[idx++] = new Coordinate(lngLatAlt.getLongitude(), lngLatAlt.getLatitude(), lngLatAlt.getAltitude());
        }

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coordinates);
        return GEOMETRY_FACTORY.createPolygon(shell, null);
    }

    public static com.vividsolutions.jts.geom.Polygon createPolygon(Coordinate[] coordinates) {
        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(coordinates);
        return GEOMETRY_FACTORY.createPolygon(shell, null);
    }

    public static Envelope getEnvelope(Feature evacuationArea) {
        Iterator<LngLatAlt> rng = ((Polygon) evacuationArea.getGeometry()).getExteriorRing().iterator();
        LngLatAlt nxt = rng.next();
        Envelope e = new Envelope(new Coordinate(nxt.getLongitude(), nxt.getLatitude()));
        while (rng.hasNext()) {
            nxt = rng.next();
            e.expandToInclude(nxt.getLongitude(), nxt.getLatitude());
        }

        return e;
    }

}
