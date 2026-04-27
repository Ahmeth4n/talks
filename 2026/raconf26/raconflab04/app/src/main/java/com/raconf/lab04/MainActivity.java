package com.raconf.lab04;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    private static final String TAG = "RDL";

    private final AtomicInteger mDoneCount   = new AtomicInteger(0);
    private final AtomicBoolean mJavaRoot    = new AtomicBoolean(false);
    private final AtomicBoolean mJavaFrida   = new AtomicBoolean(false);
    private final AtomicBoolean mNativeRoot  = new AtomicBoolean(false);
    private final AtomicBoolean mNativeFrida = new AtomicBoolean(false);

    // Timeout sonrası tekrar evaluate çağrılmasını engelle
    private final AtomicBoolean mEvaluated   = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CrcGuard.initialize();

        // ── Thread 1: Java Detection ──────────────────────
        new JavaDetectionThread(this, (rootDetected, fridaDetected) -> {
            mJavaRoot.set(rootDetected);
            mJavaFrida.set(fridaDetected);
            Log.i(TAG, "[JAVA THREAD] Root=" + rootDetected + " Frida=" + fridaDetected);
            checkIfBothDone();
        }).start();

        // ── Thread 2: Native Detection (C++ pthread) ──────
        NativeDetectionManager.startNativeDetectionThread(
                (rootDetected, fridaDetected) -> {
                    mNativeRoot.set(rootDetected);
                    mNativeFrida.set(fridaDetected);
                    Log.i(TAG, "[NATIVE THREAD] Root=" + rootDetected + " Frida=" + fridaDetected);
                    checkIfBothDone();
                }
        );

        // ── Timeout: 3 saniye içinde her ikisi bitmezse ───
        // Bir thread Frida ile engellenirse diğerinin sonuçlarıyla devam et
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (mDoneCount.get() < 2) {
                Log.e(TAG, "=== TIMEOUT — bir thread tamamlanamadı, mevcut sonuçlarla devam ===");
                evaluate();
            }
        }, 3000);
    }

    private void checkIfBothDone() {
        if (mDoneCount.incrementAndGet() == 2) {
            evaluate();
        }
    }

    private void evaluate() {

        if (!mEvaluated.compareAndSet(false, true)) return;

        boolean rootDetected  = mJavaRoot.get()  || mNativeRoot.get();
        boolean fridaDetected = mJavaFrida.get() || mNativeFrida.get();
        boolean violation     = rootDetected || fridaDetected;

        Log.i(TAG, "=== TARAMA TAMAMLANDI ===");
        Log.i(TAG, "Java  Root   : " + mJavaRoot.get());
        Log.i(TAG, "Java  Frida  : " + mJavaFrida.get());
        Log.i(TAG, "Native Root  : " + mNativeRoot.get());
        Log.i(TAG, "Native Frida : " + mNativeFrida.get());
        Log.i(TAG, "Root Detected  : " + rootDetected);
        Log.i(TAG, "Frida Detected : " + fridaDetected);

        if (violation) {
            Log.e(TAG, "=== VIOLATION DETECTED ===");
            if (rootDetected)  Log.e(TAG, "REASON: ROOT");
            if (fridaDetected) Log.e(TAG, "REASON: FRIDA");
            Log.e(TAG, "=== CRASHING NOW ===");
            new Handler(Looper.getMainLooper()).post(
                    () -> triggerCrashes(mNativeRoot.get() || mNativeFrida.get())
            );
        } else {
            Log.i(TAG, "=== CLEAN — no violation ===");
        }
    }

    private void triggerCrashes(boolean nativeDetected) {
        if (nativeDetected) {
            NativeRootDetector.crashAbort();
        }
        JavaRootDetector.crashKillProcess();
    }
}