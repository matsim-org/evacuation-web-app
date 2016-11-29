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
import org.apache.log4j.Logger;
import org.geojson.LngLatAlt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

/**
 * Created by laemmel on 09/11/2016.
 */
public class TransformerTest {


    private static final Logger log = Logger.getLogger(Transformer.class);
    private static final double GEO_EPS = 0.00001;
    private static final double UTM_EPS = 0.001;
    private Transformer transformer;

    @Before
    public void initTransformer() {
        this.transformer = new Transformer("EPSG:32616");
    }

    @After
    public void destroyTransformer() {
        this.transformer = null;
    }

    @Test
    public void toGeographicXY() {
        double x = 1600636.727802;
        double y = 4596780.663655;

        LngLatAlt geographic = this.transformer.toGeographic(x, y);

        assertThat(geographic.getLatitude(), is(closeTo(40.77953338, GEO_EPS)));
        assertThat(geographic.getLongitude(), is(closeTo(-73.97232055, GEO_EPS)));
    }

    @Test
    public void toUTME() {
        log.warn("Envelop transformation tests need to be revised!!");
        double latMin = 40.726318359376;
        double longMin = -74.032402038575;
        double latMax = 40.799928255186;
        double longMax = -73.92796250815;

        Envelope geo = new Envelope(longMin, longMax, latMin, latMax);
        Envelope utm = this.transformer.toUTM(geo);


        double xMin = 1595207.23515054;
        double yMin = 4590085.465504461;
        double xMax = 1605291.7539049124;
        double yMax = 4599621.415987491;


        assertThat(utm.getMinX(), is(closeTo(xMin, UTM_EPS)));
        assertThat(utm.getMinY(), is(closeTo(yMin, UTM_EPS)));
        assertThat(utm.getMaxX(), is(closeTo(xMax, UTM_EPS)));
        assertThat(utm.getMaxY(), is(closeTo(yMax, UTM_EPS)));

    }

    @Test
    public void toGeographicE() {
        log.warn("Envelop transformation tests need to be revised!!");

        double xMin = 1595207.23515054;
        double yMin = 4590085.465504461;
        double xMax = 1605291.7539049124;
        double yMax = 4599621.415987491;
        Envelope utm = new Envelope(xMin, xMax, yMin, yMax);
        Envelope geo = this.transformer.toGeographic(utm);
    }

    @Test
    public void toUTMCLatLon() {
        Coord c = this.transformer.toUTM(new LatLon() {
            @Override
            public double getLatitude() {
                return 40.77953338;
            }

            @Override
            public double getLongitude() {
                return -73.97232055;
            }
        });
        assertThat(c.getX(), is(closeTo(1600636.727802, UTM_EPS)));
        assertThat(c.getY(), is(closeTo(4596780.663655, UTM_EPS)));
    }

    @Test
    public void toUTMP() {
        Coordinate geoC0 = new Coordinate(-74.018662240612, 40.7001362886);
        Coordinate utmC0 = new Coordinate(1598043.346341, 4587343.782465);
        Coordinate geoC1 = new Coordinate(-74.009096168214, 40.745517715658);
        Coordinate utmC1 = new Coordinate(1598093.413237, 4592521.359569);
        Coordinate geoC2 = new Coordinate(-73.980220026845, 40.794976435571);
        Coordinate utmC2 = new Coordinate(1599707.902804, 4598399.925715);
        Coordinate geoC3 = new Coordinate(-73.934953148735, 40.872582981252);
        Coordinate utmC3 = new Coordinate(1602228.271411, 4607625.166892);
        Coordinate geoC4 = new Coordinate(-73.914938562271, 40.869093530533);
        Coordinate utmC4 = new Coordinate(1603979.782075, 4607493.694333);
        Coordinate geoC5 = new Coordinate(-73.934898376465, 40.795669555665);
        Coordinate utmC5 = new Coordinate(1603533.161693, 4599057.926752);
        Coordinate geoC6 = new Coordinate(-73.954811096192, 40.765668999066);
        Coordinate utmC6 = new Coordinate(1602353.184466, 4595460.41162);
        Coordinate geoC7 = new Coordinate(-73.976783752442, 40.742797851564);
        Coordinate utmC7 = new Coordinate(1600876.858845, 4592631.178106);
        Coordinate geoC8 = new Coordinate(-73.980216979981, 40.71258544922);
        Coordinate utmC8 = new Coordinate(1601093.815469, 4589221.49967);
        Coordinate geoC9 = new Coordinate(-74.006996154785, 40.699780025483);
        Coordinate utmC9 = new Coordinate(1599038.440761983, 4587452.869925721);
        Coordinate geoC10 = new Coordinate(-74.018662240612, 40.7001362886);
        Coordinate utmC10 = new Coordinate(1598043.346341, 4587343.782465);

        Polygon geoP = Geometries.createPolygon(new Coordinate[]{geoC0, geoC1, geoC2, geoC3, geoC4, geoC5, geoC6, geoC7,
                geoC8, geoC9, geoC10});

        Polygon utmP = this.transformer.toUTM(geoP);

        Coordinate[] utmCoords = new Coordinate[]{utmC0, utmC1, utmC2, utmC3, utmC4, utmC5, utmC6, utmC7, utmC8, utmC9, utmC10};
        for (int i = 0; i < utmP.getExteriorRing().getCoordinates().length; i++) {
            Coordinate c = utmP.getExteriorRing().getCoordinates()[i];
            assertThat(c.x, is(closeTo(utmCoords[i].x, UTM_EPS)));
            assertThat(c.y, is(closeTo(utmCoords[i].y, UTM_EPS)));

        }

    }

    @Test
    public void toUTMCLngLatAlt() {
        Coord c = this.transformer.toUTM(new LngLatAlt(-73.97232055, 40.77953338));
        assertThat(c.getX(), is(closeTo(1600636.727802, UTM_EPS)));
        assertThat(c.getY(), is(closeTo(4596780.663655, UTM_EPS)));
    }
}
