package edu.berkeley.destroyers.concrete.los.therememberer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class DeviceList extends AppCompatActivity {
    public static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";

    Button connect;
    ListView deviceList;

    private BluetoothAdapter bluetoothAdapter = null;
    //private Set<BluetoothDevice> pairedDevices;
    private AdapterView.OnItemClickListener deviceListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int a2, long a3) {
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            Intent i = new Intent(DeviceList.this, ConnectedDeviceActivity.class);
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
            startActivity(i);

            SharedPreferences.Editor editor = getSharedPreferences(Tools.SHARED_PREFS_KEY, Context.MODE_PRIVATE).edit();
            editor.putString(Tools.PAIRED_DEVICE_KEY, address);
            editor.commit();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connect = (Button) findViewById(R.id.connect);
        deviceList = (ListView) findViewById(R.id.pairedDevices);

        String address = getSharedPreferences(Tools.SHARED_PREFS_KEY, Context.MODE_PRIVATE).getString(Tools.PAIRED_DEVICE_KEY, null);
        if (address != null) {
            Intent i = new Intent(DeviceList.this, ConnectedDeviceActivity.class);
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
            startActivity(i);
        }

        setupBluetooth();

    }

    @Override
    public void onResume() {
        super.onResume();
        setupBluetooth();
    }

    private void setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "No Bluetooth Available", Toast.LENGTH_LONG).show();
            finish();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent turnBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBluetoothOn,1);
        }
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pairedDevicesList();
            }
        });
    }

    private void pairedDevicesList() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ArrayList<String> list = new ArrayList<>();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName() + "\n" + bt.getAddress());
            }
        } else {
            Toast.makeText(this, "No Paired Bluetooth Devices Available",Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,list);
        deviceList.setAdapter(adapter);
        deviceList.setOnItemClickListener(deviceListClickListener);
    }
}
