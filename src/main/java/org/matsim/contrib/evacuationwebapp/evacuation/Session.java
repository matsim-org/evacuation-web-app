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

package org.matsim.contrib.evacuationwebapp.evacuation;

import com.vividsolutions.jts.geom.Envelope;
import de.westnordost.osmapi.map.data.BoundingBox;
import org.geojson.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuationwebapp.utils.Geometries;
import org.matsim.contrib.evacuationwebapp.utils.Transformer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

/**
 * Created by laemmel on 21/11/2016.
 */
public class Session {

    private final String id;
    private final Feature area;
    private Envelope wgs84E;
    private String epsg;
    private Transformer transformer;
    private Envelope utmE;
    private BoundingBox boundingBox;
    private Scenario sc;


    public Session(String id, Feature evacuationArea) {
        this.id = id;
        this.area = evacuationArea;
    }

    public Envelope getWgs84E() {
        return wgs84E;
    }

    public Scenario getScenario() {
        return sc;
    }

    public String getEpsg() {
        return epsg;
    }

    Transformer getTransformer() {
        return transformer;
    }

    Envelope getUtmE() {
        return utmE;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public String getId() {
        return this.id;
    }

    Feature getArea() {
        return this.area;
    }

    public void prepare() {

        this.wgs84E = Geometries.getEnvelope(area);
        this.epsg = MGC.getUTMEPSGCodeForWGS84Coordinate(wgs84E.getMinX() + wgs84E.getWidth() / 2, wgs84E.getMinY() + wgs84E.getHeight() / 2);
        this.transformer = new Transformer(epsg);
        this.utmE = transformer.toUTM(wgs84E);
        this.boundingBox = new BoundingBox(wgs84E.getMinY(), wgs84E.getMinX(), wgs84E.getMaxY(), wgs84E.getMaxX());
        Config c = ConfigUtils.createConfig();
        this.sc = ScenarioUtils.createScenario(c);


    }
}
