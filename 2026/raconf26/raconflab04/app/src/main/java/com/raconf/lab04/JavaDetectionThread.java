package com.raconf.lab04;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class JavaDetectionThread extends Thread {

    private static final String TAG = "RDL_JavaThread";

    public interface Callback {
        void onJavaDetectionComplete(boolean rootDetected, boolean fridaDetected);
    }

    private final Context  mContext;
    private final Callback mCallback;

    public JavaDetectionThread(Context context, Callback callback) {
        super("JavaDetectionThread");
        this.mContext  = context.getApplicationContext();
        this.mCallback = callback;
    }

    @Override
    public void run() {
        Log.i(TAG, "[JAVA THREAD] started — tid=" + android.os.Process.myTid());

        boolean javaRoot = JavaRootDetector.isRooted(mContext);
        Log.i(TAG, "[JAVA THREAD] Root  : " + javaRoot);

        boolean javaFrida = JavaFridaDetector.isFridaPresent();
        Log.i(TAG, "[JAVA THREAD] Frida : " + javaFrida);

        Log.i(TAG, "[JAVA THREAD] finished");

        if (mCallback != null) {
            new Handler(Looper.getMainLooper())
                    .post(() -> mCallback.onJavaDetectionComplete(javaRoot, javaFrida));
        }
    }
}