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
import org.matsim.contrib.evacuationwebapp.evacuation.Session;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.SessionsManager;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.SessionAlreadyExistsException;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.UnknownSessionException;
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


    private final SessionsManager sm = new SessionsManager(() -> "http://overpass-api.de/api/");

    private long sessions = 0;

    private static final Logger log = Logger.getLogger(EvacuationController.class);

    @MessageMapping("/evac")
    @SendToUser("/topic/evacuation")
    public FeatureCollection evacuationArea(@RequestBody Feature[] message) throws Exception {

        String id = message[0].getProperty("sessionid");
        Session s = new Session(id, message[0]);
        try {
            this.sm.initializeNewSession(s);
            return this.sm.getEvacuationAnalysisGrid(id);
        } catch (SessionAlreadyExistsException e) {
            log.warn(e);
        }
        return new FeatureCollection();
    }

    @MessageMapping("/route")
    @SendToUser("/topic/routing")
    public FeatureCollection evacuationRoute(@RequestBody Feature message) throws Exception {
        FeatureCollection ret = new FeatureCollection();

        String id = message.getProperty("sessionid");
        try {
            ret = this.sm.getEvacuationRoute(id, ((Point) message.getGeometry()).getCoordinates());
        } catch (UnknownSessionException e) {
            log.warn(e);
        }
        return ret;
    }

    @MessageMapping("/session")
    @SendToUser("/topic/sessionid")
    public String sessionId(String msg) throws Exception {
        sessions++;
        String id = SessionIDGenerator.getNextSessionID();
        return id;
    }


}
