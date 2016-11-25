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
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;
import org.apache.log4j.Logger;
import org.codetome.hexameter.core.api.Hexagon;
import org.codetome.hexameter.core.api.HexagonalGrid;
import org.codetome.hexameter.core.api.HexagonalGridBuilder;
import org.codetome.hexameter.core.api.Point;
import org.matsim.contrib.evacuationwebapp.utils.Geometries;
import org.matsim.core.utils.collections.QuadTree;
import rx.Observable;
import rx.functions.Action1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.codetome.hexameter.core.api.HexagonOrientation.FLAT_TOP;
import static org.codetome.hexameter.core.api.HexagonalGridLayout.RECTANGULAR;

/**
 * Created by laemmel on 02/11/2016.
 */
public class Grid {

    private final double radius;
    private final Logger log = Logger.getLogger(Grid.class);
    private final Envelope utmE;
    private final Polygon utmArea;
    private final List<Polygon> polygons = new ArrayList<>();
    private QuadTree<Cell> quad;
    public Grid(Envelope utmEnvelope, Polygon utmArea) {
        this.utmE = utmEnvelope;
        this.utmArea = utmArea;

        double mxL = Math.min(utmEnvelope.getHeight(), utmEnvelope.getWidth());
        if (mxL <= 10000) {
            radius = 150;
        } else if (mxL <= 20000) {
            radius = 300;
        } else if (mxL <= 50000) {
            radius = 1000;
        } else {
            radius = 2000;
        }
        init();
    }

    private void init() {

        log.info("initializing hex grid.");
        this.quad = new QuadTree<Cell>(utmE.getMinX() - utmE.getWidth(), utmE.getMinY() - utmE.getHeight(), utmE.getMaxX() + utmE.getWidth(), utmE.getMaxY() + utmE.getHeight());


        int gridHeight = (int) (utmE.getHeight() / (3. / 4 * radius * 2) + 0.5) + 2;
        int gridWidth = (int) (utmE.getWidth() / (3. / 4 * radius * 2) + 0.5) + 2;

        HexagonalGridBuilder builder = new HexagonalGridBuilder()
                .setGridHeight(gridHeight)
                .setGridWidth(gridWidth)
                .setGridLayout(RECTANGULAR)
                .setOrientation(FLAT_TOP)
                .setRadius(radius);
        HexagonalGrid grid = builder.build();


        Observable<Hexagon> hexagons = grid.getHexagons();
        hexagons.forEach(new Action1<Hexagon>() {
            @Override
            public void call(Hexagon hexagon) {


                Coordinate[] coords = new Coordinate[7];
                int idx = 0;

                for (Object point : hexagon.getPoints()) {
                    Point p = (Point) point;
                    double x = p.getCoordinateX() + utmE.getMinX() - radius / 2;
                    double y = p.getCoordinateY() + utmE.getMinY() - radius / 2;
                    coords[idx++] = new Coordinate(x, y);
                }
                coords[idx] = new Coordinate(coords[0]);
                Polygon p = Geometries.createPolygon(coords);
                if (Grid.this.utmArea.intersects(p)) {
                    Grid.this.polygons.add(p);
                    Cell c = new Cell();
                    c.p = p;
                    Grid.this.quad.put(p.getCentroid().getX(), p.getCentroid().getY(), c);
                }

            }
        });
        log.info("done.");

    }

    public Cell getClosestCell(double x, double y) {
        return this.quad.getClosest(x, y);
    }

    public Collection<Cell> getCells() {
        return this.quad.values();
    }

    public enum CellColor {
        green, lime, orange, red, fuchsia, purple, yellow, white
    }

    static final class Cell {
        Polygon p;
        CellColor c = CellColor.white;
        double time = 0;
    }
}
