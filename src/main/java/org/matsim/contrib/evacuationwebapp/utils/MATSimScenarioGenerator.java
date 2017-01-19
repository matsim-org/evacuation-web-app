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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.evacuationwebapp.model.Session;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * Created by laemmel on 02/11/2016.
 */
public class MATSimScenarioGenerator {

    public static void createScenario(double sample, Session session) {
        Scenario sc = session.getScenario();
        Config c = sc.getConfig();

        c.global().setRandomSeed(4711L);
        c.global().setNumberOfThreads(6);

        c.controler().setCreateGraphs(false);
        c.controler().setLastIteration(0);
        c.controler().setOutputDirectory("/tmp/output/" + session.getId());
        c.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        c.qsim().setFlowCapFactor(sample);
        c.qsim().setStorageCapFactor(sample);
        c.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        c.qsim().setEndTime(24 * 3600);
        c.qsim().setNumberOfThreads(6);

        c.strategy().setMaxAgentPlanMemorySize(3);
        c.strategy().addParam("ModuleDisableAfterIteration_1", "30");
        c.strategy().addParam("Module_1", "ReRoute");
        c.strategy().addParam("ModuleProbability_1", "0.1");
        c.strategy().addParam("Module_2", "ChangeExpBeta");
        c.strategy().addParam("ModuleProbability_2", "0.9");

        c.travelTimeCalculator().setTravelTimeCalculatorType("TravelTimeCalculatorHashMap");
//        c.travelTimeCalculator().setTravelTimeAggregatorType("experimental_LastMile");
        c.travelTimeCalculator().setTraveltimeBinSize(300);

        c.controler().setDumpDataAtEnd(false);
        c.controler().setWriteEventsInterval(0);
        c.controler().setWritePlansInterval(0);



        PlanCalcScoreConfigGroup.ActivityParams pre = new PlanCalcScoreConfigGroup.ActivityParams("pre-evac");
        pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
        // running a simulation one gets
        // "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
        // the reason is the double precision. see also comment in
        // ActivityUtilityParameters.java (gl)
        pre.setMinimalDuration(49);
        pre.setClosingTime(49);
        pre.setEarliestEndTime(49);
        pre.setLatestStartTime(49);
        pre.setOpeningTime(49);

        PlanCalcScoreConfigGroup.ActivityParams post = new PlanCalcScoreConfigGroup.ActivityParams("post-evac");
        post.setTypicalDuration(49); // dito
        post.setMinimalDuration(49);
        post.setClosingTime(49);
        post.setEarliestEndTime(49);
        post.setLatestStartTime(49);
        post.setOpeningTime(49);
        c.planCalcScore().addActivityParams(pre);
        c.planCalcScore().addActivityParams(post);

        c.planCalcScore().setLateArrival_utils_hr(0.);
        c.planCalcScore().setPerforming_utils_hr(0.);


    }
}
