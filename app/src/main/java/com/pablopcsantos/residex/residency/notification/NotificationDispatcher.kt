package com.pablopcsantos.residex.residency.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pablopcsantos.residex.MainActivity
import com.pablopcsantos.residex.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class NotificationDispatcher @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Residex",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alertas de inscrições e etapas das seleções acompanhadas"
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun notify(candidate: NotificationCandidate): NotificationPostResult {
        if (!hasRuntimePermission()) return NotificationPostResult.PERMISSION_REQUIRED
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return NotificationPostResult.DISABLED
        }

        return runCatching {
            ensureChannel()
            if (isChannelDisabled()) return NotificationPostResult.DISABLED

            val timing = when (candidate.daysUntil) {
                0L -> "hoje"
                1L -> "em 1 dia"
                else -> "em ${candidate.daysUntil} dias"
            }
            val text = buildString {
                append(candidate.selectionName).append("\n")
                append(candidate.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .append(" (")
                    .append(timing)
                    .append(")")
                if (candidate.fee.isNotBlank()) append("\nTaxa: ").append(candidate.fee)
            }
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_residex_foreground)
                .setContentTitle(candidate.title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

            val notificationId = if (candidate.key == TEST_NOTIFICATION_KEY) {
                TEST_NOTIFICATION_ID
            } else {
                candidate.key.hashCode()
            }
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            NotificationPostResult.POSTED
        }.getOrElse {
            NotificationPostResult.FAILED
        }
    }

    fun test(): NotificationPostResult = notify(
        NotificationCandidate(
            key = TEST_NOTIFICATION_KEY,
            type = NotificationEventType.OBJECTIVE_EXAM,
            selectionId = "test",
            selectionName = "As notificações do calendário estão funcionando.",
            date = LocalDate.now(),
            daysUntil = 0,
            title = "Notificação de teste",
            fee = ""
        )
    )

    private fun hasRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    private fun isChannelDisabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
    }

    companion object {
        const val CHANNEL_ID = "residency_events"
        private const val TEST_NOTIFICATION_KEY = "residency-notification-test"
        private const val TEST_NOTIFICATION_ID = 7_001
    }
}
