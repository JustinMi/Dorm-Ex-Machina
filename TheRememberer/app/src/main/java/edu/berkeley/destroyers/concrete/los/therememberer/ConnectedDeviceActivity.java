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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by wilsonyan on 9/24/16
 */
public class ConnectedDeviceActivity extends Activity {
    public static final String TAG = "ConnectedDeviceActivity";

    private Button disconnect, settings;
    private TextView status;
    private ProgressBar progressBar;
    private ImageView checkMark;
    private String address = null;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.led_control_layout);

        address = getIntent().getStringExtra(Keys.EXTRA_ADDRESS_KEY);
        Log.i(TAG, address);

        disconnect = (Button) findViewById(R.id.disconnect);
        settings = (Button) findViewById(R.id.settings);
        status = (TextView) findViewById(R.id.bt_status);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        checkMark = (ImageView) findViewById(R.id.checkmark);

        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ConnectedDeviceActivity.this, Settings.class));
            }
        });


        // Initialize to connecting status
        status.setText(R.string.connecting);
        progressBar.setVisibility(View.VISIBLE);
        checkMark.setVisibility(View.GONE);

        startBluetoothService();
        setupConnectingReceiver();
    }

    private void setupConnectingReceiver() {
        IntentFilter connectingFilter = new IntentFilter();
        connectingFilter.addAction(Keys.CONNECTING_INTENT);
        connectingFilter.addAction(Keys.CONNECTED_INTENT);
        registerReceiver(connectingReceiver, connectingFilter);
    }

    private void setupForgotReceiver() {
        IntentFilter forgotFilter = new IntentFilter();
        forgotFilter.addAction(Keys.FORGOT_KEYS_INTENT);
        registerReceiver(forgotReceiver, forgotFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(forgotReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        startBluetoothService();
        updateUI();
        setupForgotReceiver();
    }

    @Override
    public void onStart() {
        super.onStart();
        startBluetoothService();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(connectingReceiver);
        stopService(new Intent(this, BluetoothService.class));
    }

    private void startBluetoothService() {
        if (!BluetoothService.RUNNING) {
            Intent btServiceIntent = new Intent(this, BluetoothService.class);
            btServiceIntent.putExtra(Keys.BT_DEVICE_KEY, address);
            startService(btServiceIntent);
            BluetoothService.RUNNING = true;
        }
    }

    private void disconnect() {
        stopService(new Intent(this, BluetoothService.class));
        finish();
    }

    private void updateUI() {
        if (BluetoothService.CONNECTED) {
            progressBar.setVisibility(View.GONE);
            checkMark.setVisibility(View.VISIBLE);
            status.setText(R.string.connected);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            checkMark.setVisibility(View.GONE);
            status.setText(R.string.connecting);
        }
    }

    final BroadcastReceiver connectingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "RECEIVED: " + intent.getAction());
            String action = intent.getAction();

            if (action.equals(Keys.CONNECTED_INTENT) || action.equals(Keys.CONNECTING_INTENT)) {
                updateUI();
            }
        }
    };

    final BroadcastReceiver forgotReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "RECEIVED: " + intent.getAction());
            if (intent.getAction().equals(Keys.FORGOT_KEYS_INTENT)) {
                Intent activityIntent = new Intent(ConnectedDeviceActivity.this, ForgotKeysActivity.class);
                startActivity(activityIntent);
            }
        }
    };

}
