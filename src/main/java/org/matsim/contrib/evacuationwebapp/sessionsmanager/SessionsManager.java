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

package org.matsim.contrib.evacuationwebapp.sessionsmanager;


import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.SessionAlreadyExistsException;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.UnknownSessionException;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by laemmel on 17/11/2016.
 */
public class SessionsManager {


    private final Map<String, Worker> workers = new ConcurrentHashMap<>();
    private final Queue<Thread> threads = new LinkedList<>();

    public void initializeNewSession(String sessionId, Feature area) {
        Worker w = workers.get(sessionId);
        if (w != null) {
            throw new SessionAlreadyExistsException("A session with ID: " + sessionId + " already exists.");
        }
        w = new Worker(area);
        Thread t = new Thread(w);
        t.start();
        threads.add(t);
        workers.put(sessionId, w);
    }

    public FeatureCollection getEvacuationAnalysisGrid(String sessionId) {
        Worker w = workers.get(sessionId);
        if (w != null) {
            throw new UnknownSessionException("A session with ID: " + sessionId + " does not exist.");
        }


        return null;
    }

    private static final class Worker implements Runnable {

        private final BlockingQueue<Request> requesQueue = new LinkedBlockingQueue<>();

        private final Feature area;

        private boolean isRunning;

        public Worker(final Feature area) {
            this.area = area;
        }

        @Override
        public void run() {
            isRunning = true;
            while (isRunning) {
                try {
                    Request r = requesQueue.take();
                    switch (r.getRequestType()) {
                        case Grid:
                            break;
                        case Route:
                            break;
                        case Shutdown:
                            break;
                        default:
                            throw new RuntimeException("Unknown request type: " + r.getRequestType());

                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }


        }
    }

    private static final class Request {
        private final CountDownLatch latch = new CountDownLatch(1);

        private final RequestType rt;
        private final Feature ft;

        private FeatureCollection resp;


        public Request(RequestType rt) {
            this(rt, null);
        }

        public Request(RequestType rt, Feature ft) {
            this.rt = rt;
            this.ft = ft;
        }

        public RequestType getRequestType() {
            return rt;
        }

        public Feature getFt() {
            return this.ft;
        }

        public void setResponse(FeatureCollection resp) {
            this.resp = resp;
            this.latch.countDown();
        }

        public FeatureCollection getResponse() {
            try {
                this.latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return this.resp;
        }


    }

    private enum RequestType {
        Grid, Route, Shutdown
    }
}
