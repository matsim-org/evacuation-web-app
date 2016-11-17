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

package org.matsim.contrib.evacuationwebapp.controller;


import org.apache.log4j.Logger;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.matsim.contrib.evacuationwebapp.evacuation.EvacuationManager;
import org.matsim.contrib.evacuationwebapp.utils.SessionIDGenerator;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * Created by laemmel on 01/11/2016.
 */

@Controller
public class EvacuationController {

    private long sessions = 0;

    private EvacuationManager em;


    private static final Logger log = Logger.getLogger(EvacuationController.class);

    @MessageMapping("/evac")
    @SendToUser("/topic/evacuation")
    public FeatureCollection evacuationArea(@RequestBody Feature[] message) throws Exception {


        this.em = new EvacuationManager(message[0]);


        return em.getFeatureCollection();
    }

    @MessageMapping("/route")
    @SendToUser("/topic/routing")
    public FeatureCollection evacuationRoute(@RequestBody Feature message) throws Exception {

        return em.getRoute(((Point) message.getGeometry()).getCoordinates());
    }

    @MessageMapping("/session")
    @SendToUser("/topic/sessionid")
    public String sessionId(String msg) throws Exception {
        sessions++;
        return SessionIDGenerator.getNextSessionID();
    }


}
