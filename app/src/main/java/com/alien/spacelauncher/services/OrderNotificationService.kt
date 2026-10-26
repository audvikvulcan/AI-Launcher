package com.alien.spacelauncher.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class OrderNotificationService : NotificationListenerService() {

    companion object {
        val recentNotifications = mutableListOf<String>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isNotBlank() || text.isNotBlank()) {
            val entry = "[$pkg] $title: $text"
            synchronized(recentNotifications) {
                recentNotifications.add(0, entry)
                if (recentNotifications.size > 25) {
                    recentNotifications.removeLast()
                }
            }
        }
    }
}
