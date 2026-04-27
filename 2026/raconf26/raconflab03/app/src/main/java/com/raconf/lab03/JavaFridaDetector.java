package com.raconf.lab03;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JavaFridaDetector {

    private static final String TAG = "RDL_Frida";

    // ── 1. PATH-BASED (/data/local/tmp) ───────────────────
    private static final String[] FRIDA_FILES = {
            "frida-server",
            "frida-server-arm64",
            "frida-server-x86_64",
            "frida-agent",
            "frida-inject",
            "frida-gadget",
            "frida",
            "re.frida.server",
    };

    private static boolean checkFridaFiles() {
        for (String name : FRIDA_FILES) {
            File f = new File("/data/local/tmp/" + name);
            if (f.exists()) {
                Log.w(TAG, "[JAVA][FRIDA][PATH] found: " + f.getAbsolutePath());
                return true;
            }
        }
        File tmpDir = new File("/data/local/tmp");
        if (tmpDir.exists() && tmpDir.isDirectory()) {
            String[] files = tmpDir.list();
            if (files != null) {
                for (String name : files) {
                    if (name.startsWith("frida")) {
                        Log.w(TAG, "[JAVA][FRIDA][PATH] /data/local/tmp/" + name);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── 2. PORT-BASED ─────────────────────────────────────
    // 27042 decimal = 0x69A2 hex
    private static boolean checkFridaPort() {
        String[] procFiles = { "/proc/net/tcp", "/proc/net/tcp6" };
        for (String path : procFiles) {
            File f = new File(path);
            if (!f.exists()) continue;
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(":69A2") || line.contains(":69a2")) {
                        Log.w(TAG, "[JAVA][FRIDA][PORT] port 27042 found in " + path);
                        return true;
                    }
                }
            } catch (IOException e) {
                Log.w(TAG, "[JAVA][FRIDA][PORT] direct read failed: " + e.getMessage());
            }
        }
        // su fallback
        String[] suCmds = {
                "su -c cat /proc/net/tcp",
                "su -c cat /proc/net/tcp6",
        };
        for (String cmd : suCmds) {
            try {
                Process p = Runtime.getRuntime().exec(cmd.split(" "));
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains(":69A2") || line.contains(":69a2")) {
                        Log.w(TAG, "[JAVA][FRIDA][PORT] port 27042 found via su");
                        p.destroy();
                        return true;
                    }
                }
                p.destroy();
            } catch (IOException e) {
                Log.w(TAG, "[JAVA][FRIDA][PORT] su exec failed: " + e.getMessage());
            }
        }
        return false;
    }

    // ── 3. MAPS-BASED (/proc/self/maps) ───────────────────
    private static boolean checkFridaMaps() {
        File maps = new File("/proc/self/maps");
        if (!maps.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(maps))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("frida")        ||
                        line.contains("gum-js-loop")  ||
                        line.contains("frida-agent")  ||
                        line.contains("frida-gadget")) {
                    Log.w(TAG, "[JAVA][FRIDA][MAPS] found: " + line.trim());
                    return true;
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "[JAVA][FRIDA][MAPS] read error: " + e.getMessage());
        }
        return false;
    }

    public static boolean isFridaPresent() {
        if (checkFridaFiles()) return true;
        if (checkFridaPort())  return true;
        if (checkFridaMaps())  return true;
        return false;
    }
}