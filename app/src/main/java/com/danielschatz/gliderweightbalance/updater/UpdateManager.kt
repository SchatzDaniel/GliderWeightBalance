package com.danielschatz.gliderweightbalance.updater

import android.content.Context
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.work.*
import com.danielschatz.gliderweightbalance.R
import java.util.concurrent.TimeUnit

object UpdateManager {

    private const val WORK_NAME = "UpdateCheckWork"

    fun scheduleUpdateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )
    }

    /**
     * Can be called manually from "About" or "Settings" to trigger an immediate check.
     */
    fun runUpdateCheckNow(context: Context) {
        Toast.makeText(context, R.string.update_checking, Toast.LENGTH_SHORT).show()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(updateRequest)

        // Listen for results to show a Toast
        workManager.getWorkInfoByIdLiveData(updateRequest.id).observeForever { workInfo ->
            if (workInfo != null && workInfo.state.isFinished) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val latest = prefs.getString("latest_available_version", null)
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    if (latest != null) {
                        Toast.makeText(context, context.getString(R.string.update_available_msg, latest), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, R.string.update_latest, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, R.string.update_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
