package com.tcs.home;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Lets TCS Home show your notifications in its Hub. Only works after you turn it on in Android settings. */
public class HubService extends NotificationListenerService {
    static HubService inst;

    @Override public void onListenerConnected() { inst = this; ping(); }
    @Override public void onListenerDisconnected() { if (inst == this) inst = null; ping(); }
    @Override public void onNotificationPosted(StatusBarNotification s) { ping(); }
    @Override public void onNotificationRemoved(StatusBarNotification s) { ping(); }

    private static void ping() {
        MainActivity m = MainActivity.cur;
        if (m != null) m.js("window.hubChanged&&hubChanged()");
    }
}
