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

import de.westnordost.osmapi.map.data.Way;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by laemmel on 06/11/2016.
 */
public class OSMVehicularFilter implements OSMWayFilter {

    private static final Logger log = Logger.getLogger(OSMVehicularFilter.class);

    private final Map<String, Highwaysetting> highwaysettingMap = new HashMap<>();


    public OSMVehicularFilter() {
        highwaysettingMap.put("motorway", new Highwaysetting(2, 120 / 3.6, 2000, true));
        highwaysettingMap.put("motorway_link", new Highwaysetting(1, 80 / 3.6, 1500, true));
        highwaysettingMap.put("trunk", new Highwaysetting(1, 80 / 3.6, 2000, false));
        highwaysettingMap.put("trunk_link", new Highwaysetting(1, 50 / 3.6, 1500, false));
        highwaysettingMap.put("primary", new Highwaysetting(1, 80 / 3.6, 1500, false));
        highwaysettingMap.put("primary_link", new Highwaysetting(1, 60 / 3.6, 1500, false));
        highwaysettingMap.put("secondary", new Highwaysetting(1, 30 / 3.6, 1000, false));
        highwaysettingMap.put("secondary_link", new Highwaysetting(1, 30 / 3.6, 1000, false));
        highwaysettingMap.put("tertiary", new Highwaysetting(1, 25 / 3.6, 600, false));
        highwaysettingMap.put("tertiary_link", new Highwaysetting(1, 25 / 3.6, 600, false));
        highwaysettingMap.put("minor", new Highwaysetting(1, 20 / 3.6, 600, false));
        highwaysettingMap.put("residential", new Highwaysetting(1, 15 / 3.6, 600, false));
        highwaysettingMap.put("living_street", new Highwaysetting(1, 15 / 3.6, 600, false));
        highwaysettingMap.put("unclassified", new Highwaysetting(1, 45 / 3.6, 600, false));


    }

    @Override
    public boolean rejectWay(Way way) {

        if (way.getTags() == null || way.getTags().get("highway") == null) {
            return true;
        }

        if (way.getNodeIds().size() <= 1) {
            return true;
        }

        return !highwaysettingMap.containsKey(way.getTags().get("highway"));
    }

    @Override
    public void configureLink(Way way, Link link) {
        Highwaysetting setting = highwaysettingMap.get(way.getTags().get("highway"));
        link.setNumberOfLanes(setting.lanes);

        link.setFreespeed(setting.freespeed);
        try {
            if (way.getTags().get("lanes") != null) {
                double lanes = Integer.parseInt(way.getTags().get("lanes"));
                link.setNumberOfLanes(lanes);
            }
        } catch (NumberFormatException e) {
            log.warn(e);
        }
        link.setCapacity(setting.flowCap_vehPerHour * link.getNumberOfLanes());
    }

    @Override
    public boolean isOneway(Way way) {
        return this.highwaysettingMap.get(way.getTags().get("highway")).oneWay;
    }

    private static final class Highwaysetting {

        private final int lanes;
        private final double freespeed;
        private final double flowCap_vehPerHour;
        private final boolean oneWay;

        public Highwaysetting(int lanes, double freespeed, double flowCap_vehPerHour, boolean oneWay) {
            this.lanes = lanes;
            this.freespeed = freespeed;
            this.flowCap_vehPerHour = flowCap_vehPerHour;
            this.oneWay = oneWay;
        }

    }
}
