package edu.berkeley.destroyers.concrete.los.therememberer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by wilsonyan on 10/7/16.
 */
public class ForgotKeysActivity extends Activity implements View.OnClickListener{
    Button textRoommates, callRA;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.forgot_keys_layout);

        textRoommates = (Button) findViewById(R.id.roommate);
        callRA = (Button) findViewById(R.id.ra1);

        textRoommates.setOnClickListener(this);
        callRA.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.roommate:
                textRoommates();
                break;
            case R.id.ra1:
                callRA();
                break;
            default:
                break;
        }
    }

    private void textRoommates() {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        ArrayList<String> phoneNumbers = new ArrayList<>();
        String[] keys = {Keys.RM1_KEY,Keys.RM2_KEY,Keys.RM3_KEY};
        for (int i=0; i<3; i++) {
            String phoneNumber = getSharedPreferences(Keys.SHARED_PREFS_KEY, Context.MODE_PRIVATE).getString(keys[i], null);
            if (phoneNumber != null) {
                phoneNumbers.add(phoneNumber);
            }
        }

        SmsManager sms = SmsManager.getDefault();
        for (String phoneNumber : phoneNumbers) {
            sms.sendTextMessage(phoneNumber, null, "I forgot my keys! Can I get some help?", sentPI, deliveredPI);
        }
    }

    private void callRA() {
        String phoneNumber = getSharedPreferences(Keys.SHARED_PREFS_KEY, Context.MODE_PRIVATE).getString(Keys.RA1_KEY, "");
        if (phoneNumber.equalsIgnoreCase("")) return;

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber ));
        if(checkIfPermissionGranted(CALL_PERMISSION, this)) {
            startActivity(intent);
        }
    }

    public static final String CALL_PERMISSION = "android.permission.CALL_PHONE";

    public static boolean checkIfPermissionGranted(String permission, Context context) {
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

}
