package com.qundev.fakelocation;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Config UI. Stores lat/lng in this app's SharedPreferences ("config"); the hook
 * reads them via XSharedPreferences (works across SELinux because LSPosed bridges
 * it — a /data/adb file is NOT readable by untrusted_app like Maps).
 */
public class ConfigActivity extends Activity {
    static final String PREFS = "config";
    static final String KEY = "loc"; // "lat,lng[,accuracy[,altitude]]"
    private EditText mLat, mLng;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(48, 48, 48, 48);

        TextView t = new TextView(this);
        t.setText("FakeLocation Config"); t.setTextSize(20);
        l.addView(t);

        TextView la = new TextView(this);
        la.setText("Latitude:"); la.setPadding(0, 24, 0, 4);
        l.addView(la);
        mLat = new EditText(this);
        mLat.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        l.addView(mLat);

        TextView lo = new TextView(this);
        lo.setText("Longitude:"); lo.setPadding(0, 16, 0, 4);
        l.addView(lo);
        mLng = new EditText(this);
        mLng.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        l.addView(mLng);

        Button save = new Button(this);
        save.setText("Save & Apply"); save.setPadding(0, 24, 0, 0);
        save.setOnClickListener(v -> saveConfig());
        l.addView(save);
        setContentView(l);

        // prefill from current prefs
        String cur = prefs().getString(KEY, "10.8231,106.6297");
        String[] p = cur.split(",");
        if (p.length >= 2) { mLat.setText(p[0].trim()); mLng.setText(p[1].trim()); }
    }

    @SuppressWarnings("deprecation")
    private SharedPreferences prefs() {
        // MODE_PRIVATE + the xposedsharedprefs meta-data => LSPosed stores it
        // world-readable so the hook's XSharedPreferences can read it.
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void saveConfig() {
        try {
            double lat = Double.parseDouble(mLat.getText().toString());
            double lng = Double.parseDouble(mLng.getText().toString());
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                Toast.makeText(this, "Invalid", Toast.LENGTH_SHORT).show(); return;
            }
            prefs().edit().putString(KEY, lat + "," + lng).commit();
            // LSPosed leaves the remote-prefs file 0660, so a hooked app running
            // under a different uid can't read it via XSharedPreferences. Make it
            // world-readable (this app has root via KSU).
            try {
                Process su = Runtime.getRuntime().exec("su");
                java.io.OutputStream os = su.getOutputStream();
                os.write("chmod 644 /data/misc/apexdata/*/prefs/com.qundev.fakelocation/config.xml 2>/dev/null\n".getBytes());
                os.write("exit\n".getBytes()); os.flush(); su.waitFor(); os.close();
            } catch (Exception ignored) {}
            Toast.makeText(this, "Saved (" + lat + "," + lng + "). Force-stop the target app.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
