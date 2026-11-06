package com.tcs.home;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Base64;
import android.view.HapticFeedbackConstants;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** TCS Home: a BlackBerry QNX style launcher. No internet permission, no web links. */
public class MainActivity extends Activity {
    static MainActivity cur;
    private WebView web;
    private BroadcastReceiver screenOff, pkgChange;
    private boolean lockPending = false, visible = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        cur = this;
        web = new WebView(this);
        web.setBackgroundColor(0xFF000000);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        web.addJavascriptInterface(new Bridge(), "TCS");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return !r.getUrl().toString().startsWith("file:///android_asset/");
            }
        });
        web.loadUrl("file:///android_asset/index.html");

        screenOff = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                if (visible) lockPending = true;
                js("window.onScreenOff&&onScreenOff()");
            }
        };
        pkgChange = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                js("window.refreshApps&&refreshApps()");
            }
        };
        IntentFilter f1 = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        IntentFilter f2 = new IntentFilter();
        f2.addAction(Intent.ACTION_PACKAGE_ADDED);
        f2.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f2.addDataScheme("package");
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenOff, f1, Context.RECEIVER_EXPORTED);
            registerReceiver(pkgChange, f2, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(screenOff, f1);
            registerReceiver(pkgChange, f2);
        }
    }

    void js(String code) {
        if (web != null) web.post(() -> web.evaluateJavascript(code, null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;
        if (lockPending) { lockPending = false; js("window.onScreenOff&&onScreenOff()"); }
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        js("window.goHome&&goHome()");   // Home button pressed while already home
    }

    private void start(final Intent i) {
        runOnUiThread(() -> {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "Not available on this phone", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class Bridge {
        @JavascriptInterface
        public String apps() {
            PackageManager pm = getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
            List<String[]> rows = new ArrayList<>();
            HashSet<String> seen = new HashSet<>();
            for (ResolveInfo ri : list) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
                rows.add(new String[]{String.valueOf(ri.loadLabel(pm)), pkg, icon(ri.loadIcon(pm))});
            }
            Collections.sort(rows, new Comparator<String[]>() {
                @Override public int compare(String[] a, String[] b) { return a[0].compareToIgnoreCase(b[0]); }
            });
            JSONArray out = new JSONArray();
            try {
                for (String[] r : rows) out.put(new JSONObject().put("n", r[0]).put("p", r[1]).put("i", r[2]));
            } catch (Exception ignored) {}
            return out.toString();
        }

        private String icon(Drawable d) {
            try {
                Bitmap bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                d.setBounds(0, 0, 96, 96);
                d.draw(c);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 90, bo);
                return Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP);
            } catch (Exception e) { return ""; }
        }

        @JavascriptInterface
        public void launch(String pkg) {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) start(i);
        }

        @JavascriptInterface
        public void action(String a) {
            Intent i;
            switch (a) {
                case "dial": i = new Intent(Intent.ACTION_DIAL); break;
                case "msg": i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING); break;
                case "cam": i = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA); break;
                case "gal": i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY); break;
                case "clk": i = new Intent(AlarmClock.ACTION_SHOW_ALARMS); break;
                default: i = new Intent(a);
            }
            start(i);
        }

        @JavascriptInterface
        public void info(String pkg) {
            start(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
        }

        @JavascriptInterface
        public void uninstall(String pkg) {
            start(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)));
        }

        @JavascriptInterface
        public boolean hubOn() {
            String en = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
            return en != null && en.contains(getPackageName());
        }

        @JavascriptInterface
        public String hub() {
            JSONArray out = new JSONArray();
            HubService h = HubService.inst;
            if (h == null) return out.toString();
            try {
                StatusBarNotification[] all = h.getActiveNotifications();
                PackageManager pm = getPackageManager();
                List<StatusBarNotification> l = new ArrayList<>();
                for (StatusBarNotification s : all) {
                    if (s.isOngoing()) continue;
                    if ((s.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0) continue;
                    if (s.getPackageName().equals(getPackageName())) continue;
                    l.add(s);
                }
                Collections.sort(l, new Comparator<StatusBarNotification>() {
                    @Override public int compare(StatusBarNotification a, StatusBarNotification b) {
                        return Long.compare(b.getPostTime(), a.getPostTime());
                    }
                });
                int n = 0;
                for (StatusBarNotification s : l) {
                    if (n++ >= 60) break;
                    Notification nt = s.getNotification();
                    CharSequence t = nt.extras.getCharSequence(Notification.EXTRA_TITLE);
                    CharSequence x = nt.extras.getCharSequence(Notification.EXTRA_TEXT);
                    String app;
                    try { app = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(s.getPackageName(), 0))); }
                    catch (Exception e) { app = s.getPackageName(); }
                    out.put(new JSONObject().put("k", s.getKey()).put("p", s.getPackageName()).put("a", app)
                        .put("t", t == null ? "" : t.toString()).put("x", x == null ? "" : x.toString())
                        .put("w", s.getPostTime()));
                }
            } catch (Exception ignored) {}
            return out.toString();
        }

        @JavascriptInterface
        public void openNotif(String key) {
            HubService h = HubService.inst;
            if (h == null) return;
            try {
                for (StatusBarNotification s : h.getActiveNotifications()) {
                    if (s.getKey().equals(key) && s.getNotification().contentIntent != null) {
                        s.getNotification().contentIntent.send();
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void dismiss(String key) {
            HubService h = HubService.inst;
            if (h != null) { try { h.cancelNotification(key); } catch (Exception ignored) {} }
        }

        @JavascriptInterface
        public String status() {
            boolean home = false, dark;
            try {
                ResolveInfo ri = getPackageManager().resolveActivity(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0x10000);
                home = ri != null && getPackageName().equals(ri.activityInfo.packageName);
            } catch (Exception ignored) {}
            dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
            return "{\"home\":" + home + ",\"dark\":" + dark + "}";
        }

        @JavascriptInterface
        public String setWallpaper(String b64) {
            try {
                byte[] d = Base64.decode(b64, Base64.DEFAULT);
                Bitmap bm = BitmapFactory.decodeByteArray(d, 0, d.length);
                WallpaperManager.getInstance(MainActivity.this)
                    .setBitmap(bm, null, true, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                return "ok";
            } catch (Exception e) {
                return "error";
            }
        }

        @JavascriptInterface
        public void haptic() {
            web.post(() -> web.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP));
        }

        @JavascriptInterface
        public String sys() {
            int l = 0, ram = 0, st = 0;
            boolean ch = false;
            Intent bi = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bi != null) {
                int lv = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int sc = bi.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int s = bi.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                l = lv * 100 / sc;
                ch = s == BatteryManager.BATTERY_STATUS_CHARGING || s == BatteryManager.BATTERY_STATUS_FULL;
            }
            try {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(mi);
                ram = (int) (mi.availMem * 100 / mi.totalMem);
                StatFs sf = new StatFs(Environment.getDataDirectory().getPath());
                st = (int) (sf.getAvailableBytes() * 100 / sf.getTotalBytes());
            } catch (Exception ignored) {}
            return "{\"l\":" + l + ",\"c\":" + ch + ",\"ram\":" + ram + ",\"st\":" + st + "}";
        }
    }

    @Override
    public void onBackPressed() { /* a launcher has no back */ }

    @Override
    protected void onDestroy() {
        if (cur == this) cur = null;
        try { unregisterReceiver(screenOff); unregisterReceiver(pkgChange); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
