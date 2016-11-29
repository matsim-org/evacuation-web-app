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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by laemmel on 28/11/2016.
 */
public class MedianTest {

    @Test
    public void testMedianEvenSize() {
        Median m = new Median();
        for (int i = 1; i < 10; i++) {
            m.addValue(i);
        }
        double median = m.getMedian();

        assertThat(median, is(5.));
    }

    @Test
    public void testMedianOddSize() {
        Median m = new Median();
        for (int i = 1; i <= 10; i++) {
            m.addValue(i);
        }
        double median = m.getMedian();
        assertThat(median, is(5.));
    }

    @Test
    public void testSeveralEqualValuesAsMedian() {
        Median m = new Median();
        m.addValue(1);
        m.addValue(2);
        m.addValue(3);
        m.addValue(2);
        m.addValue(2);
        m.addValue(1);
        m.addValue(3);

        double median = m.getMedian();
        assertThat(median, is(2.));

    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testEmptySize() {
        Median m = new Median();
        m.getMedian();
    }
}
