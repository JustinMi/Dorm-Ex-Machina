package edu.berkeley.destroyers.concrete.los.therememberer;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by wilsonyan on 11/6/16.
 */
public class SignalStrength {
    public static final String TAG = "SignalStrength";
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

//    public static ArrayList<SignalStrength> filterTimes(ArrayList<SignalStrength> signalStrengths, long timeThreshold){
//        ArrayList<SignalStrength> recentSignals = new ArrayList<>();
//        for (SignalStrength signal: signalStrengths) {
//            if ((System.currentTimeMillis() - signal.getTime()) < timeThreshold) {
//                recentSignals.add(signal);
//            }
//        }
//        return recentSignals;
//    }
//
//    public static boolean isGettingCloser(ArrayList<SignalStrength> signalStrengths, int distanceThreshold) {
//        int displacement = 0;
//        for (int i=0; i<signalStrengths.size()-1; i++) {
//            displacement += signalStrengths.get(i+1).getStrength() - signalStrengths.get(i).getStrength();
//        }
//
//        Log.d(TAG, "Total displacement" + displacement);
//        return displacement > distanceThreshold;
//    }
}
