package com.qundev.fakelocation;

import android.location.Location;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * FakeLocation — hooks Location via XposedBridge.hookAllMethods().
 * Config: /data/adb/fakelocation.conf (format: "lat,lng")
 */
public class LocationHook implements IXposedHookLoadPackage {
    private static final String TAG = "FakeLocation";
    private static final File CONFIG_FILE = new File("/data/adb/fakelocation.conf");
    private double mFakeLat = 10.8231, mFakeLng = 106.6297;
    private boolean mEnabled = true;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        loadConfig();
        if (!mEnabled) return;
        log("+++ loaded in " + lpparam.processName + " -> (" + mFakeLat + "," + mFakeLng + ")");

        try {
            XposedBridge.hookAllMethods(Location.class, "getLatitude", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    Location loc = (Location) p.thisObject;
                    if (shouldFake(loc)) p.setResult(mFakeLat);
                }
            });
            log("getLatitude() hooked");
        } catch (Throwable t) { log("getLatitude FAILED: " + t.getMessage()); }

        try {
            XposedBridge.hookAllMethods(Location.class, "getLongitude", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    Location loc = (Location) p.thisObject;
                    if (shouldFake(loc)) p.setResult(mFakeLng);
                }
            });
            log("getLongitude() hooked");
        } catch (Throwable t) { log("getLongitude FAILED: " + t.getMessage()); }

        try {
            XposedBridge.hookAllMethods(Location.class, "isFromMockProvider", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) { p.setResult(false); }
            });
            log("isFromMockProvider() hooked");
        } catch (Throwable t) { log("isFromMockProvider FAILED: " + t.getMessage()); }
    }

    private boolean shouldFake(Location l) {
        if (l == null) return false;
        String p = l.getProvider();
        return p != null && (p.equals("fused") || p.equals("gps") || p.equals("network"));
    }

    private void loadConfig() {
        try {
            if (!CONFIG_FILE.exists()) return;
            BufferedReader r = new BufferedReader(new FileReader(CONFIG_FILE));
            String line = r.readLine(); r.close();
            if (line != null) {
                String[] parts = line.trim().split(",");
                if (parts.length >= 2) {
                    mFakeLat = Double.parseDouble(parts[0].trim());
                    mFakeLng = Double.parseDouble(parts[1].trim());
                }
            }
        } catch (Throwable ignored) {}
    }

    private void log(String msg) {
        Log.e(TAG, msg);
        XposedBridge.log(TAG + ": " + msg);
    }
}
