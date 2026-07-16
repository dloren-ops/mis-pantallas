package com.dloren.mispantallas.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Programa la revisión diaria de vencimientos, apuntando a que corra cerca de las 9am locales. */
object ReminderScheduler {
    private const val UNIQUE_WORK_NAME = "renewal_reminder"
    private const val PREFERRED_HOUR = 9

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RenewalReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun initialDelayMillis(): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(PREFERRED_HOUR).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next).toMillis()
    }
}
