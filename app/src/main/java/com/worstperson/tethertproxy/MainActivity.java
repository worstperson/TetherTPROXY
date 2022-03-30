/*
        Copyright 2022 worstperson

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/

package com.worstperson.tethertproxy;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    PowerManager powerManager;

    @SuppressLint({"UseSwitchCompatOrMaterialCode", "BatteryLife", "WrongConstant"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View view = findViewById(android.R.id.content);

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            Snackbar.make(view, "IGNORE_BATTERY_OPTIMIZATIONS", Snackbar.LENGTH_INDEFINITE).setAction(
                    "Grant", new View.OnClickListener() {
                        @Override
                        public void onClick(View view1) {
                            MainActivity.this.startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:" + MainActivity.this.getPackageName())));
                        }
                    }).show();
        }

        // FIXME - Build these dependencies as libraries and add them to the project
        // Really though, this is getting stupid. Adding root services and bindings is not that difficult.

        File file = new File(getFilesDir().getPath() + "/dnsmasq.armeabi-v7a");
        if (!file.exists()) {
            try (InputStream in = getResources().openRawResource(R.raw.dnsmasq_arm)) {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        file.setExecutable(true);

        file = new File(getFilesDir().getPath() + "/dnsmasq.arm64-v8a");
        if (!file.exists()) {
            try (InputStream in = getResources().openRawResource(R.raw.dnsmasq_arm64)) {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        file.setExecutable(true);

        Switch service_switch = findViewById(R.id.service_switch);
        EditText ipv4_text = findViewById(R.id.ipv4_text);

        SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean serviceEnabled = sharedPref.getBoolean("serviceEnabled", false);
        String ipv4Addr = sharedPref.getString("ipv4Addr", "10.0.0.1");

        service_switch.setChecked(serviceEnabled);

        ipv4_text.setText(ipv4Addr);

        if (serviceEnabled) {
            ipv4_text.setEnabled(false);
        }

        service_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ipv4_text.setEnabled(!isChecked);
                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putBoolean("serviceEnabled", isChecked);
                edit.apply();
                Intent it = new Intent(MainActivity.this, ForegroundService.class);
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MainActivity.this.startForegroundService(it);
                    } else {
                        MainActivity.this.startService(it);
                    }
                } else {
                    MainActivity.this.stopService(it);
                }
            }
        });

        ipv4_text.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Pattern sPattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
                    if (sPattern.matcher(String.valueOf(ipv4_text.getText())).matches()) {
                        SharedPreferences.Editor edit = sharedPref.edit();
                        edit.putString("ipv4Addr", String.valueOf(ipv4_text.getText()));
                        edit.apply();
                        return false;
                    }
                }
                return true;
            }
        });

        if (serviceEnabled && !ForegroundService.isStarted) {
            Intent it = new Intent(MainActivity.this, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it);
            } else {
                startService(it);
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }
}