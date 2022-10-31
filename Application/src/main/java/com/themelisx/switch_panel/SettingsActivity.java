/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.themelisx.switch_panel;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Locale;

public class SettingsActivity extends Activity implements View.OnTouchListener {

    View lastView;
    switches sw;
    final int MAX_SWITCHES = 8;

    private final static String TAG = SettingsActivity.class.getSimpleName();

    public static final int TYPE_SWITCH = 0;
    public static final int TYPE_PUSH_BUTTON = 1;

    LinearLayout s1, s2, s3, s4, s5, s6, s7, s8;
    AutoResizeTextView text_s1, text_s2, text_s3, text_s4, text_s5, text_s6, text_s7, text_s8;
    ImageView image1, image2, image3, image4, image5, image6, image7, image8;

    private String deviceName;
    private String deviceAddress;
    private String device_pin;

    int numberOfSwitches, old_numberOfSwitches;
    int activeSwitch;

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        MenuInflater inflater = getMenuInflater();

        if (v.getId() == R.id.select_panel) {
            inflater.inflate(R.menu.num_of_switches, menu);
        } else if (v.getId() == R.id.select_color) {
            inflater.inflate(R.menu.active_button_colors, menu);
        } else if (v.getId() == R.id.select_image) {
            inflater.inflate(R.menu.select_image, menu);
        } else if (v.getId() == R.id.select_type) {
            inflater.inflate(R.menu.button_types, menu);
        } else if (v.getId() == R.id.select_relay) {
            inflater.inflate(R.menu.select_relay, menu);
        }

    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connected_to_1:
                sw.mySwitch[activeSwitch].connected_to = 1;
                updateTitles();
                break;
            case R.id.connected_to_2:
                sw.mySwitch[activeSwitch].connected_to = 2;
                updateTitles();
                break;
            case R.id.connected_to_3:
                sw.mySwitch[activeSwitch].connected_to = 3;
                updateTitles();
                break;
            case R.id.connected_to_4:
                sw.mySwitch[activeSwitch].connected_to = 4;
                updateTitles();
                break;
            case R.id.connected_to_5:
                sw.mySwitch[activeSwitch].connected_to = 5;
                updateTitles();
                break;
            case R.id.connected_to_6:
                sw.mySwitch[activeSwitch].connected_to = 6;
                updateTitles();
                break;
            case R.id.connected_to_7:
                sw.mySwitch[activeSwitch].connected_to = 7;
                updateTitles();
                break;
            case R.id.connected_to_8:
                sw.mySwitch[activeSwitch].connected_to = 8;
                updateTitles();
                break;

            case R.id.button_switch:
                sw.mySwitch[activeSwitch].type = TYPE_SWITCH;
                break;
            case R.id.button_push:
                sw.mySwitch[activeSwitch].type = TYPE_PUSH_BUTTON;
                break;

            case R.id.color_red:
                sw.mySwitch[activeSwitch].color = R.drawable.shape_fill_red;
                selectSwitch(null);
                break;
            case R.id.color_blue:
                sw.mySwitch[activeSwitch].color = R.drawable.shape_fill_blue;
                selectSwitch(null);
                break;
            case R.id.color_orange:
                sw.mySwitch[activeSwitch].color = R.drawable.shape_fill_orange;
                selectSwitch(null);
                break;
            case R.id.color_green:
                sw.mySwitch[activeSwitch].color = R.drawable.shape_fill_green;
                selectSwitch(null);
                break;

