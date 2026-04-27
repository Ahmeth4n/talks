package com.raconf.lab03;

public class NativeFridaDetector {
    static {
        System.loadLibrary("detector");
    }

    public static native boolean isFridaPresent();
}