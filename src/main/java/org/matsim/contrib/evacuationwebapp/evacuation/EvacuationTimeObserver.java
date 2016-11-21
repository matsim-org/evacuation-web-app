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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by laemmel on 02/11/2016.
 */
public class EvacuationTimeObserver implements PersonDepartureEventHandler, PersonArrivalEventHandler {


    private final Map<Id<Person>, PersonDepartureEvent> dep = new HashMap<>();
    private final Map<Grid.Cell, TTInfo> tt = new HashMap<>();
    private final Grid grid;
    private final Scenario sc;
    private double maxTT;

//    private double maxTT = 0;

    public EvacuationTimeObserver(Grid grid, Scenario sc) {
        this.grid = grid;
        this.sc = sc;
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        PersonDepartureEvent dEvent = dep.remove(event.getPersonId());
        Link link = this.sc.getNetwork().getLinks().get(dEvent.getLinkId());
        Grid.Cell cell = grid.getClosestCell(link.getCoord().getX(), link.getCoord().getY());
        TTInfo t = tt.get(cell);
        if (t == null) {
            t = new TTInfo();
            tt.put(cell, t);
        }
        t.updateTT(event.getTime() - dEvent.getTime());

//        if (t.tt > maxTT) {
//            maxTT = t.tt;
//        }

    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        dep.put(event.getPersonId(), event);

    }

    @Override
    public void reset(int iteration) {
        dep.clear();
        tt.clear();
    }

    public void updateCellColors() {
        Set<Map.Entry<Grid.Cell, TTInfo>> es = tt.entrySet();
        double maxTT = 0;
        for (Map.Entry<Grid.Cell, TTInfo> entry : es) {
            if (entry.getValue().tt > maxTT) {
                maxTT = entry.getValue().tt;
            }

        }
        this.maxTT = maxTT;

        for (Map.Entry<Grid.Cell, TTInfo> entry : es) {
            double rel = entry.getValue().tt / maxTT;
            if (rel < 0.3) {
                entry.getKey().c = Grid.CellColor.green;
            } else if (rel < 0.4) {
                entry.getKey().c = Grid.CellColor.lime;
            } else if (rel < 0.5) {
                entry.getKey().c = Grid.CellColor.yellow;
            } else if (rel < 0.6) {
                entry.getKey().c = Grid.CellColor.orange;
            } else if (rel < 0.7) {
                entry.getKey().c = Grid.CellColor.red;
            } else if (rel < 0.8) {
                entry.getKey().c = Grid.CellColor.fuchsia;
            } else {
                entry.getKey().c = Grid.CellColor.purple;
            }
            entry.getKey().time = entry.getValue().tt;
        }
    }

    public double getMAXTT() {
        return maxTT;
    }

    private final class TTInfo {
        int cnt;
        double tt;

        void updateTT(double time) {
            tt = ((double) cnt) / (1 + cnt) * tt + (1. / (1 + cnt)) * time;
            cnt++;
        }
    }
}