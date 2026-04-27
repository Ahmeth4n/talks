package com.raconf.lab02;

public class NativeFridaDetector {
    static {
        System.loadLibrary("detector");
    }

    public static native boolean isFridaPresent();
}