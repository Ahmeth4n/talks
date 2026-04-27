package com.raconf.lab01;

public class NativeRootDetector {
    static {
        System.loadLibrary("rootdetector");
    }

    public static native boolean isRooted();
    public static native void crashAbort(); // CRASH #2
}