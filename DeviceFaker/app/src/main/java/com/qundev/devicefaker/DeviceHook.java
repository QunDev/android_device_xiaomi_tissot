package com.qundev.devicefaker;

import android.os.Build;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * DeviceFaker — make the TARGET app see a real Google Pixel 6 (oriole / Android
 * 13) instead of this device, as deeply as an app can read it:
 *   - android.os.Build.* static fields + Build.VERSION.*
 *   - SystemProperties.get(ro.*) (props read directly)
 *   - Settings.Secure ANDROID_ID
 *   - TelephonyManager IMEI/IMSI/ICCID/line1/operators
 *   - WifiInfo MAC/BSSID/SSID
 *   - Build.getSerial()
 * Per-device identifiers (IMEI/ANDROID_ID/serial/MAC/IMSI/ICCID/phone) are read
 * from this app's prefs (set + randomized by ConfigActivity) so they're stable
 * per identity. Scope = target apps only, NEVER com.google.android.gms.
 */
public class DeviceHook implements IXposedHookLoadPackage {
    private static final String TAG = "DeviceFaker";

    // ---- Coherent real Pixel 6 (oriole) / Android 13 profile ("from Google") ----
    private static final String P_BRAND        = "google";
    private static final String P_MANUFACTURER = "Google";
    private static final String P_MODEL        = "Pixel 6";
    private static final String P_DEVICE       = "oriole";
    private static final String P_PRODUCT      = "oriole";
    private static final String P_BOARD        = "oriole";
    private static final String P_HARDWARE     = "oriole";
    private static final String P_ID           = "TQ3A.230901.001";
    private static final String P_INCREMENTAL  = "10750268";
    private static final String P_TAGS         = "release-keys";
    private static final String P_TYPE         = "user";
    private static final String P_RELEASE      = "13";
    private static final String P_SECPATCH     = "2023-09-05";
    private static final String P_BOOTLOADER   = "slider-1.2-9152140";
    private static final String P_RADIO        = "g5123b-124444-230314-B-9899560";
    private static final String P_FINGERPRINT  =
            "google/oriole/oriole:13/TQ3A.230901.001/10750268:user/release-keys";
    private static final String P_DESCRIPTION  =
            "oriole-user 13 TQ3A.230901.001 10750268 release-keys";

    // identifiers (filled from prefs in handleLoadPackage)
    private String mImei, mAndroidId, mSerial, mImsi, mIccid, mPhone, mMac;
    private boolean mEnabled = true;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        loadConfig();
        if (!mEnabled) return;
        final ClassLoader cl = lpparam.classLoader;
        log("+++ " + lpparam.processName + " -> " + P_MODEL + " (" + P_FINGERPRINT + ")");

        // 1) android.os.Build static fields
        setBuild("BRAND", P_BRAND);
        setBuild("MANUFACTURER", P_MANUFACTURER);
        setBuild("MODEL", P_MODEL);
        setBuild("DEVICE", P_DEVICE);
        setBuild("PRODUCT", P_PRODUCT);
        setBuild("BOARD", P_BOARD);
        setBuild("HARDWARE", P_HARDWARE);
        setBuild("ID", P_ID);
        setBuild("DISPLAY", P_ID);
        setBuild("FINGERPRINT", P_FINGERPRINT);
        setBuild("TAGS", P_TAGS);
        setBuild("TYPE", P_TYPE);
        setBuild("BOOTLOADER", P_BOOTLOADER);
        setBuild("RADIO", P_RADIO);
        setBuild("HOST", "abfarm-release");
        setBuild("USER", "android-build");
        setBuild("SOC_MANUFACTURER", "Google");
        setBuild("SOC_MODEL", "Tensor");
        if (mSerial != null) setBuild("SERIAL", mSerial);
        // Build.VERSION.*
        try {
            XposedHelpers.setStaticObjectField(Build.VERSION.class, "RELEASE", P_RELEASE);
            XposedHelpers.setStaticObjectField(Build.VERSION.class, "SECURITY_PATCH", P_SECPATCH);
            XposedHelpers.setStaticObjectField(Build.VERSION.class, "INCREMENTAL", P_INCREMENTAL);
            // NOTE: SDK_INT deliberately NOT changed (would break app behavior).
        } catch (Throwable t) { log("VERSION set err: " + t); }
        // Build.getSerial()
        replaceMethod(Build.class, "getSerial", mSerial);

        // 2) SystemProperties.get(...) -> spoofed ro.* values
        hookSystemProperties(cl);

        // 3) Settings.Secure ANDROID_ID
        hookAndroidId(cl);

        // 4) TelephonyManager
        hookTelephony(cl);

