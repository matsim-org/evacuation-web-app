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

package org.matsim.contrib.evacuationwebapp.model;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * Created by laemmel on 30/11/2016.
 */
public class ObserverController implements IterationStartsListener {

    private final EvacuationTimeObserver observer;

    public ObserverController(EvacuationTimeObserver observer) {
        this.observer = observer;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {

        if (iterationStartsEvent.getIteration() == iterationStartsEvent.getServices().getConfig().controler().getLastIteration()) {
            iterationStartsEvent.getServices().getEvents().addHandler(observer);
        }
    }
}
