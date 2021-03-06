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
import com.vividsolutions.jts.geom.Envelope;
import de.westnordost.osmapi.map.data.LatLon;
import de.westnordost.osmapi.map.data.Node;
import de.westnordost.osmapi.map.data.Way;
import org.apache.log4j.Logger;
import org.geojson.LngLatAlt;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.*;


/**
 * Created by laemmel on 05/11/2016.
 */
public class OSMNetwork {


    private static final Logger log = Logger.getLogger(OSMNetwork.class);
    private final QuadTree<org.matsim.api.core.v01.network.Node[]> quad;
    private final Scenario sc;
    private final Map<Id<Link>, LinkInfo> tracerLinks = new HashMap<>();
    private long linkIds = 0;
    private Session session;

    @Inject
    private OSMWayFilter filter;

    private Map<Long, NodeInfo> nodes = new HashMap<>();
    private Queue<Way> ways = new LinkedList<>();

    @Inject
    public OSMNetwork(Session s) {
        this.session = s;
        Envelope utmE = s.getUtmE();
        this.quad = new QuadTree<>(utmE.getMinX() - 1000000, utmE.getMinY() - 1000000, utmE.getMaxX() + 1000000, utmE.getMaxY() + 1000000);
        this.sc = s.getScenario();
    }

    public void addNode(Node node) {
        NodeInfo ni = new NodeInfo();
        ni.node = node;
        nodes.put(node.getId(), ni);
    }

    public void addWay(Way way) {
        ways.add(way);
        Iterator<Long> it = way.getNodeIds().iterator();
        while (it.hasNext()) {
            Long next = it.next();
            nodes.get(next).links++;
        }
    }

    public void createMATSimNetwork() {
        while (ways.peek() != null) {
            handleWay(ways.poll());
        }
        this.nodes.clear();
        this.ways.clear();
    }

    private void handleWay(Way way) {
        if (way.getTags() == null || way.getTags().get("highway") == null) {
            return;
        }

        if (way.getNodeIds().size() <= 1) {
            log.warn("Way of one or less nodes detected!");
            return;
        }

        boolean oneway = false;
        String ow = way.getTags().get("oneway");
        if ((ow != null && ow.equals("yes")) || filter.isOneway(way)) {
            oneway = true;
        }

        Iterator<Long> it = way.getNodeIds().iterator();
        Long id = it.next();
        NodeInfo to = nodes.get(id);
        if (to.matNode == null) {
            createAndAddMATSimNode(to);
        }
        NodeInfo from = to;

        while (it.hasNext()) {

            Id<Link> linkId = Id.createLinkId(linkIds++);
            LinkInfo tracer = new LinkInfo();
            this.tracerLinks.put(linkId, tracer);

            tracer.coords.add(new LngLatAlt(from.node.getPosition().getLongitude(), from.node.getPosition().getLatitude()));
            double length = 0;

            boolean notFinished = true;
            while (it.hasNext() && notFinished) {
                Long nxtId = it.next();
                NodeInfo next = nodes.get(nxtId);
                tracer.coords.add(new LngLatAlt(next.node.getPosition().getLongitude(), next.node.getPosition().getLatitude()));
                length += dist(to, next);


                to = next;

                if (to.links > 1) { //intersection
                    notFinished = false;
                }

            }

            if (to.matNode == null) {
                createAndAddMATSimNode(to);
            }

            createAndAddLink(from, to, length, way, linkId);


            if (!oneway) {
                Id<Link> revLinkId = Id.createLinkId(linkIds++);
                createAndAddLink(to, from, length, way, revLinkId);
                LinkInfo revTracer = new LinkInfo();
                revTracer.coords.addAll(tracer.coords);
                Collections.reverse(revTracer.coords);
                this.tracerLinks.put(revLinkId, revTracer);
            }

            from = to;

        }

    }

    private double dist(NodeInfo to, NodeInfo next) {
        Coord c1 = this.session.getTransformer().toUTM(to.node.getPosition());
        Coord c2 = this.session.getTransformer().toUTM(next.node.getPosition());
        return CoordUtils.calcProjectedEuclideanDistance(c1, c2);
    }

    private void createAndAddLink(NodeInfo from, NodeInfo to, double length, Way way, Id<Link> linkId) {
        Link l = this.sc.getNetwork().getFactory().createLink(linkId, from.matNode, to.matNode);
        l.setLength(length);
        this.filter.configureLink(way, l);
        this.sc.getNetwork().addLink(l);


    }

    private void createAndAddMATSimNode(NodeInfo ni) {
        LatLon pos = ni.node.getPosition();
        Coord c = this.session.getTransformer().toUTM(pos);
        org.matsim.api.core.v01.network.Node n = this.sc.getNetwork().getFactory().createNode(Id.createNodeId(ni.node.getId()), c);
        ni.matNode = n;
        this.sc.getNetwork().addNode(n);
    }

    public org.matsim.api.core.v01.network.Node getClosestNode(Coord start) {
        org.matsim.api.core.v01.network.Node[] nodes = this.quad.getClosest(start.getX(), start.getY());
        double minDist = Double.POSITIVE_INFINITY;
        org.matsim.api.core.v01.network.Node closest = null;
        for (org.matsim.api.core.v01.network.Node n : nodes) {
            double dist = CoordUtils.calcProjectedEuclideanDistance(n.getCoord(), start);
            if (dist < minDist) {
                minDist = dist;
                closest = n;
            }
        }

        return closest;
    }

    public void consolidate() {
        log.info("Consolidating tracer network");

        List<Id<Link>> rm = new ArrayList<>();
        for (Map.Entry<Id<Link>, LinkInfo> entry : this.tracerLinks.entrySet()) {
            Link link = this.sc.getNetwork().getLinks().get((entry.getKey()));
            if (link != null) {
                for (LngLatAlt c : entry.getValue().coords) {
                    Coord utmC = this.session.getTransformer().toUTM(c);
                    this.quad.put(utmC.getX(), utmC.getY(), new org.matsim.api.core.v01.network.Node[]{link.getFromNode(), link.getToNode()});
                }
            } else {
                rm.add(entry.getKey());
            }
        }

        log.info("Removing " + rm.size() + " links from tracer network");
        for (Id<Link> r : rm) {
            this.tracerLinks.remove(r);
        }


    }

    public List<LngLatAlt> traceLink(Id<Link> id) {
        return this.tracerLinks.get(id).coords;
    }

    private static class NodeInfo {
        Node node;
        org.matsim.api.core.v01.network.Node matNode = null;
        int links;
    }

    private static final class LinkInfo {
        List<LngLatAlt> coords = new ArrayList<>();
    }


}
