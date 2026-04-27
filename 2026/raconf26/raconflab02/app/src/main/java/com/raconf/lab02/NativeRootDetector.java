package com.raconf.lab02;

public class NativeRootDetector {
    static {
        System.loadLibrary("detector");
    }

    public static native boolean isRooted();
    public static native void crashAbort(); // CRASH #2
}