package com.raconf.lab03;

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

        CrcGuard.initialize();

        new Thread(() -> {


            boolean javaRoot  = JavaRootDetector.isRooted(MainActivity.this);
            boolean javaFrida = JavaFridaDetector.isFridaPresent();
            boolean nativeRoot  = false;
            boolean nativeFrida = false;
            try {
                // isRooted çağrısından önce CRC kontrolü
                CrcGuard.checkBeforeIsRooted();
                nativeRoot  = NativeRootDetector.isRooted();
                nativeFrida = NativeFridaDetector.isFridaPresent();
            } catch (UnsatisfiedLinkError e) {
                Log.e(TAG, "Native lib error: " + e.getMessage());
            }

            final boolean finalNativeRoot  = nativeRoot;
            final boolean finalNativeFrida = nativeFrida;
            final boolean rootDetected     = javaRoot  || finalNativeRoot;
            final boolean fridaDetected    = javaFrida || finalNativeFrida;
            final boolean violation        = rootDetected || fridaDetected;

            Log.i(TAG, "=== TARAMA TAMAMLANDI ===");
            Log.i(TAG, "Java  Root  : " + javaRoot);
            Log.i(TAG, "Java  Frida : " + javaFrida);
            Log.i(TAG, "Native Root  : " + finalNativeRoot);
            Log.i(TAG, "Native Frida : " + finalNativeFrida);
            Log.i(TAG, "Root Detected  : " + rootDetected);
            Log.i(TAG, "Frida Detected : " + fridaDetected);

            if (violation) {
                Log.e(TAG, "=== VIOLATION DETECTED ===");
                if (rootDetected)  Log.e(TAG, "REASON: ROOT");
                if (fridaDetected) Log.e(TAG, "REASON: FRIDA");
                Log.e(TAG, "=== CRASHING NOW ===");
                new Handler(Looper.getMainLooper()).post(
                        () -> triggerCrashes(finalNativeRoot || finalNativeFrida)
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