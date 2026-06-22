package com.qundev.devicefaker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.security.SecureRandom;

/**
 * Generates + persists the per-device identifiers (IMEI/ANDROID_ID/serial/IMSI/
 * ICCID/phone/MAC) the hook applies, in this app's prefs read cross-app via
 * XSharedPreferences. Profile (Pixel 6) is fixed in DeviceHook.
 * Launch: adb shell am start -n com.qundev.devicefaker/.ConfigActivity
 */
public class ConfigActivity extends Activity {
    static final String PREFS = "config";
    private static final SecureRandom RND = new SecureRandom();
    private TextView mInfo;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(48, 48, 48, 48);

        TextView t = new TextView(this);
        t.setText("DeviceFaker — Pixel 6 (oriole)"); t.setTextSize(20);
        l.addView(t);

        mInfo = new TextView(this);
        mInfo.setPadding(0, 24, 0, 24);
        mInfo.setTextIsSelectable(true);
        l.addView(mInfo);

        Button rnd = new Button(this);
        rnd.setText("Randomize identifiers");
        rnd.setOnClickListener(v -> { generate(); save(); show(); toast("New identifiers saved. Force-stop target app."); });
        l.addView(rnd);

        Button toggle = new Button(this);
        toggle.setText("Toggle enable/disable");
        toggle.setOnClickListener(v -> {
            SharedPreferences p = prefs();
            boolean on = !"off".equalsIgnoreCase(p.getString("enabled", "on"));
            p.edit().putString("enabled", on ? "off" : "on").commit();
            chmod(); show(); toast("enabled=" + (on ? "off" : "on") + ". Force-stop target app.");
        });
        l.addView(toggle);

        ScrollView sv = new ScrollView(this);
        sv.addView(l);
        setContentView(sv);

        // first run: generate if missing
        if (prefs().getString("imei", null) == null) { generate(); save(); }
        show();
    }

    @SuppressWarnings("deprecation")
    private SharedPreferences prefs() { return getSharedPreferences(PREFS, MODE_PRIVATE); }

    private void generate() {
        SharedPreferences.Editor e = prefs().edit();
        e.putString("imei", randomImei());
        e.putString("android_id", hex(16));
        e.putString("serial", alnum(12).toUpperCase());
        // IMSI = MCC(452)+MNC(04 Viettel VN)+10 digits
        e.putString("imsi", "45204" + digits(10));
        // ICCID = 8984 (VN) + 15 digits
        e.putString("iccid", "8984" + digits(15));
        e.putString("phone", "+849" + digits(8));
        e.putString("mac", randomMac());
        e.commit();
    }

    private void save() { chmod(); }

    /** prefs already committed in generate(); just make the file world-readable. */
    private void chmod() {
        // ensure file exists
        prefs().edit().putLong("_ts", System.currentTimeMillis()).commit();
        try {
            Process su = Runtime.getRuntime().exec("su");
            OutputStream os = su.getOutputStream();
            os.write("chmod 644 /data/misc/apexdata/*/prefs/com.qundev.devicefaker/config.xml 2>/dev/null\n".getBytes());
            os.write("exit\n".getBytes()); os.flush(); su.waitFor(); os.close();
        } catch (Exception ignored) {}
    }

    private void show() {
        SharedPreferences p = prefs();
        mInfo.setText(
                "enabled: " + p.getString("enabled", "on") + "\n\n" +
                "MODEL: Pixel 6 (oriole) / Android 13\n" +
                "IMEI: " + p.getString("imei", "-") + "\n" +
                "ANDROID_ID: " + p.getString("android_id", "-") + "\n" +
                "SERIAL: " + p.getString("serial", "-") + "\n" +
                "IMSI: " + p.getString("imsi", "-") + "\n" +
                "ICCID: " + p.getString("iccid", "-") + "\n" +
                "PHONE: " + p.getString("phone", "-") + "\n" +
                "MAC: " + p.getString("mac", "-"));
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    // ---- generators ----
    private static String digits(int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) s.append(RND.nextInt(10));
        return s.toString();
    }
    private static String hex(int n) {
        final String h = "0123456789abcdef";
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) s.append(h.charAt(RND.nextInt(16)));
        return s.toString();
    }
    private static String alnum(int n) {
        final String a = "0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) s.append(a.charAt(RND.nextInt(a.length())));
        return s.toString();
    }
    /** 14 random digits + Luhn check digit = valid 15-digit IMEI. */
    private static String randomImei() {
        int[] d = new int[15];
        for (int i = 0; i < 14; i++) d[i] = RND.nextInt(10);
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int x = d[i];
            if ((i % 2) == 1) { x *= 2; if (x > 9) x -= 9; }
            sum += x;
        }
        d[14] = (10 - (sum % 10)) % 10;
        StringBuilder s = new StringBuilder();
        for (int x : d) s.append(x);
        return s.toString();
    }
    private static String randomMac() {
        int[] o = new int[6];
        for (int i = 0; i < 6; i++) o[i] = RND.nextInt(256);
        o[0] = (o[0] & 0xFC) | 0x02; // locally administered, unicast
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 6; i++) { if (i > 0) s.append(':'); s.append(String.format("%02x", o[i])); }
        return s.toString();
    }
}
