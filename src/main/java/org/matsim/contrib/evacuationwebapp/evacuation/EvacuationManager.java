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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.handler.MapDataHandler;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geojson.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.evacuationwebapp.utils.DefaultDemandGenerator;
import org.matsim.contrib.evacuationwebapp.utils.Geometries;
import org.matsim.contrib.evacuationwebapp.utils.MATSimScenarioGenerator;
import org.matsim.contrib.evacuationwebapp.utils.Transformer;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by laemmel on 01/11/2016.
 */
public class EvacuationManager {

    private static final Logger log = Logger.getLogger(EvacuationManager.class);

    private static final int MAX_DEMAND = 25000;

    private final Feature evacuationArea;
    private FeatureCollection grid;
    private Transformer transformer;

    private LeastCostPathCalculator router;
    private Scenario sc;
    private Node safeNode;
    private Id<Link> sl;
    private OSMNetwork osmNetwork;


    private boolean isInitialized = false;

    public EvacuationManager(Feature evacuationArea) {
        this.evacuationArea = evacuationArea;
    }

    public synchronized void init() {
        if (isInitialized) {
            return;
        }


        Envelope e = Geometries.getEnvelope(evacuationArea);
        String epsg = MGC.getUTMEPSGCodeForWGS84Coordinate(e.getMinX() + e.getWidth() / 2, e.getMinY() + e.getHeight() / 2);
        this.transformer = new Transformer(epsg);
        Envelope utmE = this.transformer.toUTM(e);


        int demand = Integer.parseInt((String) evacuationArea.getProperties().get("num"));
        double sample = 1;
        if (demand > MAX_DEMAND) {
            sample = (double) MAX_DEMAND / demand;
            demand = MAX_DEMAND;
        }

        this.sc = MATSimScenarioGenerator.createScenario(sample);


        BoundingBox boundingBox = new BoundingBox(e.getMinY(), e.getMinX(), e.getMaxY(), e.getMaxX());

        this.osmNetwork = new OSMNetwork(utmE);

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(BoundingBox.class).toInstance(boundingBox);
                bind(Transformer.class).toInstance(EvacuationManager.this.transformer);
                bind(Network.class).toInstance(sc.getNetwork());
                bind(MapDataHandler.class).to(OSMMapDataHandler.class);
                bind(OSMNetwork.class).toInstance(EvacuationManager.this.osmNetwork);
                bind(OSMWayFilter.class).to(OSMVehicularFilter.class);
            }
        });

        OSMParser parser = injector.getInstance(OSMParser.class);

        parser.run();

        osmNetwork.createMATSimNetwork();


        this.sl = Id.createLinkId("safe");
        com.vividsolutions.jts.geom.Polygon area = Geometries.toJTS((Polygon) evacuationArea.getGeometry());
        com.vividsolutions.jts.geom.Polygon utmArea = this.transformer.toUTM(area);

        EvacuationNetworkGenerator netGen = new EvacuationNetworkGenerator(sc, utmArea, sl);
        netGen.run();
        this.safeNode = this.sc.getNetwork().getLinks().get(sl).getToNode();

        osmNetwork.consolidate();

//        DummyDemand.createDummyDemand(sc, sl);


        DefaultDemandGenerator.createDemand(sc, sl, demand);


        Grid grid = new Grid(utmE, utmArea);


        Controler c = new Controler(sc);
        EvacuationTimeObserver obs = new EvacuationTimeObserver(grid, sc);
        c.getEvents().addHandler(obs);

        Level level = Logger.getRootLogger().getLevel();
        Logger.getRootLogger().setLevel(Level.WARN);//Make output less verbose
        c.run();
        Logger.getRootLogger().setLevel(level);//restore log level

        LeastCostPathCalculatorFactory fac = c.getLeastCostPathCalculatorFactory();

        TravelDisutility travelCost = c.getTravelDisutilityFactory().createTravelDisutility(c.getLinkTravelTimes());

        this.router = fac.createPathCalculator(sc.getNetwork(), travelCost, c.getLinkTravelTimes());
        obs.updateCellColors();

        this.grid = new FeatureCollection();
        boolean first = true;
        for (Grid.Cell cell : grid.getCells()) {
            if (cell.c == Grid.CellColor.white) {
                continue;
            }
            com.vividsolutions.jts.geom.Polygon p = cell.p;

            Feature ret = new Feature();
            List<LngLatAlt> l = new ArrayList<>();
            for (Coordinate coord : p.getExteriorRing().getCoordinates()) {
                l.add(this.transformer.toGeographic(coord.x, coord.y));
            }
            GeoJsonObject geo = new Polygon(l);
            ret.setGeometry(geo);
            ret.setProperty("color", cell.c.toString());
            ret.setProperty("time", cell.time);
            if (first) {
                double maxTT = obs.getMAXTT();
                ret.setProperty("green", maxTT * 0.3);
                ret.setProperty("lime", maxTT * 0.4);
                ret.setProperty("yellow", maxTT * 0.5);
                ret.setProperty("orange", maxTT * 0.6);
                ret.setProperty("red", maxTT * 0.7);
                ret.setProperty("fuchsia", maxTT * 0.8);
                ret.setProperty("purple", maxTT);
                first = false;
            }

            this.grid.add(ret);
        }

        isInitialized = true;

    }


    public synchronized FeatureCollection getRoute(LngLatAlt start) {
        init();
        Coord coord = this.transformer.toUTM(start);
        Node from = this.osmNetwork.getClosestNode(coord);
        Person p = sc.getPopulation().getFactory().createPerson(Id.createPersonId(0));
        Vehicle v = VehicleUtils.getFactory().createVehicle(Id.createVehicleId(0), VehicleUtils.getDefaultVehicleType());
        FeatureCollection route = new FeatureCollection();
        try {
            LeastCostPathCalculator.Path r = this.router.calcLeastCostPath(from, safeNode, 0, p, v);

            for (Link link : r.links) {
                if (link.getId().equals(sl) || link.getId().toString().contains("el")) {
                    continue;
                }
                Feature ft = new Feature();

                List<LngLatAlt> trace = this.osmNetwork.traceLink(link.getId());
                GeoJsonObject geo = new org.geojson.LineString(trace.toArray(new LngLatAlt[0]));
                ft.setGeometry(geo);
                ft.setProperty("time", r.travelTime);
                route.add(ft);

            }
        } catch (Exception e) {
            log.warn(e);
        }

        return route;

    }




    public FeatureCollection getFeatureCollection() {
        init();
        return this.grid;
    }


}
