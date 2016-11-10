/*
 * Copyright (c) 2016 Gregor Lämmel
 * This file is part of evacuation.
 * evacuation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * See also LICENSE and WARRANTY file
 */

package org.matsim.contrib.evacuationwebapp.manager;

/**
 * Created by laemmel on 04/11/2016.
 */
public class LngLat {

    private double lng;
    private double lat;

    public double getLng() {
        return lng;
    }

    public double getLat() {
        return lat;
    }

    public LngLat() {

    }

    public LngLat(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }
}
