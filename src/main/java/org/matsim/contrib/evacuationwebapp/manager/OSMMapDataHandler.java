/*
 * Copyright (c) 2016 Gregor Lämmel
 * This file is part of evacuation.
 * evacuation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * See also LICENSE and WARRANTY file
 */

package org.matsim.contrib.evacuationwebapp.manager;


import com.google.inject.Inject;
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


    private Counter nodesCounter = new Counter("Nodes downloaded: ");
    private Counter waysCounter = new Counter("Ways downloaded: ");

    private long linkId = 0;

    private static final Logger log = Logger.getLogger(OSMMapDataHandler.class);

    @Inject
    private OSMNetwork network;

    @Inject
    OSMWayFilter filter;

    @Override
    public void handle(BoundingBox boundingBox) {
//        log.info(boundingBox);
    }

    @Override
    public void handle(Node node) {
        nodesCounter.incCounter();
//        LatLon pos = node.getPosition();
//        Coord c = this.transformer.toUTM(pos);
////        this.utmEnvelope.expandToInclude(c.getX(), c.getY());
//        org.matsim.api.core.v01.network.Node n = this.network.getFactory().createNode(Id.createNodeId(node.getId()), c);
//        this.network.addNode(n);
        network.addNode(node);

    }

    @Override
    public void handle(Way way) {
        waysCounter.incCounter();
        if (filter.rejectWay(way)) {
            return;
        }

        network.addWay(way);


//        Iterator<Long> nIds = way.getNodeIds().iterator();
//        boolean oneway = false;//"yes".equals(way.getTags().get("oneway"));
//
//        long nId = nIds.next();
//        Id<org.matsim.api.core.v01.network.Node> currId = Id.createNodeId(nId);
//
//        org.matsim.api.core.v01.network.Node curr = this.network.getNodes().get(currId);
//        if (curr == null) {
//            log.warn("Way refer to non existing node!");
//            return;
//        }
//        while (nIds.hasNext()) {
//            nId = nIds.next();
//            Id<org.matsim.api.core.v01.network.Node> nextId = Id.createNodeId(nId);
//            org.matsim.api.core.v01.network.Node next = this.network.getNodes().get(nextId);
//            if (next == null) {
//                log.warn("Way refer to non existing node!");
//                return;
//            }
//            createLink(curr, next, way);
//            if (!oneway) {
//                createLink(next, curr, way);
//            }
//            curr = next;
//
//        }


//        log.info(way);
    }

//    private void createLink(org.matsim.api.core.v01.network.Node curr, org.matsim.api.core.v01.network.Node next, Way way) {
//        Link l1 = this.network.getFactory().createLink(Id.createLinkId(this.linkId++), curr, next);
//        l1.setLength(CoordUtils.calcEuclideanDistance(curr.getCoord(), next.getCoord()));
//        l1.setCapacity(3600);
//        this.network.addLink(l1);
//    }


    @Override
    public void handle(Relation relation) {
//        log.info(relation);
    }
}
