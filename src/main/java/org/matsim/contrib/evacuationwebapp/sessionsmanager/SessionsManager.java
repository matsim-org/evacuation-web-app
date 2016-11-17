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
import org.geojson.LngLatAlt;
import org.matsim.contrib.evacuationwebapp.evacuation.EvacuationManager;
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

    public void initializeNewSession(String sessionId, Feature area) throws SessionAlreadyExistsException {
        Worker w = workers.get(sessionId);
        if (w != null) {
            throw new SessionAlreadyExistsException("A session with ID: " + sessionId + " already exists.");
        }
        w = new Worker(area, sessionId);
        Thread t = new Thread(w);
        t.start();
        threads.add(t);
        workers.put(sessionId, w);
    }

    public FeatureCollection getEvacuationAnalysisGrid(String sessionId) {
        Worker w = workers.get(sessionId);
        if (w == null) {
            throw new UnknownSessionException("A session with ID: " + sessionId + " does not exist.");
        }

        Request r = new Request(RequestType.Grid);
        w.addRequest(r);
        return r.getResponse();
    }

    public FeatureCollection getEvacuationRoute(String sessionId, LngLatAlt start) throws UnknownSessionException {
        Worker w = workers.get(sessionId);
        if (w == null) {
            throw new UnknownSessionException("A session with ID: " + sessionId + " does not exist.");
        }

        Request r = new Request(RequestType.Route, start);
        w.addRequest(r);
        return r.getResponse();
    }

    public void disconnect(String sessionId) {
        Worker w = workers.remove(sessionId);
        if (w == null) {
            throw new UnknownSessionException("A session with ID: " + sessionId + " does not exist.");
        }
        Request r = new Request(RequestType.Shutdown);
        w.addRequest(r);
        r.getResponse();
    }

    private static final class Worker implements Runnable {

        private final BlockingQueue<Request> requesQueue = new LinkedBlockingQueue<>();

        private final EvacuationManager em;

        private boolean isRunning;

        public Worker(final Feature area, final String session) {
            this.em = new EvacuationManager(area, session);
        }

        @Override
        public void run() {
            isRunning = true;
            while (isRunning(true)) {
                try {
                    Request r = requesQueue.take();
                    switch (r.getRequestType()) {
                        case Grid:
                            r.setResponse(this.em.getGrid());
                            break;
                        case Route:
                            r.setResponse(this.em.getRoute(r.getCoord()));
                            break;
                        case Shutdown:
                            while (requesQueue.peek() != null) {
                                Request rr = requesQueue.poll();
                                rr.setResponse(null);
                            }
                            r.setResponse(null);
                            isRunning(false);
                            break;
                        default:
                            throw new RuntimeException("Unknown request type: " + r.getRequestType());

                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }


        }

        private synchronized boolean isRunning(boolean keepRunning) {
            if (!keepRunning) {
                isRunning = false;
            }
            return isRunning;
        }

        void addRequest(Request r) {
            if (isRunning(true)) {
                requesQueue.add(r);
            } else {
                r.setResponse(null);
            }
        }
    }

    private static final class Request {
        private final CountDownLatch latch = new CountDownLatch(1);

        private final RequestType rt;
        private final LngLatAlt coord;

        private FeatureCollection resp;


        Request(RequestType rt) {
            this(rt, null);
        }

        Request(RequestType rt, LngLatAlt coord) {
            this.rt = rt;
            this.coord = coord;
        }

        RequestType getRequestType() {
            return rt;
        }

        LngLatAlt getCoord() {
            return this.coord;
        }

        void setResponse(FeatureCollection resp) {
            this.resp = resp;
            this.latch.countDown();
        }

        FeatureCollection getResponse() {
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