            case R.id.num_4:
                //Toast.makeText(SettingsActivity.this,"You Clicked 4", Toast.LENGTH_SHORT).show();
                numberOfSwitches = 4;
                updatePanel(numberOfSwitches);
                break;
            case R.id.num_6:
                //Toast.makeText(SettingsActivity.this,"You Clicked 6", Toast.LENGTH_SHORT).show();
                numberOfSwitches = 6;
                updatePanel(numberOfSwitches);
                break;
            case R.id.num_8:
                //Toast.makeText(SettingsActivity.this,"You Clicked 8", Toast.LENGTH_SHORT).show();
                numberOfSwitches = 8;
                updatePanel(numberOfSwitches);
                break;
            case R.id.angel_eyes:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_angel_eyes;
                updateImages();
                break;
            case R.id.compressor:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_compressor;
                updateImages();
                break;
            case R.id.diff_lock:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_diff_lock;
                updateImages();
                break;
            case R.id.fog_lights:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_fog_lights;
                updateImages();
                break;
            case R.id.head_lights:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_head_lights;
                updateImages();
                break;
            case R.id.led_bar:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_led_bar;
                updateImages();
                break;
            case R.id.led_lights:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_led_lights;
                updateImages();
                break;
            case R.id.left_lights:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_lights_left;
                updateImages();
                break;
            case R.id.right_lights:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_lights_right;
                updateImages();
                break;
            case R.id.side_lights:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_side_lights;
                updateImages();
                break;
            case R.id.rear_lights:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_rear_lights;
                updateImages();
                break;
            case R.id.winch:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_winch;
                updateImages();
                break;
            case R.id.winch_in:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_winch_in;
                updateImages();
                break;
            case R.id.winch_out:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_winch_out;
                updateImages();
                break;
            case R.id.winch_onoff:
                sw.mySwitch[activeSwitch].iconID = R.drawable.b_winch_onoff;
                updateImages();
                break;

        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor esp = sharedPreferences.edit();
        esp.putInt("num_switches", numberOfSwitches);
        esp.putBoolean("needs_refresh", true);
        esp.commit();

        return true;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void selectSwitch(View view) {

        View newView = null;
        if (view != null) {
            lastView = view;
            newView = view;
        } else {
            newView = lastView;
        }
        s1.setBackground(getDrawable(R.drawable.shape));
        s2.setBackground(getDrawable(R.drawable.shape));
        s3.setBackground(getDrawable(R.drawable.shape));
        s4.setBackground(getDrawable(R.drawable.shape));
        s5.setBackground(getDrawable(R.drawable.shape));
        s6.setBackground(getDrawable(R.drawable.shape));
        s7.setBackground(getDrawable(R.drawable.shape));
        s8.setBackground(getDrawable(R.drawable.shape));

        if (newView != null) {
            int i = Integer.parseInt(newView.getTag().toString());
            activeSwitch = i;
            newView.setBackground(getDrawable(sw.mySwitch[activeSwitch].color));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onPause() {

        Gson gson = new Gson();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String json = gson.toJson(sw);
        SharedPreferences.Editor esp = sharedPreferences.edit();
        esp.putString("config", json);
        esp.commit();

        super.onPause();
    }

    private String createTitle(String relay, int connected_to, String title) {
        return String.format(Locale.US, "%s: %d\n%s", relay, connected_to, title);
    }

    private void updateTitles() {

        text_s1.setText(createTitle(getString(R.string.relay), sw.mySwitch[1].connected_to, sw.mySwitch[1].title));
        text_s2.setText(createTitle(getString(R.string.relay), sw.mySwitch[2].connected_to, sw.mySwitch[2].title));
        text_s3.setText(createTitle(getString(R.string.relay), sw.mySwitch[3].connected_to, sw.mySwitch[3].title));
        text_s4.setText(createTitle(getString(R.string.relay), sw.mySwitch[4].connected_to, sw.mySwitch[4].title));
        text_s5.setText(createTitle(getString(R.string.relay), sw.mySwitch[5].connected_to, sw.mySwitch[5].title));
        text_s6.setText(createTitle(getString(R.string.relay), sw.mySwitch[6].connected_to, sw.mySwitch[6].title));
        text_s7.setText(createTitle(getString(R.string.relay), sw.mySwitch[7].connected_to, sw.mySwitch[7].title));
        text_s8.setText(createTitle(getString(R.string.relay), sw.mySwitch[8].connected_to, sw.mySwitch[8].title));
    }

    private void updateImages() {
        image1.setImageDrawable(getResources().getDrawable(sw.mySwitch[1].iconID));
        image2.setImageDrawable(getResources().getDrawable(sw.mySwitch[2].iconID));
        image3.setImageDrawable(getResources().getDrawable(sw.mySwitch[3].iconID));
        image4.setImageDrawable(getResources().getDrawable(sw.mySwitch[4].iconID));
        image5.setImageDrawable(getResources().getDrawable(sw.mySwitch[5].iconID));
        image6.setImageDrawable(getResources().getDrawable(sw.mySwitch[6].iconID));
        image7.setImageDrawable(getResources().getDrawable(sw.mySwitch[7].iconID));
        image8.setImageDrawable(getResources().getDrawable(sw.mySwitch[8].iconID));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activeSwitch = -1;

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.fragment_settings);

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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        deviceName = sharedPreferences.getString("device_name", "");
        deviceAddress = sharedPreferences.getString("device_address", "");

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.menu_settings));
            actionBar.setSubtitle(deviceName + " - " + deviceAddress);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        old_numberOfSwitches = numberOfSwitches = sharedPreferences.getInt("num_switches", 2);
        updatePanel(numberOfSwitches);

