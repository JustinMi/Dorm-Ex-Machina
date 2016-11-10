package edu.berkeley.destroyers.concrete.los.therememberer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

/**
 * Created by wilsonyan on 11/6/16.
 */
public class InitialActivity extends Activity{

    @Override
    protected  void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        int i = getSharedPreferences(Keys.SHARED_PREFS_KEY, Context.MODE_PRIVATE).getInt(Keys.FIRST_TIME_KEY, 0);
        if (i == 0){ // First time opending app
            startActivity(new Intent(this, FirstTimeActivity.class));
            SharedPreferences.Editor editor = getSharedPreferences(Keys.SHARED_PREFS_KEY, Context.MODE_PRIVATE).edit();
            editor.putInt(Keys.FIRST_TIME_KEY, 1);
            editor.commit();

            finish();
        } else if (i == 1){ // First time connecting with a device
            startActivity(new Intent(this, DeviceList.class));
            finish();
        } else if (i == 2) { // Already connected with device before
            startActivity(new Intent(this, ConnectedDeviceActivity.class));
            finish();
        }
    }

}
