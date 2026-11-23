package com.cyberguard.android;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.net.wifi.*;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION=41, REQ_NOTIFICATIONS=42;
    private static final String CHANNEL="cyberguard_alerts";
    private static final int BG=Color.rgb(7,12,20), PANEL=Color.rgb(16,25,38), PANEL2=Color.rgb(21,34,49);
    private static final int GREEN=Color.rgb(62,240,164), MUTED=Color.rgb(139,160,181), WHITE=Color.rgb(235,245,250);
    private WifiManager wifi;
    private LinearLayout root, body, nav;
    private TextView pageTitle, pageSub, status;
    private String page="HOME";
    private final Set<String> seen=new HashSet<>();
    private final ArrayList<String> history=new ArrayList<>();
    private final android.os.Handler handler=new android.os.Handler();
    private BroadcastReceiver scanReceiver;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        wifi=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        createChannel();
        buildShell();
        showPage("HOME");
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);
        scanReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
            if(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(i.getAction())) {
                boolean updated=i.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED,false);
                renderScanResults(updated);
            }
        }};
        IntentFilter filter=new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(scanReceiver,filter,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(scanReceiver,filter);
    }
    @Override protected void onDestroy(){super.onDestroy(); if(scanReceiver!=null) try{unregisterReceiver(scanReceiver);}catch(Exception ignored){}}

    private void buildShell(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        LinearLayout top=new LinearLayout(this); top.setOrientation(LinearLayout.VERTICAL); top.setPadding(dp(22),dp(18),dp(22),dp(14));
        LinearLayout brandRow=new LinearLayout(this); brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=text("⌁",26,GREEN,true); logo.setGravity(Gravity.CENTER); logo.setBackground(round(PANEL2,GREEN,dp(14))); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(46),dp(46)); lp.rightMargin=dp(12); brandRow.addView(logo,lp);
        LinearLayout names=new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL);
        names.addView(text("CYBERGUARD",18,WHITE,true)); names.addView(text("PERSONAL SECURITY CONSOLE",9,GREEN,true)); brandRow.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView live=text("●  LOCAL",10,GREEN,true); live.setPadding(dp(10),dp(7),dp(10),dp(7)); live.setBackground(round(PANEL2,GREEN,dp(18))); brandRow.addView(live);
        top.addView(brandRow);
        pageTitle=text("Overview",25,WHITE,true); LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(-1,-2); t.topMargin=dp(20); top.addView(pageTitle,t);
        pageSub=text("Your network. Your visibility.",12,MUTED,false); top.addView(pageSub);
        root.addView(top);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),dp(4),dp(20),dp(24)); scroll.addView(body); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this); nav.setPadding(dp(5),dp(8),dp(5),dp(8)); nav.setBackgroundColor(Color.rgb(11,18,28)); root.addView(nav,new LinearLayout.LayoutParams(-1,dp(66)));
        setContentView(root);
    }
    private void showPage(String p){
        page=p; body.removeAllViews();
        String title,sub;
        if(p.equals("HOME")){title="Security overview";sub="A clear view of your digital surroundings";}
        else if(p.equals("WIFI")){title="Wi-Fi scanner";sub="Discover nearby access points safely";}
        else if(p.equals("NETWORK")){title="Network details";sub="Your current connection information";}
        else if(p.equals("SECURITY")){title="Security check";sub="Practical checks for safer connections";}
        else if(p.equals("HISTORY")){title="Activity history";sub="Recent observations on this device";}
        else {title="About CyberGuard";sub="Built for awareness, not intrusion";}
        pageTitle.setText(title); pageSub.setText(sub); buildNav();
        if(p.equals("HOME")) homePage(); else if(p.equals("WIFI")) wifiPage(); else if(p.equals("NETWORK")) networkPage(); else if(p.equals("SECURITY")) securityPage(); else if(p.equals("HISTORY")) historyPage(); else aboutPage();
    }
    private void buildNav(){
        nav.removeAllViews(); String[] labels={"HOME","WI-FI","NETWORK","CHECK","LOGS"};
        String[] keys={"HOME","WIFI","NETWORK","SECURITY","HISTORY"};
        String[] icons={"⌂","◉","⌁","✓","≡"};
        for(int i=0;i<keys.length;i++){ final String key=keys[i]; boolean active=page.equals(key);
            LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER);
            TextView ic=text(icons[i],19,active?GREEN:MUTED,true); ic.setGravity(Gravity.CENTER); item.addView(ic);
            TextView tx=text(labels[i],9,active?GREEN:MUTED,true); item.addView(tx);
            item.setOnClickListener(v->showPage(key)); nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));
        }
    }
    private void homePage(){
        card(body, PANEL, 18, v->showPage("SECURITY"));
        LinearLayout hero=(LinearLayout)body.getChildAt(body.getChildCount()-1); hero.setPadding(dp(18),dp(18),dp(18),dp(18));
        hero.addView(text("SYSTEM STATUS",10,GREEN,true));
        hero.addView(text("Stay aware. Stay protected.",21,WHITE,true),margin(0,dp(7),0,dp(5)));
        hero.addView(text("CyberGuard helps you understand nearby Wi-Fi and the network your phone is using.",12,MUTED,false));
        LinearLayout stats=new LinearLayout(this); stats.setPadding(0,dp(16),0,0);
        stats.addView(stat("01","DEVICE",GREEN),new LinearLayout.LayoutParams(0,-2,1));
        stats.addView(stat("LOCAL","ONLY",WHITE),new LinearLayout.LayoutParams(0,-2,1));
        stats.addView(stat("READY","STATUS",GREEN),new LinearLayout.LayoutParams(0,-2,1)); hero.addView(stats);
        section("QUICK ACTIONS");
        actionCard("◉","Scan nearby Wi-Fi","See signal, frequency and advertised security","OPEN SCANNER",()->showPage("WIFI"));
        actionCard("⌁","Connection details","Review your current Wi-Fi and IP information","VIEW NETWORK",()->showPage("NETWORK"));
        actionCard("✓","Security check","Read practical warnings and recommendations","RUN CHECK",()->showPage("SECURITY"));
        section("LIVE CONNECTION");
        addInfo("Wi-Fi",currentSsid()); addInfo("Signal",currentSignal()); addInfo("IP address",currentIp());
        section("GOOD TO KNOW"); noteCard("Your privacy matters","Scans are processed on your phone. The app does not try to access other people's devices or networks.");
    }
    private TextView stat(String value,String label,int color){TextView t=text(value+"\\n"+label,15,color,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(8),dp(10),dp(8),dp(10));t.setBackground(round(PANEL2,Color.rgb(39,69,79),dp(10)));return t;}
    private void wifiPage(){
        status=text("Ready to scan nearby access points.",12,GREEN,true); panel(status);
        Button scan=primary("START WI-FI SCAN"); scan.setOnClickListener(v->beginScan()); body.addView(scan,margin(0,dp(12),0,dp(12)));
        Button refresh=secondary("REFRESH RESULTS"); refresh.setOnClickListener(v->renderScanResults(false)); body.addView(refresh,margin(0,0,0,dp(12)));
        section("ACCESS POINTS"); noteCard("Scan results","Android may return cached results or limit scan frequency. Enable Location and grant permission to use this feature.");
        TextView hint=text("No scan performed in this session.",12,MUTED,false); hint.setTag("scan_hint"); panel(hint);
    }
    private void networkPage(){
        section("CURRENT WI-FI"); addInfo("Network name",currentSsid()); addInfo("Signal strength",currentSignal());
        WifiInfo info=wifi==null?null:wifi.getConnectionInfo();
        addInfo("Link speed",info==null?"Unavailable":info.getLinkSpeed()+" Mbps");
        addInfo("Frequency",info==null?"Unavailable":info.getFrequency()+" MHz");
        addInfo("Device IP",currentIp());
        if(wifi!=null) try{WifiInfo wi=wifi.getConnectionInfo(); if(wi!=null && wi.getNetworkId()!=-1){android.net.DhcpInfo d=wifi.getDhcpInfo(); if(d!=null){addInfo("Gateway",ip(d.gateway));addInfo("Subnet mask",ip(d.netmask));addInfo("DNS server 1",ip(d.dns1));addInfo("DNS server 2",ip(d.dns2));}}}catch(Exception ignored){}
        section("ACTIONS"); actionCard("↻","Refresh connection","Update the information shown above","REFRESH",()->showPage("NETWORK"));
        noteCard("Privacy note","Some Android versions hide or limit network details. Unavailable fields are not necessarily a problem.");
    }
    private void securityPage(){
        section("QUICK SECURITY REVIEW");
        addCheck("Wi-Fi connection","Check whether your phone is connected to a Wi-Fi network.",wifi!=null && wifi.getConnectionInfo()!=null && wifi.getConnectionInfo().getNetworkId()!=-1);
        addCheck("Location permission","Required by Android 11 for nearby Wi-Fi scan results.",checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED);
        addCheck("Android version","Keep Android and apps updated when updates are available.",Build.VERSION.SDK_INT>=30);
        noteCard("Screen lock reminder","Make sure a PIN, password, or biometric screen lock is enabled in Android Settings.");
        section("RECOMMENDATIONS");
        noteCard("Prefer WPA2 or WPA3","Use a strong router password and modern encryption. Avoid open or outdated WEP networks for sensitive activity.");
        noteCard("Check unfamiliar networks","A familiar network name does not prove that an access point is genuine. Verify public Wi-Fi details with the venue.");
        noteCard("Keep software updated","Install Android security updates and update apps from trusted stores.");
        Button loc=secondary("OPEN LOCATION SETTINGS"); loc.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))); body.addView(loc,margin(0,dp(8),0,0));
    }
    private void historyPage(){
        section("RECENT OBSERVATIONS");
        if(history.isEmpty()) noteCard("Nothing recorded yet","Your Wi-Fi scan observations will appear here during this app session.");
        else for(String h:history) addInfo("Observed access point",h);
        Button clear=secondary("CLEAR SESSION HISTORY"); clear.setOnClickListener(v->{history.clear();seen.clear();showPage("HISTORY");}); body.addView(clear,margin(0,dp(12),0,0));
        noteCard("Session storage","This first version keeps scan history in memory only. It clears when the app process ends.");
    }
    private void aboutPage(){
        section("CYBERGUARD"); noteCard("Personal security console","A lightweight Android network-awareness app designed for everyday users.");
        addInfo("Version","1.1.0"); addInfo("Platform","Android 6.0+"); addInfo("Design","Dark security dashboard");
        section("WHAT IT DOES"); noteCard("Visibility","Shows nearby Wi-Fi information and details Android shares about your current connection.");
        noteCard("What it does not do","It is not an antivirus, cannot guarantee a network is safe, and does not break into networks or devices.");
        section("PERMISSIONS"); addInfo("Location","Needed by Android for nearby Wi-Fi scan results"); addInfo("Wi-Fi state","Reads connection and scan information"); addInfo("Notifications","Optional new-access-point alerts");
    }
    private void addCheck(String title,String desc,boolean ok){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackground(round(PANEL,ok?GREEN:Color.rgb(245,177,75),dp(15)));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(text(title,14,WHITE,true),new LinearLayout.LayoutParams(0,-2,1));row.addView(text(ok?"● OK":"● REVIEW",10,ok?GREEN:Color.rgb(245,177,75),true));c.addView(row);c.addView(text(desc,11,MUTED,false),margin(0,dp(6),0,0));body.addView(c,margin(0,0,0,dp(9)));
    }
    private void actionCard(String icon,String title,String desc,String button,Runnable action){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackground(round(PANEL,GREEN,dp(15)));
        c.addView(text(icon+"   "+title,15,WHITE,true));c.addView(text(desc,11,MUTED,false),margin(0,dp(5),0,dp(10)));
        Button b=secondary(button);b.setOnClickListener(v->action.run());c.addView(b);body.addView(c,margin(0,0,0,dp(10)));
    }
    private void noteCard(String title,String desc){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackground(round(PANEL,Color.rgb(48,74,91),dp(14)));c.addView(text(title,14,WHITE,true));c.addView(text(desc,11,MUTED,false),margin(0,dp(5),0,0));body.addView(c,margin(0,0,0,dp(9)));}
    private void addInfo(String label,String value){LinearLayout c=new LinearLayout(this);c.setGravity(Gravity.CENTER_VERTICAL);c.setPadding(dp(14),dp(13),dp(14),dp(13));c.setBackground(round(PANEL,Color.rgb(35,54,69),dp(12)));LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.addView(text(label.toUpperCase(Locale.ROOT),9,MUTED,true));col.addView(text(value,13,WHITE,true),margin(0,dp(4),0,0));c.addView(col);body.addView(c,margin(0,0,0,dp(7)));}
    private void section(String s){TextView t=text(s,10,GREEN,true);t.setLetterSpacing(.12f);body.addView(t,margin(0,dp(16),0,dp(9)));}
    private void panel(View v){v.setPadding(dp(14),dp(13),dp(14),dp(13));v.setBackground(round(PANEL,GREEN,dp(13)));body.addView(v,margin(0,0,0,dp(8)));}
    private void beginScan(){
        if(wifi==null){setStatus("Wi-Fi service is unavailable.");return;}
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQ_LOCATION);return;}
        LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
        boolean enabled=lm!=null&&(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)||lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        if(!enabled){setStatus("Enable Location services, then scan again.");startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));return;}
        setStatus("Scanning… waiting for Android scan results.");
        try{wifi.startScan();handler.postDelayed(()->renderScanResults(false),3500);}catch(SecurityException e){setStatus("Location permission is required.");}
    }
    private void renderScanResults(boolean updated){
        if(!page.equals("WIFI")||wifi==null)return;
        List<ScanResult> raw;try{raw=wifi.getScanResults();}catch(SecurityException e){setStatus("Permission required to read scan results.");return;}
        LinearLayout list=body; int hintIndex=-1;
        for(int i=0;i<list.getChildCount();i++){View v=list.getChildAt(i);if(v.getTag()!=null&&"scan_hint".equals(v.getTag())){hintIndex=i;break;}}
        if(hintIndex>=0)list.removeViewAt(hintIndex);
        if(raw==null||raw.isEmpty()){TextView empty=text("No access points returned. Check Location permission and try again later.",12,MUTED,false);empty.setTag("scan_hint");panel(empty);setStatus(updated?"No scan results available.":"Android may have throttled the scan.");return;}
        ArrayList<ScanResult> results=new ArrayList<>(raw);Collections.sort(results,(a,b)->Integer.compare(b.level,a.level));
        int found=0,newCount=0;
        for(ScanResult r:results){if(found++>=35)break;String key=!TextUtils.isEmpty(r.BSSID)?r.BSSID:r.SSID;
            if(!TextUtils.isEmpty(key)&&seen.add(key)){newCount++;history.add(0,clean(r.SSID)+" • "+r.level+" dBm");if(history.size()>60)history.remove(history.size()-1);}
            LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(round(PANEL,Color.rgb(43,68,82),dp(12)));
            c.addView(text(clean(r.SSID),14,WHITE,true));c.addView(text(security(r.capabilities)+"   •   "+r.frequency+" MHz",10,MUTED,false),margin(0,dp(4),0,0));
            c.addView(text("SIGNAL  "+r.level+" dBm",10,GREEN,true),margin(0,dp(5),0,0));list.addView(c,margin(0,0,0,dp(7)));
        }
        setStatus("Found "+Math.min(results.size(),35)+" access points • "+newCount+" new in this session");
        if(newCount>0)notifyNew(newCount);
    }
    private String security(String caps){String c=caps==null?"":caps.toUpperCase(Locale.ROOT);if(c.contains("WPA3"))return "WPA3";if(c.contains("WPA2"))return "WPA2";if(c.contains("WPA"))return "WPA";if(c.contains("WEP"))return "WEP • outdated";return "Open / unknown";}
    private void setStatus(String s){if(status!=null)status.setText(s);else Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"Network observations",NotificationManager.IMPORTANCE_DEFAULT);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
    private void notifyNew(int n){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;try{android.app.Notification note=new android.app.Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("CyberGuard update").setContentText(n+" newly observed access point(s)").setAutoCancel(true).build();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(5,note);}catch(Exception ignored){}}
    private Button primary(String s){Button b=button(s,GREEN,Color.rgb(5,23,18));return b;}
    private Button secondary(String s){return button(s,PANEL2,GREEN);}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setTextSize(11);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(fg);b.setAllCaps(false);b.setBackground(round(bg,fg,dp(12)));b.setMinHeight(dp(48));return b;}
    private LinearLayout.LayoutParams margin(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(l,t,r,b);return p;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);d.setStroke(dp(1),stroke);return d;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private String clean(String s){if(s==null||s.equals("<unknown ssid>"))return "Hidden / unavailable SSID";if(s.length()>2&&s.startsWith("\"")&&s.endsWith("\""))return s.substring(1,s.length()-1);return s;}
    private String ip(int v){return (v&255)+"."+((v>>8)&255)+"."+((v>>16)&255)+"."+((v>>24)&255);}
    private String currentSsid(){try{WifiInfo i=wifi==null?null:wifi.getConnectionInfo();return i==null||i.getNetworkId()==-1?"Not connected":clean(i.getSSID());}catch(Exception e){return "Unavailable";}}
    private String currentSignal(){try{WifiInfo i=wifi==null?null:wifi.getConnectionInfo();return i==null?"Unavailable":i.getRssi()+" dBm";}catch(Exception e){return "Unavailable";}}
    private String currentIp(){try{WifiInfo i=wifi==null?null:wifi.getConnectionInfo();return i==null?"Unavailable":ip(i.getIpAddress());}catch(Exception e){return "Unavailable";}}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==REQ_LOCATION){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)beginScan();else setStatus("Location permission denied. Wi-Fi scan is unavailable.");}}
}
