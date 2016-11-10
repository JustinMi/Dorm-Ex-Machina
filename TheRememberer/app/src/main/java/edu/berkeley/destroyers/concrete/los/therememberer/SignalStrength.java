package edu.berkeley.destroyers.concrete.los.therememberer;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by wilsonyan on 11/6/16.
 */
public class SignalStrength {
    public static final long TIME_THRSHOLD = 5000;
    public static final int DISTANCE_THRESHOLD = -70;

    private long time;
    private int strength;

    public SignalStrength(long time, int strength) {
        this.time = time;
        this.strength = strength;
    }

    public long getTime() {
        return this.time;
    }

    public int getStrength() {
        return this.strength;
    }

}
