package com.raconf.lab01;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "RDL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(() -> {
            boolean javaRooted   = JavaRootDetector.isRooted(MainActivity.this);
            boolean nativeRooted = false;
            try {
                nativeRooted = NativeRootDetector.isRooted();
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "Native lib error: " + e.getMessage());
            }

            final boolean finalNativeRooted = nativeRooted;
            final boolean violation         = javaRooted || finalNativeRooted;

            Log.i(TAG, "=== TARAMA TAMAMLANDI ===");
            Log.i(TAG, "Java  Root  : " + javaRooted);
            Log.i(TAG, "Native Root  : " + finalNativeRooted);
            Log.i(TAG, "Root Detected  : " + violation);

            if (violation) {
                Log.e(TAG, "=== VIOLATION DETECTED ===");
                Log.e(TAG, "REASON: ROOT");
                Log.e(TAG, "=== CRASHING NOW ===");
                new Handler(Looper.getMainLooper()).post(
                        () -> triggerCrashes(finalNativeRooted)
                );
            } else {
                Log.i(TAG, "=== CLEAN — no violation ===");
            }
        }).start();
    }

    private void triggerCrashes(boolean nativeDetected) {
        if (nativeDetected) {
            NativeRootDetector.crashAbort();
        }
        JavaRootDetector.crashKillProcess();
    }
}