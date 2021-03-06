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

package org.matsim.contrib.evacuationwebapp.utils;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Session ID generator, see: http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
 * Created by laemmel on 17/11/2016.
 */
public class SessionIDGenerator {

    private static final SecureRandom r = new SecureRandom();

    public static String getNextSessionID() {
        return new BigInteger(130, r).toString(32);
    }
}
