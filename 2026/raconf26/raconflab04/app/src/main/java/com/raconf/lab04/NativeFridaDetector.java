package com.raconf.lab04;

public class NativeFridaDetector {
    static {
        System.loadLibrary("detector");
    }

    public static native boolean isFridaPresent();
}