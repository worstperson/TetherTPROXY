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

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;
import androidx.core.os.HandlerCompat;
import androidx.core.app.NotificationCompat;

public class ForegroundService extends Service {

    PowerManager powerManager;
    WakeLock wakeLock;

    static public Boolean isStarted = false;

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setContentTitle("Service is running");

    final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable delayedRestore = new Runnable() {
        @Override
        public void run() {
            if (isStarted) {
                SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
                String ipv4Addr = sharedPref.getString("ipv4Addr", "10.0.0.1");
                String iface = sharedPref.getString("iface", null);
                if (Script.isDnsmasqRunning()) {
                    if (iface == null && (iface = Script.getTetherInterface()) != null) {
                        SharedPreferences.Editor edit = sharedPref.edit();
                        edit.putString("iface", iface);
                        edit.apply();
                        Script.configureTether(ipv4Addr, iface);
                        Script.startDnsmasq(ipv4Addr, iface, getFilesDir().getPath());
                        if (!HandlerCompat.hasCallbacks(handler2, delayedWatchdog)) {
                            handler2.postDelayed(delayedWatchdog, 10000);
                        }
                    } else {
                        Log.i("TetherTPROXY", "Tether is already applied...");
                    }
                } else {
                    if (iface != null) {
                        Log.i("TetherTPROXY", "Stopping tether operation");
                        SharedPreferences.Editor edit = sharedPref.edit();
                        edit.putString("iface", null);
                        edit.apply();
                        Script.stopDnsmasq(ipv4Addr, iface);
                        if (!HandlerCompat.hasCallbacks(handler2, delayedWatchdog)) {
                            handler2.removeCallbacks(delayedWatchdog);
                        }
                    } else {
                        Log.i("TetherTPROXY", "Tethering is not active");
                    }
                }
            }
        }
    };

    final Handler handler2 = new Handler(Looper.getMainLooper());
    private Runnable delayedWatchdog = new Runnable() {
        @Override
        public void run() {
            SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
            String ipv4Addr = sharedPref.getString("ipv4Addr", "10.0.0.1");
            String iface = sharedPref.getString("iface", null);
            if (isStarted && iface != null) {
                String pid = Script.getDnsmasqPid(getFilesDir().getPath());
                if (pid != null) {
                    if (!Script.isDnsmasqRunning(pid)) {
                        Log.i("TetherTPROXY", "Restarting Dnsmasq");
                        Script.startDnsmasq(ipv4Addr, iface, getFilesDir().getPath());
                    }
                    if (!HandlerCompat.hasCallbacks(handler2, delayedWatchdog)) {
                        handler2.postDelayed(delayedWatchdog, 10000);
                    }
                } else {
                    Log.i("TetherTPROXY", "Try Restarting Dnsmasq");
                    Script.startDnsmasq(ipv4Addr, iface, getFilesDir().getPath());
                    if (!HandlerCompat.hasCallbacks(handler2, delayedWatchdog)) {
                        handler2.postDelayed(delayedWatchdog, 10000);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver TetherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("TetherTPROXY", "Recieved TETHER_STATE_CHANGED broadcast");
            if (!HandlerCompat.hasCallbacks(handler, delayedRestore)) {
                Log.i("TetherTPROXY", "Creating callback to restore tether in 5 seconds...");
                handler.postDelayed(delayedRestore, 5000);
            } else {
                Log.i("TetherTPROXY", "Tether restore callback already scheduled");
            }
        }
    };

    private final BroadcastReceiver RestartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isStarted) {
                String name = intent.getStringExtra("name");
                Log.i("TetherTPROXY", "Restarting service " + name);
                Intent i = new Intent("com.worstperson.tethertproxy." + name + ".START");
                i.setPackage("com.worstperson.tethertproxy");
                sendBroadcast(i);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        isStarted = true;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tether TPROXY::TetherWakelockTag");
        }
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(serviceChannel);
            }
        }

        SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String ipv4Addr = sharedPref.getString("ipv4Addr", "10.0.0.1");
        String iface = sharedPref.getString("iface", null);
        if (iface != null) {
            Log.i("TetherTPROXY", "Stopping tether operation");
            SharedPreferences.Editor edit = sharedPref.edit();
            edit.putString("iface", null);
            edit.apply();
            Script.stopDnsmasq(ipv4Addr, iface);
        }

        Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();

        startForeground(1, notification.build());

        Script.addRules(ipv4Addr);

        if (!HandlerCompat.hasCallbacks(handler, delayedRestore)) {
            handler.postDelayed(delayedRestore, 5000);
        }

        registerReceiver(TetherReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
        registerReceiver(RestartReceiver, new IntentFilter("com.worstperson.tethertproxy.ForegroundService.CHECK"));

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isStarted = false;

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        try {
            unregisterReceiver(TetherReceiver);
        } catch (IllegalArgumentException ignored) {}
        try {
            unregisterReceiver(RestartReceiver);
        } catch (IllegalArgumentException ignored) {}

        if (!HandlerCompat.hasCallbacks(handler, delayedRestore)) {
            handler.removeCallbacks(delayedRestore);
        }
        if (!HandlerCompat.hasCallbacks(handler2, delayedWatchdog)) {
            handler2.removeCallbacks(delayedWatchdog);
        }

        SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String ipv4Addr = sharedPref.getString("ipv4Addr", "10.0.0.1");
        String iface = sharedPref.getString("iface", null);

        if (iface != null) {
            Log.i("TetherTPROXY", "Stopping tether operation");
            SharedPreferences.Editor edit = sharedPref.edit();
            edit.putString("iface", null);
            edit.apply();
            Script.stopDnsmasq(ipv4Addr, iface);
        }
        Script.removeRules(ipv4Addr);

        Toast.makeText(this, "Service destroyed by user.", Toast.LENGTH_LONG).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
