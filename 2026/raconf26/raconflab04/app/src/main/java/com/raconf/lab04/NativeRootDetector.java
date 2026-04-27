package com.raconf.lab04;

public class NativeRootDetector {
    static {
        System.loadLibrary("detector");
    }

    public static native boolean isRooted();
    public static native void crashAbort();
}