package com.stfalcon.unlocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by anton on 8/2/13.
 */
public class LockReceiver extends BroadcastReceiver {
    private boolean screenOff;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screenOff = true;
            Log.v("LOGER", "OFF SCREEN");
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            screenOff = false;
            Log.v("LOGER", "ON SCREEN");
        }
        Log.v("LOGER", intent.getAction());
        Intent i = new Intent(context, LockService.class);
        i.putExtra("screen_state", screenOff);
        context.startService(i);
    }
}
