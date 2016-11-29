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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This class takes a Network and a Geometry defining the evacuation area to create the corresponding evacuation network.
 *
 * @author laemmel
 */
public class EvacuationNetworkGenerator {

    private static final Logger log = Logger.getLogger(EvacuationNetworkGenerator.class);

    private final Geometry evacuationArea;
    private final Network network;

    private final HashSet<Link> redundantLinks = new HashSet<Link>();
    private final HashSet<Node> safeNodes = new HashSet<Node>();
    private final HashSet<Node> redundantNodes = new HashSet<Node>();

    private final Id<Node> safeNodeAId;

    private final Id<Node> safeNodeBId;

    private final Id<Link> safeLinkId;

    public EvacuationNetworkGenerator(Scenario sc, Geometry evavcuationArea, Id<Link> safeLinkId) {
        this.evacuationArea = evavcuationArea;//.buffer(4000);
        this.network = sc.getNetwork();
        this.safeNodeAId = Id.create("en1", Node.class);
        this.safeNodeBId = Id.create("en2", Node.class);
        this.safeLinkId = safeLinkId;
    }

    public void run() {
        log.info("generating evacuation net ...");
        log.info("pre-cleaning network");
        preClean();
        log.info("classifing nodes");
        classifyNodesAndLinks();
        log.info("creating evacuation nodes and links");
        createEvacuationNodsAndLinks();
        log.info("removing links and nodes that are outside the evacuation area");
        cleanUpNetwork();
        log.info("done.");
    }

    private void preClean() {
        log.info("Pre-cleanup #nodes: " + this.network.getNodes().size());
        List<Node> rm = new ArrayList<>();
        for (Node n : this.network.getNodes().values()) {
            if (n.getInLinks().size() == 0 && n.getOutLinks().size() == 0) {
                rm.add(n);
            }
        }
        for (Node n : rm) {
            this.network.removeNode(n.getId());
        }
        log.info("After basic cleanup #nodes: " + this.network.getNodes().size());
    }

    /**
     * Creates links from all save nodes to the evacuation node A
     */
    private void createEvacuationNodsAndLinks() {

        Coordinate cc1 = this.evacuationArea.getCentroid().getCoordinate();
        cc1.x += 10000;
        cc1.y += 10000;
        Coord safeCoord1 = MGC.coordinate2Coord(cc1);

        Coordinate cc2 = this.evacuationArea.getCentroid().getCoordinate();
        cc2.x += 10010;
        cc2.y += 10010;
        Coord safeCoord2 = MGC.coordinate2Coord(cc2);

        Node safeNodeA = this.network.getFactory().createNode(this.safeNodeAId, safeCoord1);
        this.network.addNode(safeNodeA);
        Node safeNodeB = this.network.getFactory().createNode(this.safeNodeBId, safeCoord2);
        this.network.addNode(safeNodeB);

        double capacity = 1000000.;
        Link l = this.network.getFactory().createLink(this.safeLinkId, safeNodeA, safeNodeB);
        l.setLength(10);
        l.setFreespeed(100000);
        l.setCapacity(capacity);
        l.setNumberOfLanes(100);
        this.network.addLink(l);

        int linkId = 1;
        for (Node node : this.network.getNodes().values()) {
            if (this.safeNodes.contains(node)) {
                linkId++;
                String sLinkID = "el" + Integer.toString(linkId);
                Link l2 = this.network.getFactory().createLink(Id.create(sLinkID, Link.class), node, safeNodeA);
                l2.setLength(10);
                l2.setFreespeed(100000);
                l2.setCapacity(capacity);
                l2.setNumberOfLanes(1);
                this.network.addLink(l2);
            }
        }
    }

    private void classifyNodesAndLinks() {

        for (Node node : this.network.getNodes().values()) {
            Point p = MGC.coord2Point(node.getCoord());
            if (!this.evacuationArea.contains(p)) {
                boolean isSafe = false;
                for (Link l : node.getInLinks().values()) {
                    Node from = l.getFromNode();
                    Point p2 = MGC.coord2Point(from.getCoord());
                    if (this.evacuationArea.contains(p2)) {
                        isSafe = true;
                        break;
                    }
                }
                if (isSafe) {
                    this.safeNodes.add(node);
                } else {
                    this.redundantNodes.add(node);
                }
            }
        }
        for (Node node : this.redundantNodes) {
            for (Link l : node.getInLinks().values()) {
                this.redundantLinks.add(l);
            }
            for (Link l : node.getOutLinks().values()) {
                this.redundantLinks.add(l);
            }
        }
    }

    /**
     * Removes all links and nodes outside the evacuation area except the nodes
     * next to the evacuation area that are reachable from inside the evacuation
     * area ("save nodes").
     */
    protected void cleanUpNetwork() {

        for (Link l : this.redundantLinks) {
            this.network.removeLink(l.getId());
        }
        log.info(this.redundantLinks.size() + " links outside the evacuation area have been removed.");

        for (Node n : this.redundantNodes) {
            this.network.removeNode(n.getId());
        }
        log.info(this.redundantNodes.size() + " nodes outside the evacuation area have been removed.");


        //since NetworkCleaner would remove all one-way streets that lead out of the evacuation area we have to add a return path
        log.info("adding dummy links");
        List<Link> dummies = new ArrayList<Link>();
        int dCnt = 0;
        for (Node n : this.network.getNodes().values()) {
            if (!this.safeNodes.contains(n) && !this.redundantNodes.contains(n)) {
                NetworkFactory fac = this.network.getFactory();
                Link l = fac.createLink(Id.create("dummy" + dCnt++, Link.class), this.network.getNodes().get(this.safeNodeBId), n);
                this.network.addLink(l);
                dummies.add(l);
            }
        }
        new NetworkCleaner().run(this.network);
        log.info("removing dummy links");
        for (Link dummy : dummies) {
            this.network.removeLink(dummy.getId());
        }
        this.redundantLinks.clear();
        this.redundantNodes.clear();

    }


}
