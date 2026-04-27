package com.raconf.lab04;

import java.util.concurrent.atomic.AtomicBoolean;

public class DetectionResult {

    public final AtomicBoolean javaRoot    = new AtomicBoolean(false);
    public final AtomicBoolean nativeRoot  = new AtomicBoolean(false);
    public final AtomicBoolean javaFrida   = new AtomicBoolean(false);
    public final AtomicBoolean nativeFrida = new AtomicBoolean(false);

    public boolean isViolation()     { return javaRoot.get() || nativeRoot.get() || javaFrida.get() || nativeFrida.get(); }
    public boolean isRootDetected()  { return javaRoot.get() || nativeRoot.get(); }
    public boolean isFridaDetected() { return javaFrida.get() || nativeFrida.get(); }
    public boolean isNativeDetected(){ return nativeRoot.get() || nativeFrida.get(); }
}