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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.os.HandlerCompat;
import androidx.core.app.NotificationCompat;

import com.topjohnwu.superuser.Shell;

public class TPROXY4Service extends Service {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setContentTitle("Service is running");

    final Handler handler = new Handler(Looper.getMainLooper());

    private void reportFailure() {
        Log.i("TetherTPROXY4", "TPROXY4 has stopped");
        Intent i = new Intent("com.worstperson.tethertproxy4.ForegroundService.CHECK");
        i.setPackage("com.worstperson.tethertproxy");
        i.putExtra("name", "TPROXY4Service");
        sendBroadcast(i);
    }

    private Runnable TPROXY4Runnable = new Runnable() {
        @Override
        public void run() {
            Log.i("TetherTPROXY4", "TPROXY4 has started");
            Shell.cmd(getFilesDir().getPath() + "/hev-socks5-tproxy." + Build.SUPPORTED_ABIS[0] + " " + getFilesDir().getPath() + "/tproxy4.yml").submit(result -> reportFailure());
        }
    };

    private final BroadcastReceiver RestartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handler.postDelayed(TPROXY4Runnable, 5000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @SuppressLint("WakelockTimeout")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.postDelayed(TPROXY4Runnable, 1000);
        registerReceiver(RestartReceiver, new IntentFilter("com.worstperson.tethertproxy.TPROXY4Service.START"));
        startForeground(1, notification.build());
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(RestartReceiver);
        } catch (IllegalArgumentException ignored) {}

        if (!HandlerCompat.hasCallbacks(handler, TPROXY4Runnable)) {
            handler.removeCallbacks(TPROXY4Runnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
