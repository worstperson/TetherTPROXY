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
import android.util.Log;
import androidx.core.os.HandlerCompat;
import androidx.core.app.NotificationCompat;

import com.topjohnwu.superuser.Shell;

public class DnsmasqService extends Service {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setContentTitle("Service is running");

    final Handler handler = new Handler(Looper.getMainLooper());

    private void reportFailure() {
        Log.i("TetherTPROXY", "Dnsmasq has stopped");
        Intent i = new Intent("com.worstperson.tethertproxy.ForegroundService.CHECK");
        i.setPackage("com.worstperson.tethertproxy");
        i.putExtra("name", "DnsmasqService");
        sendBroadcast(i);
    }

    private Runnable DnsmasqRunnable = new Runnable() {
        @Override
        public void run() {
            SharedPreferences sharedPref = getSharedPreferences("Settings", Context.MODE_PRIVATE);
            Log.i("TetherTPROXY", "Dnsmasq has started");
            String ipv6Prefix = sharedPref.getBoolean("ipv6Default", false) ? "2001:db8::" : "fd00::";
            String ipv4Addr = sharedPref.getString("ipv4Addr", "192.168.42.129");
            String ipv4Prefix = ipv4Addr.substring(0, ipv4Addr.lastIndexOf("."));
            Shell.cmd(getFilesDir().getPath() + "/dnsmasq." + Build.SUPPORTED_ABIS[0] + " --keep-in-foreground --no-resolv --no-poll --domain-needed --bogus-priv --dhcp-authoritative --port=5353 --dhcp-alternate-port=6767,68 --dhcp-range=" + ipv4Prefix + ".10," + ipv4Prefix + ".99,1h --dhcp-range=" + ipv6Prefix + "10," + ipv6Prefix + "99,slaac,64,1h --dhcp-option=option:dns-server," + ipv4Addr + " --dhcp-option=option6:dns-server,[2001:4860:4860::8888],[2001:4860:4860::8844] --server=8.8.8.8 --server=8.8.4.4 --listen-mark 0xf0063 --dhcp-leasefile=" + getFilesDir().getPath() + "/dnsmasq.leases --pid-file=" + getFilesDir().getPath() + "/dnsmasq.pid").submit(result -> reportFailure());
        }
    };

    private final BroadcastReceiver RestartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.postDelayed(DnsmasqRunnable, 5000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.postDelayed(DnsmasqRunnable, 1000);
        registerReceiver(RestartReceiver, new IntentFilter("com.worstperson.tethertproxy.DnsmasqService.START"));
        startForeground(1, notification.build());
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(RestartReceiver);
        } catch (IllegalArgumentException ignored) {}

        if (!HandlerCompat.hasCallbacks(handler, DnsmasqRunnable)) {
            handler.removeCallbacks(DnsmasqRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
