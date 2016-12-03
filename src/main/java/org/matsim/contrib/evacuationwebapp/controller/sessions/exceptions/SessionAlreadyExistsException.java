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

package org.matsim.contrib.evacuationwebapp.controller.sessions.exceptions;


/**
 * Created by laemmel on 17/11/2016.
 */
public class SessionAlreadyExistsException extends RuntimeException {

    public SessionAlreadyExistsException() {
        super();
    }

    public SessionAlreadyExistsException(String msg) {
        super(msg);
    }
}
