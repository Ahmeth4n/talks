package com.raconf.lab03;

import android.util.Log;

public class CrcGuard {

    private static final String TAG = "RDL_CRC";

    static {
        System.loadLibrary("detector");
    }

    public static native void    init();
    public static native boolean verifyIsRooted();

    private static boolean sInitialized = false;

    public static void initialize() {
        if (sInitialized) return;
        init();
        sInitialized = true;
        Log.i(TAG, "[CRC] CrcGuard initialized");
    }

    public static void checkBeforeIsRooted() {
        Log.i(TAG, "[CRC] Checking before isRooted...");
        boolean ok = verifyIsRooted();
        if (!ok) {
            Log.e(TAG, "[CRC] Native check failed — emergency exit");
            System.exit(2);
        }
        Log.i(TAG, "[CRC] isRooted check passed");
    }
}