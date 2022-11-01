package com.themelisx.switch_panel;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.themelisx.switch_panel.BluetoothLeService.UUID_READ_FROM_ESP;
import static com.themelisx.switch_panel.BluetoothLeService.UUID_WRITE_TO_ESP;
import static java.lang.Thread.sleep;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class MainActivity extends Activity implements View.OnTouchListener {

    private final static String TAG = MainActivity.class.getSimpleName();

    public static final int TYPE_SWITCH = 0;
    public static final int TYPE_PUSH_BUTTON = 1;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    public static boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;

    String device_pin = "";
    switches sw;

    final int MAX_SWITCHES = 8;
    int totalSwitches;
    float voltage;

    long start = 0;

    boolean autohide_toolbar;
    boolean runOnceDone = false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    LinearLayout s1, s2, s3, s4, s5, s6, s7, s8;
    AutoResizeTextView text_s1, text_s2, text_s3, text_s4, text_s5, text_s6, text_s7, text_s8;
    ImageView image1, image2, image3, image4, image5, image6, image7, image8;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    void startCommunication() {

        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendData("DEVICE_PIN=" + device_pin);
            }
        }, 1000);

    }
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                //sendStartUpValues();
                startCommunication();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();

                Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_SHORT).show();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Intent i = new Intent(MainActivity.this, DeviceScanActivity.class);
                        i.putExtra("CONNECT_AUTO", false);
                        startActivity(i);
                        finish();
                        //doConnect(null);
                    }
                }, 1000);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                String s = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                Log.e(TAG, "ESP says: " + s);

                if (s.startsWith("SET_SW:")) {
                    s = s.replace("SET_SW:", "");
                    String[] values = s.split("=", 2);
                    int x = Integer.parseInt(values[0]);
                    for (int i=1; i<MAX_SWITCHES; i++) {
                        if (sw.mySwitch[i].connected_to == x) {
                            sw.mySwitch[i].active = Integer.parseInt(values[1]) == 0;
                            break;
                        }
                    }
                    updateActiveButtons();
                } else if (s.startsWith("VOLT:")) {
                    s = s.replace("VOLT:", "");
                    try {
                        voltage = Float.parseFloat(s);

                        //just to update the voltage
                        updateConnectionState(R.string.connected);

                        if (voltage == 0.0) {
                            Log.d(TAG, "Voltage sensor is not connected");
                        } else {
                            Log.d(TAG, "Voltage: " + voltage);
                            //TODO:Optional
                            if (voltage > 3 && voltage < 12) {
                                Log.e(TAG, "Voltage is low!");
                                for (int i=1; i<MAX_SWITCHES+1; i++) {
                                    if (sw.mySwitch[i].active) {
                                        sw.mySwitch[i].active = false;
                                        sendData("SET_OFF=" + sw.mySwitch[i].connected_to);
                                    }
                                    sleep(500);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "ESP sending wrong values. Voltage = " + voltage);
                    }
                }
            } /*else {
                Log.e(TAG, "Action=" + action);
            }*/
        }
    };

    /*
    private void sendStartUpValues() {

        if (!runOnceDone) {
            runOnceDone = true;

            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (int sw = 1; sw < MAX_SWITCHES + 1; sw++) {
                        if (switches[sw]) {
                            sendData(sw + "=ON");
                        }
                    }
                }
            }, 2000);
        }
    }
    */

    @SuppressLint("InlinedApi")
    private static final int UI_OPTIONS =
            View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.fragment_switches_8);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        totalSwitches = sharedPreferences.getInt("num_switches", 1);

        /*
        if (num == 4) {
            setContentView(R.layout.fragment_switches_4);
        } else if (num == 6) {
            setContentView(R.layout.fragment_switches_6);
        } else {
            setContentView(R.layout.fragment_switches_8);
        }
        */
        //final TextView textView = findViewById(R.id.section_label);

        //setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.app_name) + " - " + getString(R.string.init));
            //actionBar.setSubtitle(getString(R.string.init));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        sw = new switches();
        for (int i=1; i<=MAX_SWITCHES;i++) {
            sw.mySwitch[i] = new switch_config();
        }

        Gson gson = new Gson();
        String saved_config = sharedPreferences.getString("config", "");
        if (!saved_config.isEmpty()) {
            sw = gson.fromJson(saved_config, switches.class);
        } else {
            //prompt user running for first time
            //open settings
            for (int i=1; i<=MAX_SWITCHES;i++) {
                sw.mySwitch[i].title = getString(R.string.my_switch) + " " + i;
                sw.mySwitch[i].connected_to = i;
                sw.mySwitch[i].active = false;
                sw.mySwitch[i].type = TYPE_SWITCH;
                sw.mySwitch[i].color = R.drawable.shape_fill_green;
                sw.mySwitch[i].iconID = R.drawable.b_select_img;
            }

            String json = gson.toJson(sw);
            SharedPreferences.Editor esp = sharedPreferences.edit();
            esp.putString("config", json);
            esp.commit();
        }

        for (int i=1; i<=MAX_SWITCHES;i++) {
            sw.mySwitch[i].active = false;
        }

        autohide_toolbar = sharedPreferences.getBoolean("autohide_toolbar", true);

        getWindow().getDecorView()
                .setOnSystemUiVisibilityChangeListener(new View
                        .OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            hideSystemUI();
                        }
                    }
                });

        hideSystemUI();
        hideTabBar();

        image1 = findViewById(R.id.image_s1);
        image2 = findViewById(R.id.image_s2);
        image3 = findViewById(R.id.image_s3);
        image4 = findViewById(R.id.image_s4);
        image5 = findViewById(R.id.image_s5);
        image6 = findViewById(R.id.image_s6);
        image7 = findViewById(R.id.image_s7);
        image8 = findViewById(R.id.image_s8);

        s1 = findViewById(R.id.s1); s1.setOnTouchListener(this);
        s2 = findViewById(R.id.s2); s2.setOnTouchListener(this);
        s3 = findViewById(R.id.s3); s3.setOnTouchListener(this);
        s4 = findViewById(R.id.s4); s4.setOnTouchListener(this);
        s5 = findViewById(R.id.s5); s5.setOnTouchListener(this);
        s6 = findViewById(R.id.s6); s6.setOnTouchListener(this);
        s7 = findViewById(R.id.s7); s7.setOnTouchListener(this);
        s8 = findViewById(R.id.s8); s8.setOnTouchListener(this);

        text_s1 = findViewById(R.id.text_s1);
        text_s2 = findViewById(R.id.text_s2);
        text_s3 = findViewById(R.id.text_s3);
        text_s4 = findViewById(R.id.text_s4);
        text_s5 = findViewById(R.id.text_s5);
        text_s6 = findViewById(R.id.text_s6);
        text_s7 = findViewById(R.id.text_s7);
        text_s8 = findViewById(R.id.text_s8);

        updatePanel(totalSwitches);
        updateButtonsLayout();
        //updateActiveButtons();

        String tmp_device_pin = sharedPreferences.getString("BLE_PIN", "");

        if (!tmp_device_pin.isEmpty()) {
            device_pin = tmp_device_pin;
        } else {

            final EditText inputEditTextField = new EditText(this);
            AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
                    .setTitle(getString(R.string.app_name))
                    .setMessage(getString(R.string.enter_ble_pin))
                    .setView(inputEditTextField)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            device_pin = inputEditTextField.getText().toString();

                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor esp = sharedPreferences.edit();
                            esp.putString("BLE_PIN", device_pin);
                            esp.commit();

                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.show();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        boolean ret = false;


        if (v instanceof LinearLayout) {

            String tag = v.getTag().toString();

            if (!tag.isEmpty()) {
                final int i = Integer.parseInt(v.getTag().toString());

                if (sw.mySwitch[i].type == TYPE_PUSH_BUTTON) {

                    int action = event.getAction() & MotionEvent.ACTION_MASK;
                    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {

                        boolean newState = false;

                        if (action == MotionEvent.ACTION_DOWN) {

                            newState = true;
                            start = System.nanoTime();

                            if (newState != sw.mySwitch[i].active) {
                                sw.mySwitch[i].active = newState;

                                String s = sw.mySwitch[i].active ? "SET_ON" : "SET_OFF";
                                sendData(s + "=" + sw.mySwitch[i].connected_to);
                            }

                        } else if (action == MotionEvent.ACTION_UP) {

                            long delay = 0;

                            newState = false;
                            long finish = System.nanoTime();
                            //Log.e(TAG, "delay=" + (finish - start) / 1000000);
                            long milliseconds = (finish - start) / 1000000;//for milliseconds
                            if (milliseconds < 250) {
                                delay = 250 - milliseconds;
                            }

                            Handler h = new Handler();
                            final boolean finalNewState = newState;
                            h.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (finalNewState != sw.mySwitch[i].active) {
                                        sw.mySwitch[i].active = finalNewState;

                                        String s = sw.mySwitch[i].active ? "SET_ON" : "SET_OFF";
                                        sendData(s + "=" + sw.mySwitch[i].connected_to);
                                    }
                                }
                            }, delay);
                        }
                    }
                } else if (sw.mySwitch[i].type == TYPE_SWITCH) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        sw.mySwitch[i].active = !sw.mySwitch[i].active;
                        //updateActiveButtons();
                        String s = sw.mySwitch[i].active ? "SET_ON" : "SET_OFF";
                        sendData(s + "=" + sw.mySwitch[i].connected_to);
                    }
                }
                //updateActiveButtons();
                ret = true;
            }
        }

        return ret;
    }

    void updateActiveButtons() {

        s1.setBackground(sw.mySwitch[1].active ? getDrawable(sw.mySwitch[1].color) : getDrawable(R.drawable.shape));
        s2.setBackground(sw.mySwitch[2].active ? getDrawable(sw.mySwitch[2].color) : getDrawable(R.drawable.shape));
        s3.setBackground(sw.mySwitch[3].active ? getDrawable(sw.mySwitch[3].color) : getDrawable(R.drawable.shape));
        s4.setBackground(sw.mySwitch[4].active ? getDrawable(sw.mySwitch[4].color) : getDrawable(R.drawable.shape));
        s5.setBackground(sw.mySwitch[5].active ? getDrawable(sw.mySwitch[5].color) : getDrawable(R.drawable.shape));
        s6.setBackground(sw.mySwitch[6].active ? getDrawable(sw.mySwitch[6].color) : getDrawable(R.drawable.shape));
        s7.setBackground(sw.mySwitch[7].active ? getDrawable(sw.mySwitch[7].color) : getDrawable(R.drawable.shape));
        s8.setBackground(sw.mySwitch[8].active ? getDrawable(sw.mySwitch[8].color) : getDrawable(R.drawable.shape));

    }

    void updateButtonsLayout() {
        text_s1.setText(sw.mySwitch[1].title);
        text_s2.setText(sw.mySwitch[2].title);
        text_s3.setText(sw.mySwitch[3].title);
        text_s4.setText(sw.mySwitch[4].title);
        text_s5.setText(sw.mySwitch[5].title);
        text_s6.setText(sw.mySwitch[6].title);
        text_s7.setText(sw.mySwitch[7].title);
        text_s8.setText(sw.mySwitch[8].title);

        image1.setImageDrawable(getResources().getDrawable(sw.mySwitch[1].iconID));
        image2.setImageDrawable(getResources().getDrawable(sw.mySwitch[2].iconID));
        image3.setImageDrawable(getResources().getDrawable(sw.mySwitch[3].iconID));
        image4.setImageDrawable(getResources().getDrawable(sw.mySwitch[4].iconID));
        image5.setImageDrawable(getResources().getDrawable(sw.mySwitch[5].iconID));
        image6.setImageDrawable(getResources().getDrawable(sw.mySwitch[6].iconID));
        image7.setImageDrawable(getResources().getDrawable(sw.mySwitch[7].iconID));
        image8.setImageDrawable(getResources().getDrawable(sw.mySwitch[8].iconID));
    }

    void updatePanel(int num) {

        LinearLayout panel56 = findViewById(R.id.panel56);
        LinearLayout panel78 = findViewById(R.id.panel78);

        switch (num) {
            case 4:
                s5.setVisibility(View.GONE);
                s6.setVisibility(View.GONE);
                s7.setVisibility(View.GONE);
                s8.setVisibility(View.GONE);
                if (panel56 != null) { panel56.setVisibility(View.GONE); }
                if (panel78 != null) { panel78.setVisibility(View.GONE); }
                break;
            case 6:
                s5.setVisibility(View.VISIBLE);
                s6.setVisibility(View.VISIBLE);
                s7.setVisibility(View.GONE);
                s8.setVisibility(View.GONE);
                if (panel56 != null) { panel56.setVisibility(View.VISIBLE); }
                if (panel78 != null) { panel78.setVisibility(View.GONE); }
                break;
            case 8:
                s5.setVisibility(View.VISIBLE);
                s6.setVisibility(View.VISIBLE);
                s7.setVisibility(View.VISIBLE);
                s8.setVisibility(View.VISIBLE);
                if (panel56 != null) { panel56.setVisibility(View.VISIBLE); }
                if (panel78 != null) { panel78.setVisibility(View.VISIBLE); }
                break;
        }
    }

    private void hideTabBar() {

        /*
        if (autohide_toolbar) {
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tabs.setVisibility(View.GONE);
                }
            }, 1000);
        }*/
    }

    private void hideSystemUI() {
        //ActionBar actionBar = getSupportActionBar();
        //if (actionBar != null) actionBar.hide();

        //getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (sharedPreferences.getBoolean("needs_refresh", false)) {
            SharedPreferences.Editor esp = sharedPreferences.edit();
            esp.putBoolean("needs_refresh", false);
            esp.commit();
            recreate();
        }

        //hideSystemUI();
        //hideTabBar();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                ActionBar actionBar = getActionBar();
                if (actionBar != null) {
                    if (resourceId == R.string.connected && voltage > 0) {
                        String str = String.format(Locale.US, "%s - %s (%1.1f V)", getString(R.string.app_name), getString(resourceId), voltage);
                        actionBar.setTitle(str);
                    } else {
                        actionBar.setTitle(String.format(Locale.US, "%s - %s",
                                getString(R.string.app_name), getString(resourceId)));
                    }
                    //actionBar.setSubtitle(resourceId);
                }
            }
        });
    }

    /*
    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }*/

    private void sendData(String data) {

        if (mConnected) {
            if (mWriteCharacteristic != null) {
                byte[] strBytes = data.getBytes();
                mWriteCharacteristic.setValue(strBytes);
                mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
            } else {
                Log.e(TAG, "mWriteCharacteristic is null");
                doDisconnect(null);

                Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_SHORT).show();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Intent i = new Intent(MainActivity.this, DeviceScanActivity.class);
                        i.putExtra("CONNECT_AUTO", false);
                        startActivity(i);
                        finish();
                        //doConnect(null);
                    }
                }, 1000);
            }
        } else {
            Toast.makeText(this, getString(R.string.device_not_connected), Toast.LENGTH_SHORT).show();
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                if (gattCharacteristic.getUuid().equals(UUID_READ_FROM_ESP)) {
                    //Log.e(TAG, "------------------ Found -------------------");
                    mNotifyCharacteristic = gattCharacteristic;
                    mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                }

                if (gattCharacteristic.getUuid().equals(UUID_WRITE_TO_ESP)) {
                    mWriteCharacteristic = gattCharacteristic;
                    //Log.e(TAG, "------------------ Found -------------------");
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void doDisconnect(View view) {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
        }
        //finish();
    }

    public void doConnect(View view) {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        /*
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }*/
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            /*case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;*/
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }



}
