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

package org.matsim.contrib.evacuationwebapp.model;

import com.google.inject.Inject;
import com.vividsolutions.jts.geom.Coordinate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geojson.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.evacuationwebapp.utils.DefaultDemandGenerator;
import org.matsim.contrib.evacuationwebapp.utils.Geometries;
import org.matsim.contrib.evacuationwebapp.utils.MATSimScenarioGenerator;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by laemmel on 01/11/2016.
 */
public class EvacuationManager {

    private static final Logger log = Logger.getLogger(EvacuationManager.class);

    private static final int MAX_np_DEMAND = 25000;
    private static final int MAX_100p_DEMAND = 5000;
    @Inject
    Session session;
    @Inject
    OSMParser parser;
    private FeatureCollection gridColl;
    private LeastCostPathCalculator router;
    //    private Scenario sc;
    private Node safeNode;
    private Id<Link> sl;
    @Inject
    private OSMNetwork osmNetwork;
    private boolean isInitialized = false;
    private Grid grid;


    public synchronized void init() {
        if (isInitialized) {
            return;
        }

        int demand = Integer.parseInt((String) session.getArea().getProperties().get("num"));
        double sample = 1;
        if (demand > MAX_np_DEMAND) {
            sample = (double) MAX_np_DEMAND / demand;
            demand = MAX_np_DEMAND;
        } else if (demand > MAX_100p_DEMAND) {
            sample = 0.1;
            demand = (int) (demand * sample);
        }

        MATSimScenarioGenerator.createScenario(sample, session);

        parser.run(this.osmNetwork);

        osmNetwork.createMATSimNetwork();

        this.sl = Id.createLinkId("safe");
        com.vividsolutions.jts.geom.Polygon area = Geometries.toJTS((Polygon) session.getArea().getGeometry());
        com.vividsolutions.jts.geom.Polygon utmArea = session.getTransformer().toUTM(area);

        Scenario sc = this.session.getScenario();

        EvacuationNetworkGenerator netGen = new EvacuationNetworkGenerator(sc, utmArea, sl);
        netGen.run();
        this.safeNode = sc.getNetwork().getLinks().get(sl).getToNode();

        osmNetwork.consolidate();

        DefaultDemandGenerator.createDemand(sc, sl, demand);

        this.grid = new Grid(session.getUtmE(), utmArea);

        Controler cntr = new Controler(sc);

        EvacuationTimeObserver obs = new EvacuationTimeObserver(grid, sc, sample);
//        cntr.getEvents().addHandler(obs);
        ObserverController obsCntr = new ObserverController(obs);
        cntr.addControlerListener(obsCntr);


//        Level level = Logger.getRootLogger().getLevel();
        Logger.getRootLogger().setLevel(Level.WARN);//Make output less verbose
        cntr.run();
//        Logger.getRootLogger().setLevel(level);//restore log level

        LeastCostPathCalculatorFactory fac = cntr.getLeastCostPathCalculatorFactory();

        TravelDisutility travelCost = cntr.getTravelDisutilityFactory().createTravelDisutility(cntr.getLinkTravelTimes());

        this.router = fac.createPathCalculator(sc.getNetwork(), travelCost, cntr.getLinkTravelTimes());
        obs.updateCellColors();

        this.gridColl = new FeatureCollection();
        boolean first = true;
        for (Grid.Cell cell : grid.getCells()) {
            if (cell.c == Grid.CellColor.white) {
                continue;
            }
            com.vividsolutions.jts.geom.Polygon p = cell.p;

            Feature ret = new Feature();
            List<LngLatAlt> l = new ArrayList<>();
            for (Coordinate coord : p.getExteriorRing().getCoordinates()) {
                l.add(this.session.getTransformer().toGeographic(coord.x, coord.y));
            }
            GeoJsonObject geo = new Polygon(l);
            ret.setGeometry(geo);
            ret.setProperty("color", cell.c.toString());
            ret.setProperty("time", cell.time);
            if (first) {
                List<Double> percentils = obs.getPercentiles();
                ret.setProperty("green", percentils.get(0));
                ret.setProperty("lime", percentils.get(1));
                ret.setProperty("yellow", percentils.get(2));
                ret.setProperty("orange", percentils.get(3));
                ret.setProperty("red", percentils.get(4));
                ret.setProperty("fuchsia", percentils.get(5));
                ret.setProperty("purple", percentils.get(6));
                first = false;
            }

            this.gridColl.add(ret);
        }

        isInitialized = true;

    }

    public synchronized FeatureCollection getRoute(LngLatAlt start) {
        init();
        Coord coord = this.session.getTransformer().toUTM(start);
        Grid.Cell cell = this.grid.getClosestCell(coord.getX(), coord.getY());
        Node from = this.osmNetwork.getClosestNode(coord);
        Person p = this.session.getScenario().getPopulation().getFactory().createPerson(Id.createPersonId(0));
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
//                ft.setProperty("time", r.travelTime);
                ft.setProperty("time", cell.time);
                route.add(ft);

            }
        } catch (Exception e) {
            log.warn(e);
        }

        return route;

    }


    public FeatureCollection getGrid() {
        init();
        return this.gridColl;
    }


}
