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


import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Relation;
import de.westnordost.osmapi.map.data.Way;
import de.westnordost.osmapi.map.handler.MapDataHandler;
import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.Counter;


/**
 * Created by laemmel on 01/11/2016.
 */
public class OSMMapDataHandler implements MapDataHandler {


    private static final Logger log = Logger.getLogger(OSMMapDataHandler.class);
    private final OSMNetwork network;
    private final OSMWayFilter filter;
    private Counter nodesCounter = new Counter("Nodes downloaded: ");
    private Counter waysCounter = new Counter("Ways downloaded: ");
    private long linkId = 0;


    public OSMMapDataHandler(OSMNetwork network, OSMWayFilter filter) {
        this.network = network;
        this.filter = filter;
    }


    @Override
    public void handle(BoundingBox boundingBox) {
//        log.info(boundingBox);
    }

    @Override
    public void handle(Node node) {
        nodesCounter.incCounter();
        network.addNode(node);

    }

    @Override
    public void handle(Way way) {
        waysCounter.incCounter();
        if (filter.rejectWay(way)) {
            return;
        }

        network.addWay(way);


    }

    @Override
    public void handle(Relation relation) {
//        log.info(relation);
    }
}
