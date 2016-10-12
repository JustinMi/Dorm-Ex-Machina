package edu.berkeley.destroyers.concrete.los.therememberer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by wilsonyan on 10/7/16.
 */
public class Background extends Service {


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
