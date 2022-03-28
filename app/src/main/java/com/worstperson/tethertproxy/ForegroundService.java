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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

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
                restoreTether();
            }
        }
    };

    private String getPrefix(String iface, String ipv6Prefix) {
        try {
            NetworkInterface netint = NetworkInterface.getByName(iface);
            if (netint != null) {
                for (InetAddress inetAddress : Collections.list(netint.getInetAddresses())) {
                    if (inetAddress instanceof Inet6Address && !inetAddress.isLinkLocalAddress() && inetAddress.getHostAddress() != null && !inetAddress.getHostAddress().equals(ipv6Prefix + "1")) {
                        String ipv6Addr = inetAddress.getHostAddress();
                        if (ipv6Addr.contains("::")) {
                            return ipv6Addr.split("::")[0];
                        } else {
                            String[] tmp = ipv6Addr.split(":");
                            return tmp[0] + ":" + tmp[1] + ":" + tmp[2] + ":" + tmp[3];
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void restoreTether() {
        SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean dnsmasq = sharedPref.getBoolean("dnsmasq", false);
        String ipv6Prefix = sharedPref.getBoolean("ipv6Default", false) ? "2001:db8::" : "fd00::";
        String ipv4Addr = sharedPref.getString("ipv4Addr", "192.168.42.129");
        String iface = Script.getTetherInterface();
        if (iface != null) {
            if (dnsmasq) {
                Log.i("TetherTPROXY", "Applied addresses to tether interface");
                if (Script.setTetherAddresses(iface, ipv4Addr, ipv6Prefix)){
                    Script.configureRoutes(ipv4Addr, ipv6Prefix, iface);
                } else {
                    Log.i("TetherTPROXY", "Interface already configured");
                }
            }
            String tmp = getPrefix(iface, ipv6Prefix);
            if (tmp != null) {
                Log.i("TetherTPROXY", tmp);
                Script.setTetherRoute(tmp);
            }
        } else {
            Log.i("TetherTPROXY", "Tether interface not found...");
        }
    }

    private final BroadcastReceiver TetherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("TetherTPROXY", "Recieved TETHER_STATE_CHANGED broadcast");
            if (!HandlerCompat.hasCallbacks(handler, delayedRestore)) {
                Log.i("TetherTPROXY", "Tetherable devices connected");
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

        if (Script.hasTPROXY()) {

            SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
            boolean dpiCircumvention = sharedPref.getBoolean("dpiCircumvention", false);
            boolean dropForward = sharedPref.getBoolean("dropForward", true);
            Boolean dnsmasq = sharedPref.getBoolean("dnsmasq", false);
            String ipv6Prefix = sharedPref.getBoolean("ipv6Default", false) ? "2001:db8::" : "fd00::";
            String ipv4Addr = sharedPref.getString("ipv4Addr", "192.168.42.129");

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

            Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();

            startForeground(1, notification.build());

            Script.configureTPROXY(ipv4Addr, ipv6Prefix, dnsmasq, getFilesDir().getPath(), dpiCircumvention, dropForward);

            if (dnsmasq) {
                Intent it = new Intent(this, DnsmasqService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(it);
                } else {
                    startService(it);
                }
            }

            if (dpiCircumvention) {
                Intent it = new Intent(this, TPWSService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(it);
                } else {
                    startService(it);
                }
            }

            Intent it = new Intent(this, Socks5Service.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it);
            } else {
                startService(it);
            }

            it = new Intent(this, TPROXYService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it);
            } else {
                startService(it);
            }

            it = new Intent(this, TPROXY4Service.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it);
            } else {
                startService(it);
            }

            if (!HandlerCompat.hasCallbacks(handler, delayedRestore)) {
                handler.postDelayed(delayedRestore, 5000);
            }

            registerReceiver(TetherReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
            registerReceiver(RestartReceiver, new IntentFilter("com.worstperson.tethertproxy.ForegroundService.CHECK"));
        }

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

        SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        boolean dpiCircumvention = sharedPref.getBoolean("dpiCircumvention", false);
        boolean dnsmasq = sharedPref.getBoolean("dnsmasq", false);
        String ipv4Addr = sharedPref.getString("ipv4Addr", "192.168.42.129");
        boolean dropForward = sharedPref.getBoolean("dropForward", true);

        Script.unconfigureTPROXY(ipv4Addr, dnsmasq, dpiCircumvention, dropForward);

        stopService(new Intent(this, DnsmasqService.class));
        stopService(new Intent(this, TPWSService.class));
        stopService(new Intent(this, Socks5Service.class));
        stopService(new Intent(this, TPROXYService.class));
        stopService(new Intent(this, TPROXY4Service.class));

        Toast.makeText(this, "Service destroyed by user.", Toast.LENGTH_LONG).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
