package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Calls back once a minute (the system's own ACTION_TIME_TICK, so nothing polls) and when the clock, the date or
 * the time zone changes. Views that draw the time or the date themselves start it when attached to a window.
 */
final class MinuteTicker {
    private final Runnable onTick;
    private boolean running;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onTick.run();
        }
    };

    MinuteTicker(Runnable onTick) {
        this.onTick = onTick;
    }

    void start(Context ctx) {
        if (running) return;
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        f.addAction(Intent.ACTION_DATE_CHANGED);
        ctx.getApplicationContext().registerReceiver(receiver, f);
        running = true;
        onTick.run();
    }

    void stop(Context ctx) {
        if (!running) return;
        running = false;
        try {
            ctx.getApplicationContext().unregisterReceiver(receiver);
        } catch (IllegalArgumentException ignored) {
            // Already gone.
        }
    }
}
