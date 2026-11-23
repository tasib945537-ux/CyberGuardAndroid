package com.cyberguard.android;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION = 41;
    private static final String CHANNEL_ID = "cyberguard_scan";
    private WifiManager wifi;
    private LinearLayout content;
    private TextView status;
    private final Set<String> seen = new HashSet<>();
    private final List<String> history = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        buildUi();
        createNotificationChannel();
        showConnection();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
    }

    private void buildUi() {
        getWindow().setStatusBarColor(Color.rgb(10, 15, 28));
        getWindow().setNavigationBarColor(Color.rgb(10, 15, 28));
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(10, 15, 28));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(28));
        scroll.addView(root);
        TextView brand = text("CYBERGUARD", 25, Color.WHITE, true);
        root.addView(brand);
        TextView sub = text("Wi-Fi awareness & device network overview", 13, Color.rgb(153, 170, 196), false);
        root.addView(sub);
        status = text("Ready to check your network", 14, Color.rgb(116, 225, 185), true);
        status.setPadding(0, dp(18), 0, dp(12));
        root.addView(status);
        Button scan = button("SCAN NEARBY WI-FI");
        scan.setOnClickListener(v -> beginScan());
        root.addView(scan);
        Button refresh = button("REFRESH CONNECTION");
        refresh.setOnClickListener(v -> showConnection());
        root.addView(refresh);
        TextView connTitle = text("CURRENT CONNECTION", 16, Color.WHITE, true);
        connTitle.setPadding(0, dp(22), 0, dp(8));
        root.addView(connTitle);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);
        TextView note = text("Note: Android requires Location permission and Location services for nearby Wi-Fi scan results. Scan frequency may be limited by Android.", 12, Color.rgb(150, 165, 188), false);
        note.setPadding(0, dp(20), 0, dp(8));
        root.addView(note);
        setContentView(scroll);
    }

    private void showConnection() {
        if (content == null) return;
        content.removeAllViews();
        if (wifi == null) { addCard("Wi-Fi service unavailable"); return; }
        WifiInfo info = wifi.getConnectionInfo();
        if (info == null || info.getNetworkId() == -1) { addCard("Not connected to Wi-Fi"); return; }
        addCard("Network: " + clean(info.getSSID()));
        addCard("Signal: " + info.getRssi() + " dBm");
        addCard("Link speed: " + info.getLinkSpeed() + " Mbps");
        addCard("Frequency: " + info.getFrequency() + " MHz");
        addCard("Device IP: " + intToIp(info.getIpAddress()));
        try {
            android.net.DhcpInfo d = wifi.getDhcpInfo();
            if (d != null) {
                addCard("Gateway: " + intToIp(d.gateway));
                addCard("Subnet: " + intToIp(d.netmask));
                addCard("DNS 1: " + intToIp(d.dns1));
                addCard("DNS 2: " + intToIp(d.dns2));
            }
        } catch (Exception ignored) { addCard("Router details are restricted on this Android version"); }
    }

    private void beginScan() {
        if (wifi == null) { status.setText("Wi-Fi is not available on this device"); return; }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        if (!enabled) {
            status.setText("Turn on Location services, then scan again.");
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        status.setText("Scanning… Android may limit scan frequency.");
        try {
            boolean started = wifi.startScan();
            displayResults(wifi.getScanResults(), started);
        } catch (SecurityException e) {
            status.setText("Location permission is needed to scan Wi-Fi.");
        } catch (Exception e) {
            status.setText("Scan unavailable: " + e.getClass().getSimpleName());
        }
    }

    private void displayResults(List<ScanResult> results, boolean started) {
        if (results == null || results.isEmpty()) {
            status.setText(started ? "Scan requested, but Android returned no results yet. Try again shortly." : "Android throttled this scan. Showing no fresh results.");
            return;
        }
        List<ScanResult> sorted = new ArrayList<>(results);
        Collections.sort(sorted, (a,b) -> Integer.compare(b.level, a.level));
        content.removeAllViews();
        int count = 0, newCount = 0;
        for (ScanResult r : sorted) {
            if (count++ >= 30) break;
            String bssid = r.BSSID == null ? "" : r.BSSID;
            String key = bssid.isEmpty() ? r.SSID : bssid;
            if (!TextUtils.isEmpty(key) && seen.add(key)) {
                newCount++;
                history.add(0, clean(r.SSID) + " • " + r.level + " dBm");
                if (history.size() > 30) history.remove(history.size()-1);
            }
            addCard(clean(r.SSID) + "\n" + security(r.capabilities) + "  •  " + r.frequency + " MHz\nSignal: " + r.level + " dBm");
        }
        status.setText("Found " + Math.min(sorted.size(),30) + " access points" + (newCount > 0 ? " • " + newCount + " newly observed" : ""));
        if (newCount > 0) notifyNew(newCount);
    }

    private String security(String caps) {
        if (caps == null || caps.isEmpty()) return "Security: Unknown";
        String c = caps.toUpperCase(Locale.ROOT);
        if (c.contains("WPA3")) return "WPA3";
        if (c.contains("WPA2")) return "WPA2";
        if (c.contains("WPA")) return "WPA";
        if (c.contains("WEP")) return "WEP (outdated)";
        return "Open / no encryption";
    }

    private void notifyNew(int count) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        try {
            android.app.Notification n = new android.app.Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("CyberGuard network update")
                .setContentText(count + " newly observed Wi-Fi access point(s)")
                .setAutoCancel(true).build();
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(7, n);
        } catch (Exception ignored) { }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Wi-Fi scan updates", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private void addCard(String value) {
        TextView t = text(value, 14, Color.rgb(225, 234, 248), false);
        t.setPadding(dp(15), dp(14), dp(15), dp(14));
        t.setBackgroundColor(Color.rgb(24, 34, 54));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(8);
        content.addView(t, p);
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE);
        b.setAllCaps(false); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(36, 108, 224)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52)); p.topMargin = dp(8);
        b.setLayoutParams(p); return b;
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private String clean(String s) {
        if (s == null || s.equals("<unknown ssid>")) return "Hidden / unavailable SSID";
        if (s.length() > 2 && s.startsWith("\"") && s.endsWith("\"")) return s.substring(1,s.length()-1);
        return s;
    }
    private String intToIp(int v) { return (v & 255) + "." + ((v >> 8) & 255) + "." + ((v >> 16) & 255) + "." + ((v >> 24) & 255); }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_LOCATION) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) beginScan();
            else status.setText("Location permission denied. Nearby Wi-Fi scan is unavailable.");
        }
    }
}
