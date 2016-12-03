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
import de.westnordost.osmapi.OsmConnection;
import de.westnordost.osmapi.map.MapDataDao;
import de.westnordost.osmapi.map.handler.MapDataHandler;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;

/**
 * Created by laemmel on 01/11/2016.
 */
public class OSMParser {


    @Inject
    OSMAPIURLProvider aPIURL;

    @Inject
    Session session;


    @Inject
    OSMWayFilter filter;


    public void run(OSMNetwork osmNetwork) {
        OAuthConsumer auth = new DefaultOAuthConsumer("null", "null");
        OsmConnection osm = new OsmConnection(aPIURL.getOSMAPIURL(),
                "Agent Smith", auth);

        MapDataHandler myMapDataHandler = new OSMMapDataHandler(osmNetwork, filter);

        MapDataDao mapDao = new MapDataDao(osm);


        mapDao.getMap(session.getBoundingBox(), myMapDataHandler);
    }
}
