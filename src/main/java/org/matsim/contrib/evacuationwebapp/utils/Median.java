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

import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by laemmel on 28/11/2016.
 */
public class Median {

    private final List<Double> vals = new ArrayList<>();

    public static void main(String[] args) {
        //Speed test


        final int SZ = 10000000;

        int mIdx = (int) (SZ / 2. + 0.5);


        for (int j = 0; j < 100; j++) {

            System.out.println("Filling arrays with rnd numbers.");
            Median m = new Median();
            List<Double> l = new ArrayList<>();
            for (int i = 1; i < SZ; i++) {
                double val = MatsimRandom.getRandom().nextInt(200);
                m.addValue(val);
                l.add(val);
            }
            System.out.println("Preparation done.");

            {
                long start = System.nanoTime();
                double m1 = m.getMedian();
                long stop = System.nanoTime();
                long total = (stop - start) / 1000 / 1000;
                System.out.println("O(n) Median is: " + m1 + ". Calculation took: " + total);
            }
            {
                long start = System.nanoTime();
                Collections.sort(l);
                double m1 = l.get(mIdx);
                long stop = System.nanoTime();
                long total = (stop - start) / 1000 / 1000;
                System.out.println("O(n log n) Median is: " + m1 + ". Calculation took: " + total);
            }
        }
    }

    public void addValue(double val) {
        vals.add(val);
    }

    public double getMedian() {


        int leftCnt = 0;
        int rightCnt = 0;


        List<Double> current = this.vals;


        while (current.size() > 1) {

            double pivot = current.get(MatsimRandom.getRandom().nextInt(current.size()));
            List<Double> left = new ArrayList<>();
            List<Double> right = new ArrayList<>();


            for (Double d : current) {
                if (d < pivot) {
                    left.add(d);
                } else if (d > pivot) {
                    right.add(d);
                } else if (left.size() > right.size()) {
                    right.add(d);
                } else {
                    left.add(d);
                }
            }


            if (leftCnt + left.size() < rightCnt + right.size()) {
                current = right;
                leftCnt += left.size();
            } else {
                current = left;
                rightCnt += right.size();
            }

        }

        return current.get(0);
    }
}
