package edu.berkeley.destroyers.concrete.los.therememberer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;

/**
 * Created by wilsonyan on 11/6/16.
 */
public class FirstTimeActivity extends Activity implements View.OnClickListener{
    private EditText rm1, rm2, rm3, ra1;
    private Button next;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.first_time_layout);

        rm1 = (EditText) findViewById(R.id.rm1);
        rm2 = (EditText) findViewById(R.id.rm2);
        rm3 = (EditText) findViewById(R.id.rm3);
        ra1 = (EditText) findViewById(R.id.ra1);

        next = (Button) findViewById(R.id.next);

        next.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.next) {
            saveNumbers();
            startActivity(new Intent(this, DeviceList.class));
            finish();
        }
    }

    private void saveNumbers() {
        String rm1Text = rm1.getText().toString();
        String rm2Text = rm2.getText().toString();
        String rm3Text = rm3.getText().toString();
        String ra1Text = ra1.getText().toString();

        SharedPreferences sharedPrefs = getSharedPreferences(Keys.SHARED_PREFS_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();

        if (isPhoneNumber(rm1Text)) {
            editor.putString(Keys.RM1_KEY, rm1Text);
        }
        if (isPhoneNumber(rm2Text)) {
            editor.putString(Keys.RM2_KEY, rm2Text);
        }
        if (isPhoneNumber(rm3Text)) {
            editor.putString(Keys.RM3_KEY, rm3Text);
        }
        if (isPhoneNumber(ra1Text)) {
            editor.putString(Keys.RA1_KEY, ra1Text);
        }
        editor.apply();
    }

    private boolean isPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() != 10) {
            return false;
        }
        for (int i=0; i<phoneNumber.length(); i++) {
            try {
                Integer.parseInt(Character.toString(phoneNumber.charAt(i)));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
}
