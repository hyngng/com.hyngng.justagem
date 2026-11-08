package com.hyngng.gemtalk.media

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PhotoIndexingNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PhotoSemanticIndexManager.ACTION_STOP_INDEXING) return
        PhotoSemanticIndexManager.stopActiveIndexing()
        // The tap can arrive after the process that owned the run was killed.
        // No manager exists then, so the stop call is a no-op and the ongoing
        // notification would stay in the shade forever with a dead button.
        if (!PhotoSemanticIndexManager.hasActiveIndexing()) {
            context.getSystemService(NotificationManager::class.java)
                ?.cancel(PhotoSemanticIndexManager.NOTIFICATION_ID)
        }
    }
}
