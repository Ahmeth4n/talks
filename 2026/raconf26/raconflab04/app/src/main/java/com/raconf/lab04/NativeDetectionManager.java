package com.raconf.lab04;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class NativeDetectionManager {

    private static final String TAG = "RDL_NativeMgr";

    static {
        System.loadLibrary("detector");
    }

    public interface Callback {
        void onNativeDetectionComplete(boolean rootDetected, boolean fridaDetected);
    }

    private static Callback sPendingCallback;

    public static void startNativeDetectionThread(Callback callback) {
        sPendingCallback = callback;
        Log.i(TAG, "[NativeMgr] starting native detection thread...");
        ClassLoader cl = NativeDetectionManager.class.getClassLoader();
        nativeStartDetectionThread(cl);
    }

    public static void nativeDetectionCallback(boolean rootDetected, boolean fridaDetected) {
        Log.i(TAG, "[NativeMgr] callback received root=" + rootDetected
                + " frida=" + fridaDetected);
        if (sPendingCallback != null) {
            Callback cb = sPendingCallback;
            sPendingCallback = null;
            new Handler(Looper.getMainLooper())
                    .post(() -> cb.onNativeDetectionComplete(rootDetected, fridaDetected));
        }
    }

    private static native void nativeStartDetectionThread(ClassLoader classLoader);
}