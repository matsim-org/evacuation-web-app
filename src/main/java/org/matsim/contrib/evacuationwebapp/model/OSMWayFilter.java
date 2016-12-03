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

import de.westnordost.osmapi.map.data.Way;
import org.matsim.api.core.v01.network.Link;

/**
 * Created by laemmel on 06/11/2016.
 */
public interface OSMWayFilter {

    boolean rejectWay(Way way);

    void configureLink(Way way, Link link);

    boolean isOneway(Way way);

}
