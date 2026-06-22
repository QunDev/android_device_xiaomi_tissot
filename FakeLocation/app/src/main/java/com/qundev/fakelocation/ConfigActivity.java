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

        Button rnd = new Button(this);
        rnd.setText("Random by IP (match proxy)");
        rnd.setOnClickListener(v -> randomByIp());
        l.addView(rnd);
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

    /**
     * Look up the current public IP's location and set it (with jitter). Uses
     * DoH (Cloudflare 1.1.1.1) + a direct TLS-by-IP connection so a network that
     * HIJACKS DNS for IP-geo services (e.g. router returning 10.x for ipinfo.io)
     * can't break it. Still respects a full VPN (all traffic incl. 1.1.1.1 is
     * tunneled -> reports the VPN/proxy IP). Falls back to a normal request.
     */
    private void randomByIp() {
        Toast.makeText(this, "Fetching IP location...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                org.json.JSONObject j = new org.json.JSONObject(fetchIpInfo());
                String[] loc = j.optString("loc", "").split(",");
                if (loc.length < 2) throw new Exception("no loc in response");
                final String ip = j.optString("ip", "?");
                final String city = j.optString("city", "?") + ", " + j.optString("country", "?");
                double lat = Double.parseDouble(loc[0]);
                double lng = Double.parseDouble(loc[1]);
                java.util.Random rnd = new java.util.Random();
                double R = 0.025; // ~2.5 km jitter
                lat += (rnd.nextDouble() - 0.5) * 2 * R;
                lng += (rnd.nextDouble() - 0.5) * 2 * R;
                final double flat = Math.round(lat * 1e6) / 1e6;
                final double flng = Math.round(lng * 1e6) / 1e6;
                runOnUiThread(() -> {
                    mLat.setText(String.valueOf(flat));
                    mLng.setText(String.valueOf(flng));
                    Toast.makeText(this, "IP " + ip + " (" + city + ")", Toast.LENGTH_SHORT).show();
                    saveConfig();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "IP lookup failed: " + e, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /** ipinfo.io/json via DoH + TLS-by-IP (bypass DNS hijack); fall back to normal. */
    private String fetchIpInfo() throws Exception {
        String host = "ipinfo.io";
        Exception last;
        try { return httpsGetByIp(dohResolve(host), host, "/json"); }
        catch (Exception e) { last = e; }
        try { // fallback: normal request (works on clean DNS / via VPN)
            java.net.HttpURLConnection c = (java.net.HttpURLConnection)
                    new java.net.URL("https://" + host + "/json").openConnection();
            c.setConnectTimeout(8000); c.setReadTimeout(8000);
            c.setRequestProperty("Accept", "application/json");
            return readAll(c.getInputStream());
        } catch (Exception e) { last = e; }
        throw last;
    }

    /** Resolve A record over Cloudflare DoH — connects to IP 1.1.1.1 so the
     *  local (hijacked) DNS server is never used. */
    private String dohResolve(String host) throws Exception {
        java.net.URL u = new java.net.URL("https://1.1.1.1/dns-query?name=" + host + "&type=A");
        javax.net.ssl.HttpsURLConnection c = (javax.net.ssl.HttpsURLConnection) u.openConnection();
        c.setRequestProperty("accept", "application/dns-json");
        c.setConnectTimeout(8000); c.setReadTimeout(8000);
        org.json.JSONObject j = new org.json.JSONObject(readAll(c.getInputStream()));
        org.json.JSONArray ans = j.optJSONArray("Answer");
        if (ans != null) for (int i = 0; i < ans.length(); i++) {
            org.json.JSONObject a = ans.getJSONObject(i);
            String d = a.optString("data", "");
            if (a.optInt("type") == 1 && d.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return d;
        }
        throw new Exception("DoH: no A record for " + host);
    }

    /** HTTPS GET to a specific IP with SNI=host (HTTP/1.0, read to close). */
    private String httpsGetByIp(String ip, String host, String path) throws Exception {
        javax.net.ssl.SSLSocketFactory f =
                (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
        javax.net.ssl.SSLSocket s = (javax.net.ssl.SSLSocket) f.createSocket();
        s.connect(new java.net.InetSocketAddress(ip, 443), 8000);
        s.setSoTimeout(8000);
        javax.net.ssl.SSLParameters p = s.getSSLParameters();
        p.setServerNames(java.util.Collections.singletonList(new javax.net.ssl.SNIHostName(host)));
        s.setSSLParameters(p);
        s.startHandshake();
        if (!javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier()
                .verify(host, s.getSession())) throw new Exception("TLS hostname mismatch");
        s.getOutputStream().write(("GET " + path + " HTTP/1.0\r\nHost: " + host
                + "\r\nUser-Agent: curl/8\r\nAccept: application/json\r\nConnection: close\r\n\r\n").getBytes());
        s.getOutputStream().flush();
        String resp = readAll(s.getInputStream());
        s.close();
        int i = resp.indexOf("\r\n\r\n");
        return i >= 0 ? resp.substring(i + 4) : resp;
    }

    private static String readAll(java.io.InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096]; int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return new String(bo.toByteArray(), "UTF-8");
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
