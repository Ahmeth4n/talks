package com.raconf.lab03;

public class NativeRootDetector {
    static {
        System.loadLibrary("detector");
    }

    public static native boolean isRooted();
    public static native void crashAbort(); // CRASH #2
}