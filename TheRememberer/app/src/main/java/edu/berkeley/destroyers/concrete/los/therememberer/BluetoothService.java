package edu.berkeley.destroyers.concrete.los.therememberer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by wilsonyan on 11/5/16.
 */
public class BluetoothService extends Service {
    public static final String TAG = "BluetoothService";
    public static boolean RUNNING = false;

    public static final UUID uiud = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String BT_DEVICE = "btdevice";
    private static final long RESPONSE_WAIT_THRESHOLD = 10000;

    public static int KEY_FOB_STATE = 0;
    public static final int KEY_PRESENT = 1;
    public static final int KEY_NONE = 0;

    public static final int STATE_CONNECTED = 1;
    public static final int STATE_CONNECTING = 2;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler bluetoothIn;
    final int handlerState = 0;

    private static int mState = STATE_CONNECTING;
    private static String MAC_ADDRESS;

    private ConnectingThread mConnectingThread;
    private ConnectedThread mConnectedThread;

    private StringBuilder recDataString = new StringBuilder();
    private long lastResponse;

    private Handler handler;
    private Runnable runnable;
    private SignalStrength lastLocation;
    private boolean isWalkingTowardsDoor = false;


    @Override
    public void onCreate(){
        Log.d(TAG, "Service Started");
        super.onCreate();

        buildNotification();
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG, "On Start Command");
        RUNNING = true;

        MAC_ADDRESS = intent.getStringExtra(BT_DEVICE);
        Log.d(TAG, MAC_ADDRESS);

        lastResponse = System.currentTimeMillis();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        createBluetoothHandler();
        createResponseHandler();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(signalReceiver, filter);

        return super.onStartCommand(intent, flags, startId);
    }

    private void scanDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.startDiscovery();
        }
    }

    private void createBluetoothHandler() {
        bluetoothIn = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("~");
                    if (endOfLineIndex > 0) {
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);
                        if (recDataString.charAt(0) == '#')
                        {
                            String valString = dataInPrint.substring(1);
                            if (valString.equalsIgnoreCase("1") || valString.equalsIgnoreCase("0")) {
                                lastResponse = System.currentTimeMillis();
                                KEY_FOB_STATE = Integer.parseInt(valString);
                                Log.d(TAG, "KEY_FOB_STATE: " + KEY_FOB_STATE);
//                                mConnectedThread.write("2");

                                scanDevices();
                                reconnect(3000);


                            }
                        }
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };
    }

    private void createResponseHandler() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if ((System.currentTimeMillis() - lastResponse) > RESPONSE_WAIT_THRESHOLD){
                    reconnect(CONNECTING_DELAY);
                }

                if (RUNNING) {
                    handler.postDelayed(runnable, 5000);
                }
            }
        };
        handler.postDelayed(runnable, 2000);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        closeThreads();
        unregisterReceiver(signalReceiver);

        RUNNING = false;
        Log.d(TAG, "Service Destroyed");
    }

    private void closeThreads(){
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread.interrupt();
            mConnectedThread = null;
        }

        if (mConnectingThread != null) {
            mConnectingThread.interrupt();
            mConnectingThread = null;
        }
    }

    private void turnOnLED(){
        if (mState == STATE_CONNECTED) {
            mConnectedThread.write("1");
        }
    }

    private void turnOffLED() {
        if (mState == STATE_CONNECTED) {
            mConnectedThread.write("0");
        }
    }

    private static boolean reconnecting = false;
    private static final long CONNECTING_DELAY = 10000; // 5 minutes in milliseconds

    private void reconnect(long delay) {
        if (!reconnecting) {
            mState = STATE_CONNECTING;
            sendIntent(ConnectedDeviceActivity.CONNECTING_INTENT);

            if (KEY_FOB_STATE == KEY_PRESENT && delay == CONNECTING_DELAY) {
                sendNotification();
                sendIntent(ConnectedDeviceActivity.FORGOT_KEYS_INTENT);
            }

            reconnecting = true;
            closeThreads();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {}

            Log.d(TAG, "Reconnecting...");
            if (mState == STATE_CONNECTING) {
                checkBTState();
            }
            reconnecting = false;
        }
    }

    private void checkBTState(){
        if (mBluetoothAdapter == null){
            Log.d(TAG, "Bluetooth not supported by device, STOPPING service");
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                try {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS);
                    mConnectingThread = new ConnectingThread(device);
                    mConnectingThread.start();
                } catch(IllegalArgumentException e){
                    Log.d(TAG, "Failed to connect to BT Device");
                    reconnect(CONNECTING_DELAY);
                }
            }
        }
    }

    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    int mNotificationId = 001;

    private void buildNotification() {
        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("You forgot your keys!")
                .setContentText("Tap here to notify your friends or RA");
        Intent resultIntent = new Intent(this, ForgotKeysActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void sendNotification() {
        mNotificationManager.notify(mNotificationId, mBuilder.build());
    }

    private void sendIntent(String intentIdentifier) {
        Intent intent = new Intent(intentIdentifier);
        sendBroadcast(intent);
    }

    private class ConnectingThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public ConnectingThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket temp = null;
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(uiud);
            } catch (IOException e) {
                Log.d(TAG, "Failed to create socket");
                reconnect(CONNECTING_DELAY);
            }
            mmSocket = temp;
        }

        @Override
        public void run() {
            super.run();
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();

                if (isWalkingTowardsDoor) {
                    mConnectedThread.write("3");
                } else {
                    mConnectedThread.write("2");
                }
                mState = STATE_CONNECTED;
                sendIntent(ConnectedDeviceActivity.CONNECTED_INTENT);
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                }
            } catch (IllegalStateException e) {
            }
        }

        public void closeSocket() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (RUNNING) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Failed to read message: " + e.toString());
                    reconnect(CONNECTING_DELAY);
                    break;
                }
            }
        }

        public void write(String input) {
            byte[] msgBuffer = input.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "Failed to write message: " + input);
                reconnect(CONNECTING_DELAY);
            }
        }

        public void closeStreams() {
            try {
                mmInStream.close();
                mmOutStream.close();
            } catch (IOException e) {
            }
        }
    }

    private final BroadcastReceiver signalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "RECEIVED: " + intent.getAction());
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    if (device.getAddress().equalsIgnoreCase(MAC_ADDRESS)) {
                        Log.d(TAG, device.getName() + "\n" + device.getAddress() + " " + rssi);
                        lastLocation = new SignalStrength(System.currentTimeMillis(), rssi);
                        isWalkingTowardsDoor = (System.currentTimeMillis() - lastLocation.getTime()) < SignalStrength.TIME_THRSHOLD
                                && lastLocation.getStrength() > SignalStrength.DISTANCE_THRESHOLD;

                    }
                    break;
            }
        }
    };
}
