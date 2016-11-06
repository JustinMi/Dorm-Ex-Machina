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
    public static final String FIRST_TIME_KEY = "first_time";

    @Override
    protected  void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        int i = getSharedPreferences(Tools.SHARED_PREFS_KEY, Context.MODE_PRIVATE).getInt(FIRST_TIME_KEY, 0);
        if (i == 0){
            startActivity(new Intent(this, FirstTimeActivity.class));
            SharedPreferences.Editor editor = getSharedPreferences(Tools.SHARED_PREFS_KEY, Context.MODE_PRIVATE).edit();
            editor.putInt(FIRST_TIME_KEY, 1);
            editor.commit();

            finish();
        } else if (i == 1){
            startActivity(new Intent(this, DeviceList.class));
            finish();
        } else if (i == 2) {
            startActivity(new Intent(this, ConnectedDeviceActivity.class));
            finish();
        }
    }

}
