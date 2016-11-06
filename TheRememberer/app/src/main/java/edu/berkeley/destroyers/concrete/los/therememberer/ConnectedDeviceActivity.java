package edu.berkeley.destroyers.concrete.los.therememberer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by wilsonyan on 9/24/16
 */
public class ConnectedDeviceActivity extends Activity {
    public static final String TAG = "ConnectedDeviceActivity";
    public static final String FORGOT_KEYS_INTENT = "forgot_keys";
    public static final String CONNECTING_INTENT = "connecting";
    public static final String CONNECTED_INTENT = "connected";

    Button disconnect;
    TextView status;
    ProgressBar progressBar;
    String address = null;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.led_control_layout);

        address = getIntent().getStringExtra(DeviceList.EXTRA_ADDRESS);
        Log.i(TAG, address);

        disconnect = (Button) findViewById(R.id.disconnect);
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });
        status = (TextView) findViewById(R.id.bt_status);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        status.setText("Connecting...");
        progressBar.setVisibility(View.VISIBLE);

        startBluetoothService();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FORGOT_KEYS_INTENT);
        intentFilter.addAction(CONNECTING_INTENT);
        intentFilter.addAction(CONNECTED_INTENT);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBluetoothService();
    }

    @Override
    public void onStart() {
        super.onStart();
        startBluetoothService();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, BluetoothService.class));
    }

    private void startBluetoothService() {
        if (!BluetoothService.RUNNING) {
            Intent btServiceIntent = new Intent(this, BluetoothService.class);
            btServiceIntent.putExtra(BluetoothService.BT_DEVICE, address);
            startService(btServiceIntent);
            BluetoothService.RUNNING = true;
        }
    }

    private void disconnect() {
        stopService(new Intent(this, BluetoothService.class));
        finish();
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            switch (intent.getAction()) {
                case FORGOT_KEYS_INTENT:
                    Intent activityIntent = new Intent(ConnectedDeviceActivity.this, ForgotKeysActivity.class);
                    startActivity(activityIntent);
                    break;
                case CONNECTING_INTENT:
                    progressBar.setVisibility(View.VISIBLE);
                    status.setText("Connecting...");
                    break;
                case CONNECTED_INTENT:
                    progressBar.setVisibility(View.GONE);
                    status.setText("Connected");
            }


        }
    };

}
