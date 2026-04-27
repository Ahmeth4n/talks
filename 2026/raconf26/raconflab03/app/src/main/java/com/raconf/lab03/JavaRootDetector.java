package com.raconf.lab03;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class JavaRootDetector {

    private static final String TAG = "RDL_Java";

    private static final String[] ROOT_PATHS = {
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/system/xbin/busybox",
            "/system/sbin/su",
            "/vendor/bin/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/system/xbin/daemonsu",
            "/system/etc/init.d/99SuperSUDaemon",
            "/system/bin/.ext/.su",
            "/system_ext/bin/su",
            "/system_ext/xbin/su",
            "/system_ext/sbin/su",
            "/apex/com.android.runtime/bin/su",
            "/apex/com.android.art/bin/su",
            "/system/bin/qemu-props",
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
    };

    private static final String[] MAGISK_PATHS = {
            "/data/adb/magisk",
            "/data/adb/magisk.db",
            "/data/adb/magisk.img",
            "/data/adb/modules",
            "/data/adb/post-fs-data.d",
            "/data/adb/service.d",
            "/sbin/.magisk",
            "/sbin/.core/mirror",
            "/sbin/.core/img",
    };

    private static final String[] ROOT_PACKAGES = {
            "com.topjohnwu.magisk",
            "com.topjohnwu.magisk.stub",
            "com.topjohnwu.magisk.alpha",
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.koushikdutta.superuser",
            "com.yellowes.su",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "me.phh.superuser",
            "com.alephzain.framaroot",
    };

    public static boolean isRooted(Context ctx) {
        for (String path : ROOT_PATHS) {
            if (new File(path).exists()) {
                Log.w(TAG, "[JAVA][ROOT][PATH] " + path);
                return true;
            }
        }
        for (String path : MAGISK_PATHS) {
            if (new File(path).exists()) {
                Log.w(TAG, "[JAVA][ROOT][MAGISK] " + path);
                return true;
            }
        }
        PackageManager pm = ctx.getPackageManager();
        for (String pkg : ROOT_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0);
                Log.w(TAG, "[JAVA][ROOT][PKG] " + pkg);
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        if (Build.TAGS != null && Build.TAGS.contains("test-keys")) {
            Log.w(TAG, "[JAVA][ROOT][BUILD] test-keys");
            return true;
        }
        if ("1".equals(getProp("ro.debuggable")) ||
                "0".equals(getProp("ro.secure"))) {
            Log.w(TAG, "[JAVA][ROOT][BUILD] ro.debuggable/ro.secure");
            return true;
        }
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"which", "su"});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.destroy();
            if (line != null && !line.isEmpty()) {
                Log.w(TAG, "[JAVA][ROOT][EXEC] which su = " + line);
                return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    public static void crashKillProcess() {
        Log.e(TAG, "[JAVA] CRASH #1 killProcess");
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private static String getProp(String key) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            return (String) c.getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) { return ""; }
    }
}