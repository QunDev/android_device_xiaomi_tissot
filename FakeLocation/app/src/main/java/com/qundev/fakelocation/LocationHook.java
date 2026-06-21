package com.qundev.fakelocation;

import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * FakeLocation — hooks android.location.Location in the TARGET app's process so
 * every Location it reads (incl. FusedLocationProvider results) reports a fake
 * fix, with NO mock flag and a coherent accuracy/time/altitude.
 *
 * Config: /data/adb/fakelocation.conf  ->  "lat,lng" (or "lat,lng,accuracy,altitude")
 * Scope:  the target app(s) ONLY — never com.google.android.gms.
 */
public class LocationHook implements IXposedHookLoadPackage {
    private static final String TAG = "FakeLocation";
    private static final File CONFIG_FILE = new File("/data/adb/fakelocation.conf");

    private double  mLat = 10.8231, mLng = 106.6297;
    private float   mAccuracy = 12.0f;
    private double  mAltitude = 15.0;
    private boolean mEnabled = true;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        loadConfig();
        if (!mEnabled) return;
        log("+++ " + lpparam.processName + " -> (" + mLat + "," + mLng + ") acc=" + mAccuracy);

        // 1) Fake every Location getter (only for real location providers).
        hookResult("getLatitude",  mLat);
        hookResult("getLongitude", mLng);
        hookResult("getAccuracy",  mAccuracy);
        hookResult("getAltitude",  mAltitude);
        hookResult("hasAltitude",  Boolean.TRUE);
        hookResult("hasAccuracy",  Boolean.TRUE);
        hookResult("getSpeed",     0.0f);
        hookResult("getBearing",   0.0f);
        // freshen the timestamps so the fix never looks stale
        hookDynamic("getTime", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (faking(p)) p.setResult(System.currentTimeMillis());
            }
        });
        hookDynamic("getElapsedRealtimeNanos", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (faking(p)) p.setResult(SystemClock.elapsedRealtimeNanos());
            }
        });
        // hide the mock flags (API 18 isFromMockProvider, API 31 isMock)
        hookResult("isFromMockProvider", Boolean.FALSE);
        hookResult("isMock",             Boolean.FALSE);
        // accuracy fields (API 26+) — harmless if absent on older API
        hookResult("getVerticalAccuracyMeters",        8.0f);
        hookResult("getSpeedAccuracyMetersPerSecond",  0.0f);
        hookResult("getBearingAccuracyDegrees",        0.0f);

        // 2) getLastKnownLocation() -> return a fully fabricated Location.
        try {
            XposedBridge.hookAllMethods(LocationManager.class, "getLastKnownLocation",
                new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        String prov = (p.args.length > 0 && p.args[0] != null)
                                ? String.valueOf(p.args[0]) : "fused";
                        p.setResult(buildFake(prov));
                    }
                });
            log("getLastKnownLocation hooked");
        } catch (Throwable t) { log("getLastKnownLocation skip: " + t.getMessage()); }
    }

    /** Hook a Location getter to always return a constant (when faking). */
    private void hookResult(String name, final Object value) {
        hookDynamic(name, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (faking(p)) p.setResult(value);
            }
        });
    }

    private void hookDynamic(String name, XC_MethodHook cb) {
        try {
            XposedBridge.hookAllMethods(Location.class, name, cb);
            log(name + "() hooked");
        } catch (Throwable t) {
            log(name + "() skip: " + t.getMessage());
        }
    }

    /** Only fake Locations that come from a real location provider. */
    private boolean faking(XC_MethodHook.MethodHookParam p) {
        if (!(p.thisObject instanceof Location)) return false;
        String prov = ((Location) p.thisObject).getProvider();
        return prov == null
                || prov.equals("fused") || prov.equals("gps")
                || prov.equals("network") || prov.equals("passive");
    }

    private Location buildFake(String provider) {
        Location l = new Location(provider == null ? "fused" : provider);
        l.setLatitude(mLat);
        l.setLongitude(mLng);
        l.setAccuracy(mAccuracy);
        l.setAltitude(mAltitude);
        l.setTime(System.currentTimeMillis());
        try { l.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos()); } catch (Throwable ignored) {}
        return l;
    }

    private void loadConfig() {
        try {
            if (!CONFIG_FILE.exists()) return;
            BufferedReader r = new BufferedReader(new FileReader(CONFIG_FILE));
            String line = r.readLine(); r.close();
            if (line == null) return;
            line = line.trim();
            if (line.equalsIgnoreCase("off") || line.equalsIgnoreCase("disabled")) {
                mEnabled = false; return;
            }
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                mLat = Double.parseDouble(parts[0].trim());
                mLng = Double.parseDouble(parts[1].trim());
            }
            if (parts.length >= 3) mAccuracy = Float.parseFloat(parts[2].trim());
            if (parts.length >= 4) mAltitude = Double.parseDouble(parts[3].trim());
        } catch (Throwable ignored) {}
    }

    private void log(String msg) {
        Log.e(TAG, msg);
        XposedBridge.log(TAG + ": " + msg);
    }
}
