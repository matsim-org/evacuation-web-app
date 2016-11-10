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

package org.matsim.contrib.evacuationwebapp.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;

/**
 * Created by laemmel on 02/11/2016.
 */
public abstract class DummyDemand {

    public static void createDummyDemand(Scenario sc, Id<Link> sl) {
        Population pop = sc.getPopulation();
        PopulationFactory fac = pop.getFactory();
        int id = 0;
        for (Link l : sc.getNetwork().getLinks().values()) {
            if (l.getId().toString().contains("el")) {
                continue;
            }

            Person p = fac.createPerson(Id.createPersonId(id++));
            pop.addPerson(p);
            Plan plan = fac.createPlan();
            Activity act = fac.createActivityFromLinkId("pre-evac", l.getId());
            act.setCoord(l.getCoord());
            act.setEndTime(0);
            plan.addActivity(act);
            Leg leg = fac.createLeg("car");
            plan.addLeg(leg);
            Activity act2 = fac.createActivityFromLinkId("post-evac", sl);
            act2.setEndTime(0);
            act2.setCoord(sc.getNetwork().getLinks().get(sl).getCoord());
            plan.addActivity(act2);
            plan.setScore(0.);
            p.addPlan(plan);
        }

    }

}
