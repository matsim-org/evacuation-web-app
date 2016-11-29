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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.contrib.evacuationwebapp.evacuation.Session;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.SessionAlreadyExistsException;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.UnknownSessionException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

/**
 * Created by laemmel on 17/11/2016.
 */
public class SessionsManagerTest {
    private volatile AssertionError exc;

    private SessionsManager m;
    private Feature ft1;


    private void validateRoute(FeatureCollection route) throws AssertionError {
        assertThat(route.getFeatures().size(), is(1));

        Double prop = route.getFeatures().get(0).getProperty("time");
        assertThat(prop, is(40.0001));
    }

    private void validateGrid(FeatureCollection grid) throws AssertionError {
        assertThat(grid.getFeatures().size(), is(3));

        {
            Feature f = grid.getFeatures().get(0);
            Object color = f.getProperty("color");
            assertThat(color.toString(), is("lime"));
        }
        {
            Feature f = grid.getFeatures().get(1);
            Object color = f.getProperty("color");
            assertThat(color.toString(), is("orange"));
        }
        {
            Feature f = grid.getFeatures().get(2);
            Object color = f.getProperty("color");
            assertThat(color.toString(), is("green"));
        }

    }

    @Before
    public void intialize() {
        this.m = new SessionsManager(() -> "http://localhost:9090/api/", 5);

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

    @After
    public void shutdown() {
        this.m.shutdown();
        this.m = null;
    }

    @Test
    public void keepAliveTest() {
        Session s = new Session("client", this.ft1);
        this.m.initializeNewSession(s);
        FeatureCollection grid = this.m.getEvacuationAnalysisGrid("client");
        assertThat(grid, notNullValue());
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        FeatureCollection grid2 = this.m.getEvacuationAnalysisGrid("client");
        assertThat(grid2, nullValue());


    }

    @Test
    public void shutdownTest() {
        this.m.shutdown();
    }

    @Test(expected = UnknownSessionException.class)
    public void unknownClientGridTest() {
        Session s = new Session("client1", this.ft1);
        this.m.initializeNewSession(s);
        this.m.getEvacuationAnalysisGrid("client2");
    }

    @Test(expected = UnknownSessionException.class)
    public void unknownClientRouteTest() {
        Session s = new Session("client1", this.ft1);
        this.m.initializeNewSession(s);
        this.m.getEvacuationRoute("client2", null);
    }

    @Test(expected = UnknownSessionException.class)
    public void unknownClientDisconnectTest() {
        Session s = new Session("client1", this.ft1);
        this.m.initializeNewSession(s);
        this.m.disconnect("client2");
    }

    @Test(expected = SessionAlreadyExistsException.class)
    public void sessionAlreadyExistsExceptionTest() {
        Session s = new Session("client1", this.ft1);
        this.m.initializeNewSession(s);
        Session s2 = new Session("client1", this.ft1);
        this.m.initializeNewSession(s2);
    }

    @Test(timeout = 120 * 1000) //timeout 120s
    public void runSeveralSessions() {
        for (int i = 0; i < 4; i++) {
            Session s = new Session("client" + i, this.ft1);
            this.m.initializeNewSession(s);
        }

        for (int i = 0; i < 4; i++) {
            FeatureCollection grid = this.m.getEvacuationAnalysisGrid("client" + i);
            assertThat(grid, notNullValue());
        }
        this.m.disconnect("client0");

        for (int i = 1; i < 4; i++) {
            LngLatAlt ll = new LngLatAlt(-74.03363892451813, 40.753928071164495);
            FeatureCollection route = this.m.getEvacuationRoute("client" + i, ll);
            assertThat(route, notNullValue());
        }

        for (int i = 1; i < 4; i++) {
            this.m.disconnect("client" + i);
        }
    }

    @Test(timeout = 60 * 1000) //timeout 60s
    public void runParallelQueries() {
        Session s = new Session("client", this.ft1);
        this.m.initializeNewSession(s);

        Queue<Thread> threads = new LinkedList<>();

        for (int i = 0; i < 100; i++) {

            Thread t = new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < 100; i++) {
                        FeatureCollection grid = SessionsManagerTest.this.m.getEvacuationAnalysisGrid("client");
                        LngLatAlt ll = new LngLatAlt(-74.03363892451813, 40.753928071164495);
                        FeatureCollection route = SessionsManagerTest.this.m.getEvacuationRoute("client", ll);
                        try {
                            validateGrid(grid);
                            validateRoute(route);
                        } catch (AssertionError e) {
                            exc = e;
                        }
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
            } finally {
                if (exc != null) {
                    throw exc;
                }
            }
        }

    }
}
