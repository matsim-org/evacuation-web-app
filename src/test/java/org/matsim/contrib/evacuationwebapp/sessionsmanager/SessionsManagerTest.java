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

package org.matsim.contrib.evacuationwebapp.sessionsmanager;

import org.geojson.*;
import org.junit.Before;
import org.junit.Test;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.SessionAlreadyExistsException;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.UnknownSessionException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by laemmel on 17/11/2016.
 */
public class SessionsManagerTest {

    private SessionsManager m;
    private Feature ft1;

    @Before
    public void intialize() {
        this.m = new SessionsManager();

        this.ft1 = new Feature();
        List<LngLatAlt> lngLatAlt = new ArrayList<>();
        lngLatAlt.add(new LngLatAlt(-74.034862, 40.755217));
        lngLatAlt.add(new LngLatAlt(-74.035368, 40.753196));
        lngLatAlt.add(new LngLatAlt(-74.032458, 40.752661));
        lngLatAlt.add(new LngLatAlt(-74.031791, 40.754712));
        lngLatAlt.add(new LngLatAlt(-74.034862, 40.755217));
        GeoJsonObject geo = new Polygon(lngLatAlt);
        ft1.setGeometry(geo);
        ft1.setProperty("num", "500");
    }

//    @After
//    public void shutdown() {
//        this.m.shutdown();
//        this.m = null;
//    }

    @Test
    public void shutdownTest() {
        this.m.shutdown();
    }

    @Test(expected = UnknownSessionException.class)
    public void unknownClientGridTest() {
        this.m.initializeNewSession("client1", this.ft1);
        this.m.getEvacuationAnalysisGrid("client2");
    }

    @Test(expected = UnknownSessionException.class)
    public void unknownClientRouteTest() {
        this.m.initializeNewSession("client1", this.ft1);
        this.m.getEvacuationRoute("client2", null);
    }

    @Test(expected = UnknownSessionException.class)
    public void unknownClientDisconnectTest() {
        this.m.initializeNewSession("client1", this.ft1);
        this.m.disconnect("client2");
    }

    @Test(expected = SessionAlreadyExistsException.class)
    public void sessionAlreadyExistsExceptionTest() {
        this.m.initializeNewSession("client1", this.ft1);
        this.m.initializeNewSession("client1", this.ft1);
    }

//    @Test(timeout = 120 * 1000) //timeout 120s
//    public void runSeveralSessions() {
//        for (int i = 0; i < 4; i++) {
//            this.m.initializeNewSession("client" + i, this.ft1);
//        }
//
//        for (int i = 0; i < 4; i++) {
//            FeatureCollection grid = this.m.getEvacuationAnalysisGrid("client" + i);
//            validateGrid(grid);
//        }
//
//        for (int i = 0; i < 4; i++) {
//            LngLatAlt ll = new LngLatAlt(-74.03363892451813, 40.753928071164495);
//            FeatureCollection route = this.m.getEvacuationRoute("client" + i, ll);
//            validateRoute(route);
//        }
//
//        for (int i = 0; i < 4; i++) {
//            this.m.disconnect("client" + i);
//        }
//    }

    @Test(timeout = 60 * 1000) //timeout 60s
    public void runParallelQueries() {
        this.m.initializeNewSession("client", this.ft1);

        Queue<Thread> threads = new LinkedList<>();

        for (int i = 0; i < 100; i++) {

            Thread t = new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < 10000; i++) {
                        FeatureCollection grid = SessionsManagerTest.this.m.getEvacuationAnalysisGrid("client");
                        validateGrid(grid);
                        LngLatAlt ll = new LngLatAlt(-74.03363892451813, 40.753928071164495);
                        FeatureCollection route = SessionsManagerTest.this.m.getEvacuationRoute("client", ll);
                        validateRoute(route);
                    }

                }
            };
            t.start();
            threads.add(t);

        }

        while (threads.peek() != null) {
            Thread t = threads.poll();
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static void validateRoute(FeatureCollection route) {
        assertThat(route.getFeatures().size(), is(2));

        Double prop = route.getFeatures().get(1).getProperty("time");
        assertThat(prop, is(69.52951176470589));
    }

    private static void validateGrid(FeatureCollection grid) {
        assertThat(grid.getFeatures().size(), is(3));

        {
            Feature f = grid.getFeatures().get(0);
            Object color = f.getProperty("color");
            assertThat(color.toString(), is("purple"));
        }
        {
            Feature f = grid.getFeatures().get(1);
            Object color = f.getProperty("color");
            assertThat(color.toString(), is("purple"));
        }
        {
            Feature f = grid.getFeatures().get(2);
            Object color = f.getProperty("color");
            assertThat(color.toString(), is("green"));
        }

    }
}