        device_pin = sharedPreferences.getString("BLE_PIN", "");

        sw = new switches();
        for (int i=1; i<=MAX_SWITCHES;i++) {
            sw.mySwitch[i] = new switch_config();
        }

        Gson gson = new Gson();
        String saved_config = sharedPreferences.getString("config", "");
        if (!saved_config.isEmpty()) {
            sw = gson.fromJson(saved_config, switches.class);
            /*for (int i=1; i<=MAX_SWITCHES;i++) {
                //sw.mySwitch[i].color = R.drawable.shape_fill_green;
                //sw.mySwitch[i].iconID =
            }*/
            updateImages();
        } else {
            for (int i=1; i<=MAX_SWITCHES;i++) {
                sw.mySwitch[i].title = getString(R.string.my_switch) + " " + i;
                sw.mySwitch[i].connected_to = i;
                sw.mySwitch[i].active = false;
                sw.mySwitch[i].type = TYPE_SWITCH;
                sw.mySwitch[i].color = R.drawable.shape_fill_green;
                sw.mySwitch[i].iconID = R.drawable.b_select_img;
            }
        }
        updateTitles();

        //String json = gson.toJson(sw);

        final Button select_panel = findViewById(R.id.select_panel);

        final Button select_color = findViewById(R.id.select_color);
        final Button select_image = findViewById(R.id.select_image);
        final Button select_type = findViewById(R.id.select_type);
        final Button select_relay = findViewById(R.id.select_relay);
        final Button select_title = findViewById(R.id.select_title);

        registerForContextMenu(select_color);
        registerForContextMenu(select_panel);
        registerForContextMenu(select_type);
        registerForContextMenu(select_relay);
        registerForContextMenu(select_image);

        select_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final EditText inputEditTextField = new EditText(SettingsActivity.this);
                inputEditTextField.setText(sw.mySwitch[activeSwitch].title);

                AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(SettingsActivity.this, R.style.AlertDialogCustom))
                        .setTitle(getString(R.string.app_name))
                        .setMessage(getString(R.string.select_title))
                        .setView(inputEditTextField)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                sw.mySwitch[activeSwitch].title = inputEditTextField.getText().toString();

                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                dialog.show();
            }
        });

        /*
        select_relay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                PopupMenu popup = new PopupMenu(SettingsActivity.this, Gravity.CENTER);
                popup.getMenuInflater().inflate(R.menu.select_relay, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.connected_to_1:
                                sw.mySwitch[activeSwitch].connected_to = 1;
                                break;
                            case R.id.connected_to_2:
                                sw.mySwitch[activeSwitch].connected_to = 2;
                                break;
                            case R.id.connected_to_3:
                                sw.mySwitch[activeSwitch].connected_to = 3;
                                break;
                            case R.id.connected_to_4:
                                sw.mySwitch[activeSwitch].connected_to = 4;
                                break;
                            case R.id.connected_to_5:
                                sw.mySwitch[activeSwitch].connected_to = 5;
                                break;
                            case R.id.connected_to_6:
                                sw.mySwitch[activeSwitch].connected_to = 6;
                                break;
                            case R.id.connected_to_7:
                                sw.mySwitch[activeSwitch].connected_to = 7;
                                break;
                            case R.id.connected_to_8:
                                sw.mySwitch[activeSwitch].connected_to = 8;
                                break;
                        }

                        updateTitles();

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor esp = sharedPreferences.edit();
                        esp.putBoolean("needs_refresh", true);
                        esp.commit();

                        return true;
                    }
                });

                popup.show();//showing popup menu
            }
        });

        select_type.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                PopupMenu popup = new PopupMenu(SettingsActivity.this, parent_panel);
                popup.getMenuInflater().inflate(R.menu.button_types, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.button_switch:
                                sw.mySwitch[activeSwitch].type = TYPE_SWITCH;
                                break;
                            case R.id.button_push:
                                sw.mySwitch[activeSwitch].type = TYPE_PUSH_BUTTON;
                                break;
                        }

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor esp = sharedPreferences.edit();
                        esp.putBoolean("needs_refresh", true);
                        esp.commit();
                        return true;
                    }
                });

                popup.show();//showing popup menu
            }
        });

        select_color.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                PopupMenu popup = new PopupMenu(SettingsActivity.this, parent_panel);
                popup.getMenuInflater().inflate(R.menu.active_button_colors, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {
                            case R.id.color_red:
                                sw.mySwitch[activeSwitch].color = R.drawable.shape_fill_red;
                                break;
                            case R.id.color_blue:
                                sw.mySwitch[activeSwitch].color = R.drawable.shape_fill_blue;
                                break;
                            case R.id.color_orange:
                                sw.mySwitch[activeSwitch].color = R.drawable.shape_fill_orange;
                                break;
                            case R.id.color_green:
                                sw.mySwitch[activeSwitch].color = R.drawable.shape_fill_green;
                                break;
                        }

                        selectSwitch(null);

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor esp = sharedPreferences.edit();
                        esp.putBoolean("needs_refresh", true);
                        esp.commit();
                        return true;
                    }
                });

                popup.show();//showing popup menu
            }
        });

        final TextView num_of_switches = findViewById(R.id.select_panel);
        //num_of_switches.setText(String.format(Locale.US, getString(R.string.num_of_switches), numberOfSwitches));

        select_panel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                PopupMenu popup = new PopupMenu(SettingsActivity.this, parent_panel);
                popup.getMenuInflater().inflate(R.menu.num_of_switches, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        numberOfSwitches = item.getItemId();

                        switch (item.getItemId()) {
                            case R.id.num_4:
                                //Toast.makeText(SettingsActivity.this,"You Clicked 4", Toast.LENGTH_SHORT).show();
                                numberOfSwitches = 4;
                                updatePanel(numberOfSwitches);
                                break;
                            case R.id.num_6:
                                //Toast.makeText(SettingsActivity.this,"You Clicked 6", Toast.LENGTH_SHORT).show();
                                numberOfSwitches = 6;
                                updatePanel(numberOfSwitches);
                                break;
                            case R.id.num_8:
                                //Toast.makeText(SettingsActivity.this,"You Clicked 8", Toast.LENGTH_SHORT).show();
                                numberOfSwitches = 8;
                                updatePanel(numberOfSwitches);
                                break;
                        }

                        //num_of_switches.setText(String.format(Locale.US, getString(R.string.num_of_switches), numberOfSwitches));

                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor esp = sharedPreferences.edit();
                        esp.putInt("num_switches", numberOfSwitches);
                        esp.putBoolean("needs_refresh", !(numberOfSwitches == old_numberOfSwitches));
                        esp.commit();

                        return true;
                    }
                });

                popup.show();//showing popup menu
            }
        });*/

        selectSwitch(s1);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void toggleSwitch(View view) {
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        boolean ret = false;


        if (v instanceof LinearLayout) {

            String tag = v.getTag().toString();

            if (!tag.isEmpty()) {
                selectSwitch(v);
                ret = true;
            }
        }

        return ret;
    }

    public void selectOption(View view) {
        openContextMenu(view);
    }
}
