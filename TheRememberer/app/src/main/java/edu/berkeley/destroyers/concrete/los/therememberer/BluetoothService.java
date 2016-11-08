package edu.berkeley.destroyers.concrete.los.therememberer;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by wilsonyan on 11/5/16.
 * Service to connect to the Arduino Bluetooth and communicate with it
 */
public class BluetoothService extends Service {
    public static final String TAG = "BluetoothService";
    public static final UUID uiud = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final long RESPONSE_WAIT_THRESHOLD = 10000;

    public static int KEY_FOB_STATE = 0;
    public static final int KEY_PRESENT = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private Handler bluetoothIn;
    private final int handlerState = 0;

    private static String MAC_ADDRESS;

    private ConnectingThread mConnectingThread;
    private ConnectedThread mConnectedThread;

    private StringBuilder recDataString = new StringBuilder();

    private Handler handler;
    private Runnable runnable;
    private long lastResponse;

    private boolean isWalkingTowardsDoor = false;

    public static boolean CONNECTED = false;
    public static boolean RUNNING = false;
    public static boolean ALREADY_PROMPTED = false;

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

        MAC_ADDRESS = intent.getStringExtra(Keys.BT_DEVICE_KEY);
        Log.d(TAG, MAC_ADDRESS);

        lastResponse = System.currentTimeMillis();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        createBluetoothHandler();
        createResponseHandler();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(Keys.RECONNECT_INTENT);

        checkBTState();
        registerReceiver(signalReceiver, filter);

        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();

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
                if (RUNNING && msg.what == handlerState) {
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
                                scanDevices();
                                sendToReconnect(3000);
                            }
                        }
                        recDataString.delete(0, recDataString.length());
                    }
                }
            }
        };
    }

    private void sendToReconnect(long delay) {
        Intent intent = new Intent(Keys.RECONNECT_INTENT);
        intent.putExtra(Keys.DELAY_KEY, delay);
        sendBroadcast(intent);
    }

    private void createResponseHandler() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (RUNNING && (System.currentTimeMillis() - lastResponse) > RESPONSE_WAIT_THRESHOLD && !reconnecting){
                    sendToReconnect(CONNECTING_DELAY);
                }
                if (RUNNING) {
                    handler.postDelayed(runnable, 5000);
                }
            }
        };
        handler.postDelayed(runnable, 2000);
    }

    private void closeThreads(){
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
            mConnectedThread.interrupt();
            mConnectedThread = null;
        }

        if (mConnectingThread != null) {
            mConnectingThread.interrupt();
            mConnectingThread.closeSocket();
            mConnectingThread = null;
        }
    }

    private static boolean reconnecting = false;
    private static final long CONNECTING_DELAY = 10000; // 5 minutes in milliseconds - 10s now for testing
    private static final long BRIEF_DELAY = 3000;

    private void reconnect(long delay) {
        if (!reconnecting && RUNNING) {
            reconnecting = true;
            if (delay == CONNECTING_DELAY) {
                CONNECTED = false;
                sendIntent(Keys.CONNECTING_INTENT);
            }
            if (KEY_FOB_STATE == KEY_PRESENT && delay == CONNECTING_DELAY && !ALREADY_PROMPTED) {
                sendNotification();
                sendIntent(Keys.FORGOT_KEYS_INTENT);
                ALREADY_PROMPTED = true;
            }
            closeThreads();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkBTState();
                    reconnecting = false;
                }
            }, delay);
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
                    sendToReconnect(CONNECTING_DELAY);
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
                sendToReconnect(CONNECTING_DELAY);
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
                CONNECTED = true;
                ALREADY_PROMPTED = false;
                sendIntent(Keys.CONNECTED_INTENT);
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
                    sendToReconnect(BRIEF_DELAY);
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
                sendToReconnect(BRIEF_DELAY);
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
                        SignalStrength lastLocation = new SignalStrength(System.currentTimeMillis(), rssi);
                        isWalkingTowardsDoor = (System.currentTimeMillis() - lastLocation.getTime()) < SignalStrength.TIME_THRSHOLD
                                && lastLocation.getStrength() > SignalStrength.DISTANCE_THRESHOLD;

                    }
                    break;
                case Keys.RECONNECT_INTENT:
                    long delay = intent.getLongExtra(Keys.DELAY_KEY, CONNECTING_DELAY);
                    Log.d(TAG, "Delay: " + delay);
                    reconnect(delay);
                    break;
            }
        }
    };
}
