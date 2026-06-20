package com.qundev.fakelocation;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.*;

/** Config UI: writes to /data/adb/fakelocation.conf via su */
public class ConfigActivity extends Activity {
    private static final File CONFIG = new File("/data/adb/fakelocation.conf");
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

        try {
            if (CONFIG.exists()) {
                BufferedReader r = new BufferedReader(new FileReader(CONFIG));
                String line = r.readLine(); r.close();
                if (line != null) {
                    String[] parts = line.trim().split(",");
                    if (parts.length >= 2) { mLat.setText(parts[0].trim()); mLng.setText(parts[1].trim()); return; }
                }
            }
        } catch (Exception ignored) {}
        mLat.setText("10.8231"); mLng.setText("106.6297");
    }

    private void saveConfig() {
        try {
            double lat = Double.parseDouble(mLat.getText().toString());
            double lng = Double.parseDouble(mLng.getText().toString());
            if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                Toast.makeText(this, "Invalid", Toast.LENGTH_SHORT).show(); return;
            }
            Process su = Runtime.getRuntime().exec("su");
            OutputStream os = su.getOutputStream();
            os.write(("echo '" + lat + "," + lng + "' > " + CONFIG.getAbsolutePath() + " && chmod 644 " + CONFIG.getAbsolutePath() + "\n").getBytes());
            os.write("exit\n".getBytes()); os.flush(); su.waitFor(); os.close();
            Toast.makeText(this, "Saved. Force-stop target app.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