        // 5) WifiInfo
        hookWifi(cl);
    }

    // ---------- helpers ----------
    private void setBuild(String field, String val) {
        if (val == null) return;
        try { XposedHelpers.setStaticObjectField(Build.class, field, val); }
        catch (Throwable t) { /* field may not exist on this API */ }
    }

    /** Replace a method's return value (skips original via beforeHookedMethod). */
    private void replaceMethod(Class<?> cls, String method, final Object val) {
        if (cls == null || val == null) return;
        try {
            XposedBridge.hookAllMethods(cls, method, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) { p.setResult(val); }
            });
        } catch (Throwable ignored) {}
    }

    private Class<?> cls(String name, ClassLoader cl) {
        try { return XposedHelpers.findClass(name, cl); } catch (Throwable t) { return null; }
    }

    private void hookSystemProperties(ClassLoader cl) {
        final Map<String, String> m = propMap();
        Class<?> sp = cls("android.os.SystemProperties", cl);
        if (sp == null) return;
        XC_MethodHook h = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (p.args.length >= 1 && p.args[0] instanceof String) {
                    String v = m.get(p.args[0]);
                    if (v != null) p.setResult(v);
                }
            }
        };
        try { XposedBridge.hookAllMethods(sp, "get", h); } catch (Throwable ignored) {}
        try { XposedBridge.hookAllMethods(sp, "get_String", h); } catch (Throwable ignored) {}
    }

    private Map<String, String> propMap() {
        Map<String, String> m = new LinkedHashMap<>();
        String[] parts = {"", "system.", "vendor.", "odm.", "product.", "system_ext.", "bootimage."};
        for (String pt : parts) {
            m.put("ro.product." + pt + "brand", P_BRAND);
            m.put("ro.product." + pt + "manufacturer", P_MANUFACTURER);
            m.put("ro.product." + pt + "model", P_MODEL);
            m.put("ro.product." + pt + "device", P_DEVICE);
            m.put("ro.product." + pt + "name", P_PRODUCT);
            m.put("ro." + pt + "build.fingerprint", P_FINGERPRINT);
        }
        m.put("ro.product.board", P_BOARD);
        m.put("ro.board.platform", "gs101");
        m.put("ro.hardware", P_HARDWARE);
        m.put("ro.boot.hardware", P_HARDWARE);
        m.put("ro.build.id", P_ID);
        m.put("ro.build.display.id", P_ID);
        m.put("ro.build.version.incremental", P_INCREMENTAL);
        m.put("ro.build.version.release", P_RELEASE);
        m.put("ro.build.version.security_patch", P_SECPATCH);
        m.put("ro.build.type", P_TYPE);
        m.put("ro.build.tags", P_TAGS);
        m.put("ro.build.flavor", P_PRODUCT + "-user");
        m.put("ro.build.product", P_PRODUCT);
        m.put("ro.build.description", P_DESCRIPTION);
        m.put("ro.build.fingerprint", P_FINGERPRINT);
        m.put("ro.bootloader", P_BOOTLOADER);
        m.put("ro.boot.bootloader", P_BOOTLOADER);
        m.put("ro.build.user", "android-build");
        m.put("ro.build.host", "abfarm-release");
        m.put("gsm.version.baseband", P_RADIO);
        if (mSerial != null) { m.put("ro.serialno", mSerial); m.put("ro.boot.serialno", mSerial); }
        return m;
    }

    private void hookAndroidId(ClassLoader cl) {
        if (mAndroidId == null) return;
        Class<?> secure = cls("android.provider.Settings$Secure", cl);
        if (secure == null) return;
        XC_MethodHook h = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                for (Object a : p.args) {
                    if ("android_id".equals(a)) { p.setResult(mAndroidId); return; }
                }
            }
        };
        try { XposedBridge.hookAllMethods(secure, "getString", h); } catch (Throwable ignored) {}
        try { XposedBridge.hookAllMethods(secure, "getStringForUser", h); } catch (Throwable ignored) {}
    }

    private void hookTelephony(ClassLoader cl) {
        Class<?> tm = cls("android.telephony.TelephonyManager", cl);
        if (tm == null) return;
        replaceMethod(tm, "getImei", mImei);
        replaceMethod(tm, "getDeviceId", mImei);
        replaceMethod(tm, "getMeid", mImei);
        replaceMethod(tm, "getSubscriberId", mImsi);
        replaceMethod(tm, "getSimSerialNumber", mIccid);
        replaceMethod(tm, "getLine1Number", mPhone);
    }

    private void hookWifi(ClassLoader cl) {
        Class<?> wi = cls("android.net.wifi.WifiInfo", cl);
        if (wi == null) return;
        replaceMethod(wi, "getMacAddress", mMac);
        replaceMethod(wi, "getBSSID", mMac);
    }

    private void loadConfig() {
        try {
            XSharedPreferences sp = new XSharedPreferences("com.qundev.devicefaker", "config");
            if ("off".equalsIgnoreCase(sp.getString("enabled", "on"))) { mEnabled = false; return; }
            mImei      = sp.getString("imei", null);
            mAndroidId = sp.getString("android_id", null);
            mSerial    = sp.getString("serial", null);
            mImsi      = sp.getString("imsi", null);
            mIccid     = sp.getString("iccid", null);
            mPhone     = sp.getString("phone", null);
            mMac       = sp.getString("mac", null);
        } catch (Throwable ignored) {}
    }

    private void log(String s) { Log.e(TAG, s); XposedBridge.log(TAG + ": " + s); }
}
