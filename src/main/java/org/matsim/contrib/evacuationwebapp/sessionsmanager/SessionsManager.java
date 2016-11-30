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


import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.matsim.contrib.evacuationwebapp.evacuation.*;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.SessionAlreadyExistsException;
import org.matsim.contrib.evacuationwebapp.sessionsmanager.exceptions.UnknownSessionException;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by laemmel on 17/11/2016.
 */
public class SessionsManager {

    private final OSMAPIURLProvider osmURL;

    private final Map<String, Worker> workers = new ConcurrentHashMap<>();
    private final long keepAlive;

    private final BlockingQueue<Worker> removalQueue = new LinkedBlockingQueue<>();

    public SessionsManager(OSMAPIURLProvider osmURL, long keepAlive) {
        this.osmURL = osmURL;
        this.keepAlive = keepAlive;
        new Thread(() -> {
            while (true) {
                try {
                    Worker worker = SessionsManager.this.removalQueue.take();
                    SessionsManager.this.shutdown(worker);
                    this.workers.remove(worker.getSessionId());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Logger.getRootLogger().setLevel(Level.WARN);//Make output less verbose
    }

    public void initializeNewSession(Session s) throws SessionAlreadyExistsException {
        Worker w = workers.get(s.getId());
        if (w != null) {
            throw new SessionAlreadyExistsException("A session with ID: " + s.getId() + " already exists.");
        }
        w = new Worker(s);
        Thread t = new Thread(w);
        t.start();
        workers.put(s.getId(), w);
    }

    public FeatureCollection getEvacuationAnalysisGrid(String sessionId) {
        Worker w = workers.get(sessionId);
        if (w == null) {
            throw new UnknownSessionException("A session with ID: " + sessionId + " does not exist.");
        }

        Request r = new Request(RequestType.Grid);
        boolean succ = w.addRequest(r);
        if (!succ) {
            shutdown(w);
        }
        return r.getResponse();
    }

    public FeatureCollection getEvacuationRoute(String sessionId, LngLatAlt start) throws UnknownSessionException {
        Worker w = workers.get(sessionId);
        if (w == null) {
            throw new UnknownSessionException("A session with ID: " + sessionId + " does not exist.");
        }

        Request r = new Request(RequestType.Route, start);
        boolean succ = w.addRequest(r);
        if (!succ) {
            shutdown(w);
        }
        return r.getResponse();
    }

    public void disconnect(String sessionId) {
        //TODO remove and join thread [GL Nov '16]
        Worker w = workers.remove(sessionId);
        if (w == null) {
            throw new UnknownSessionException("A session with ID: " + sessionId + " does not exist.");
        }
        shutdown(w);
    }

    public void shutdown() {
        Iterator<Map.Entry<String, Worker>> it = this.workers.entrySet().iterator();
        while (it.hasNext()) {
            Request r = new Request(RequestType.Shutdown);
            Worker w = it.next().getValue();
            shutdown(w);
            it.remove();


        }
    }

    public void shutdown(Worker w) {
        Request r = new Request(RequestType.Shutdown);
        w.addRequest(r);
        r.getResponse();
        try {
            w.join();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }


    private enum RequestType {
        Grid, Route, Shutdown
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

        FeatureCollection getResponse() {
            try {
                this.latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return this.resp;
        }

        void setResponse(FeatureCollection resp) {
            this.resp = resp;
            this.latch.countDown();
        }
    }

    private final class Worker implements Runnable {

        private final BlockingQueue<Request> requesQueue = new LinkedBlockingQueue<>();
        private final Session session;
        private EvacuationManager em;
        private boolean isRunning = true;
        private Thread t;
        private String sessionId;

        public Worker(Session session) {
            this.session = session;
        }

        @Override
        public void run() {

            this.t = Thread.currentThread();

            session.prepare();

            Injector injector = Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Session.class).toInstance(session);
                    bind(OSMWayFilter.class).to(OSMVehicularFilter.class);
                    bind(OSMAPIURLProvider.class).toInstance(SessionsManager.this.osmURL);
                }
            });

            this.em = injector.getInstance(EvacuationManager.class);

            isRunning = true;
            while (isRunning(true)) {
                try {
                    Request r = requesQueue.poll(SessionsManager.this.keepAlive, TimeUnit.SECONDS);

                    if (r == null) {
                        cleanUp();
                        SessionsManager.this.removalQueue.add(this);
                        continue;
                    }

                    switch (r.getRequestType()) {
                        case Grid:
                            FeatureCollection grid = this.em.getGrid();
                            r.setResponse(grid);
                            break;
                        case Route:
                            r.setResponse(this.em.getRoute(r.getCoord()));
                            break;
                        case Shutdown:
                            cleanUp();
                            r.setResponse(null);
                            break;
                        default:
                            throw new RuntimeException("Unknown request type: " + r.getRequestType());

                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }

        private void cleanUp() {
            isRunning(false);
            while (requesQueue.peek() != null) {
                Request rr = requesQueue.poll();
                rr.setResponse(null);
            }

        }

        private synchronized boolean isRunning(boolean keepRunning) {
            if (!keepRunning) {
                isRunning = false;
            }
            return isRunning;
        }

        boolean addRequest(Request r) {
            if (isRunning(true)) {
                requesQueue.add(r);
                return true;
            } else {
                r.setResponse(null);
                return false;
            }
        }

        public void join() throws InterruptedException {
            this.t.join();
        }

        public String getSessionId() {
            return sessionId;
        }
    }


}
