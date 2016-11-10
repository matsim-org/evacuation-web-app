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

package org.matsim.contrib.evacuationwebapp.manager;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.geojson.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Created by laemmel on 09/11/2016.
 */
public class EvacuationManagerTest {


    @Test
    public void testRun() {

        Feature ft = new Feature();
        List<LngLatAlt> lngLatAlt = new ArrayList<>();
        lngLatAlt.add(new LngLatAlt(-74.034862, 40.755217));
        lngLatAlt.add(new LngLatAlt(-74.035368, 40.753196));
        lngLatAlt.add(new LngLatAlt(-74.032458, 40.752661));
        lngLatAlt.add(new LngLatAlt(-74.031791, 40.754712));
        lngLatAlt.add(new LngLatAlt(-74.034862, 40.755217));

        GeoJsonObject geo = new Polygon(lngLatAlt);
        ft.setGeometry(geo);

        ft.setProperty("num", "500");

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Feature.class).toInstance(ft);

            }
        });

        EvacuationManager em = injector.getInstance(EvacuationManager.class);
        em.run();
        FeatureCollection ftcoll = em.getFeatureCollection();

        assertThat(ftcoll.getFeatures().size(), is(3));

        {
            Feature f = ftcoll.getFeatures().get(0);
            Object color = f.getProperty("color");
            assertThat(color.toString(), is("purple"));
        }
        {
            Feature f = ftcoll.getFeatures().get(1);
            Object color = f.getProperty("color");
            assertThat(color.toString(), is("purple"));
        }
        {
            Feature f = ftcoll.getFeatures().get(2);
            Object color = f.getProperty("color");
            assertThat(color.toString(), is("green"));
        }

        LngLat ll = new LngLat(-74.03363892451813, 40.753928071164495);

        FeatureCollection route = em.getRoute(ll);
        assertThat(route.getFeatures().size(), is(2));

        Double prop = route.getFeatures().get(1).getProperty("time");
        assertThat(prop, is(69.52951176470589));

    }
}
