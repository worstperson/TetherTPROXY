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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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

        file = new File(getFilesDir().getPath() + "/tpws.armeabi-v7a");
        if (!file.exists()) {
            try (InputStream in = getResources().openRawResource(R.raw.tpws_arm)) {
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

        file = new File(getFilesDir().getPath() + "/tpws.arm64-v8a");
        if (!file.exists()) {
            try (InputStream in = getResources().openRawResource(R.raw.tpws_arm64)) {
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

        file = new File(getFilesDir().getPath() + "/hev-socks5-server.armeabi-v7a");
        if (!file.exists()) {
            try (InputStream in = getResources().openRawResource(R.raw.hevserver_arm)) {
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

        file = new File(getFilesDir().getPath() + "/hev-socks5-server.arm64-v8a");
        if (!file.exists()) {
            try (InputStream in = getResources().openRawResource(R.raw.hevserver_arm64)) {
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

        file = new File(getFilesDir().getPath() + "/hev-socks5-tproxy.armeabi-v7a");
        if (!file.exists()) {
            try (InputStream in = getResources().openRawResource(R.raw.hevtproxy_arm)) {
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

        file = new File(getFilesDir().getPath() + "/hev-socks5-tproxy.arm64-v8a");
        if (!file.exists()) {
            try (InputStream in = getResources().openRawResource(R.raw.hevtproxy_arm64)) {
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

        file = new File(getFilesDir().getPath() + "/socks.yml");
        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("main:\n");
                writer.append("  workers: 20\n");
                writer.append("  port: 1080\n");
                writer.append("  listen-address: '::1'\n");
                writer.append("  listen-ipv6-only: false\n");
                writer.append("  bind-address: '::'\n\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        file = new File(getFilesDir().getPath() + "/tproxy4.yml");
        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("socks5:\n");
                writer.append("  port: 1080\n");
                writer.append("  address: '::1'\n\n");
                writer.append("tcp:\n");
                writer.append("  port: 1088\n");
                writer.append("  address: '127.0.0.1'\n\n");
                writer.append("udp:\n");
                writer.append("  port: 1088\n");
                writer.append("  address: '127.0.0.1'\n\n");
                writer.append("dns:\n");
                writer.append("  port: 5353\n");
                writer.append("  address: '::'\n");
                writer.append("  upstream: 8.8.8.8\n\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        file = new File(getFilesDir().getPath() + "/tproxy.yml");
        if (!file.exists()) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.append("socks5:\n");
                writer.append("  port: 1080\n");
                writer.append("  address: '::1'\n\n");
                writer.append("tcp:\n");
                writer.append("  port: 1088\n");
                writer.append("  address: '::1'\n\n");
                writer.append("udp:\n");
                writer.append("  port: 1088\n");
                writer.append("  address: '::1'\n\n");
                writer.append("dns:\n");
                writer.append("  port: 5353\n");
                writer.append("  address: '::'\n");
                writer.append("  upstream: 8.8.8.8\n\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        TextView net_textview = findViewById(R.id.net_textview);
        Switch service_switch = findViewById(R.id.service_switch);
        Switch block_switch = findViewById(R.id.block_switch);
        Switch dnsmasq_switch = findViewById(R.id.dnsmasq_switch);
        Switch dpi_switch = findViewById(R.id.dpi_switch);
        Spinner prefix_spinner = findViewById(R.id.prefix_spinner);
        EditText ipv4_text = findViewById(R.id.ipv4_text);

        SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean serviceEnabled = sharedPref.getBoolean("serviceEnabled", false);
        boolean dropForward = sharedPref.getBoolean("dropForward", true);
        boolean dnsmasq = sharedPref.getBoolean("dnsmasq", false);
        boolean dpiCircumvention = sharedPref.getBoolean("dpiCircumvention", false);
        String ipv4Addr = sharedPref.getString("ipv4Addr", "192.168.42.129");
        boolean ipv6Default = sharedPref.getBoolean("ipv6Default", false); // ? "2001:db8::" : "fd00::";

        service_switch.setChecked(serviceEnabled);
        block_switch.setChecked(dropForward);
        dnsmasq_switch.setChecked(dnsmasq);
        dpi_switch.setChecked(dpiCircumvention);

        ipv4_text.setText(ipv4Addr);

        ArrayList<String> arraySpinner3 = new ArrayList<>();
        arraySpinner3.add("ULA (fd00::)");     // Prefer IPv4
        arraySpinner3.add("GUA (2001:db8::)"); // Prefer IPv6
        ArrayAdapter<String> adapter3 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, arraySpinner3);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prefix_spinner.setAdapter(adapter3);
        prefix_spinner.setSelection(ipv6Default ? 1 : 0);

        if (serviceEnabled) {
            block_switch.setEnabled(false);
            dnsmasq_switch.setEnabled(false);
            dpi_switch.setEnabled(false);
            ipv4_text.setEnabled(false);
            prefix_spinner.setEnabled(false);
        }

        service_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                block_switch.setEnabled(!isChecked);
                dnsmasq_switch.setEnabled(!isChecked);
                dpi_switch.setEnabled(!isChecked);
                ipv4_text.setEnabled(!isChecked);
                prefix_spinner.setEnabled(!isChecked);
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

        block_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putBoolean("dropForward", isChecked);
                edit.apply();
            }
        });

        dnsmasq_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putBoolean("dnsmasq", isChecked);
                edit.apply();
            }
        });

        dpi_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putBoolean("dpiCircumvention", isChecked);
                edit.apply();
            }
        });

        prefix_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                SharedPreferences.Editor edit = sharedPref.edit();
                edit.putBoolean("ipv6Default", position == 1);
                edit.apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
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

        if (!Script.hasTPROXY()) {
            net_textview.setText("FAIL");
            service_switch.setEnabled(false);
            block_switch.setEnabled(false);
            dnsmasq_switch.setEnabled(false);
            dpi_switch.setEnabled(false);
            ipv4_text.setEnabled(false);
            prefix_spinner.setEnabled(false);
        } else {
            net_textview.setText("PASS");
            if (serviceEnabled && !ForegroundService.isStarted) {
                Intent it = new Intent(MainActivity.this, ForegroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(it);
                } else {
                    startService(it);
                }
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
    }
}