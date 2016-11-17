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


import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.matsim.contrib.evacuationwebapp.manager.EvacuationManager;
import org.matsim.contrib.evacuationwebapp.manager.LngLat;
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

        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Feature.class).toInstance(message[0]);

            }
        });

        EvacuationManager em = injector.getInstance(EvacuationManager.class);

        this.em = em;

        em.run();

        return em.getFeatureCollection();
    }

    @MessageMapping("/route")
    @SendToUser("/topic/routing")
    public FeatureCollection evacuationRoute(LngLat message) throws Exception {

//        this.em.calcRoute(message);

        return em.getRoute(message);
    }

    @MessageMapping("/session")
    @SendToUser("/topic/sessionid")
    public Long sessionId(String msg) throws Exception {
        return sessions++;
    }


}
