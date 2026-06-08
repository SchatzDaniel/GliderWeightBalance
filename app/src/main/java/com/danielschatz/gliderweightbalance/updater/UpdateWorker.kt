package com.danielschatz.gliderweightbalance.updater

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.danielschatz.gliderweightbalance.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val GITHUB_REPO = "SchatzDaniel/GliderWeightBalance"
        private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases"
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): ListenableWorker.Result {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val channel = prefs.getString("update_channel", "stable") ?: "stable"

        try {
            val releases = fetchReleases()
            val latestRelease = releases.firstOrNull { release ->
                if (channel == "stable") !release.prerelease else true
            }

            if (latestRelease != null) {
                val currentVersion = applicationContext.packageManager
                    .getPackageInfo(applicationContext.packageName, 0).versionName ?: "0.0.0"

                if (VersionComparator.isNewer(latestRelease.tag_name.removePrefix("v"), currentVersion)) {
                    // Speichere die verfügbare Version für die Anzeige in der App
                    prefs.edit {
                        putString("latest_available_version", latestRelease.tag_name)
                        putString("latest_available_url", latestRelease.html_url)
                    }
                    showNotification(latestRelease)
                } else {
                    // Zurücksetzen, falls wir auf dem neuesten Stand sind
                    prefs.edit {
                        remove("latest_available_version")
                        remove("latest_available_url")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ListenableWorker.Result.retry()
        }

        return ListenableWorker.Result.success()
    }

    private fun fetchReleases(): List<GitHubRelease> {
        val url = URL(API_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        if (connection.responseCode == 200) {
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<GitHubRelease>>() {}.type
            return Gson().fromJson(json, type)
        } else {
            throw Exception("GitHub API error: ${connection.responseCode}")
        }
    }

    private fun showNotification(release: GitHubRelease) {
        createNotificationChannel()

        val intent = Intent(Intent.ACTION_VIEW, release.html_url.toUri())
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_wb)
            .setContentTitle(applicationContext.getString(R.string.update_available_title))
            .setContentText(applicationContext.getString(R.string.update_available_msg, release.tag_name))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.mipmap.ic_wb,
                applicationContext.getString(R.string.update_button),
                pendingIntent
            )
            .build()

        with(NotificationManagerCompat.from(applicationContext)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates"
            val descriptionText = "Notifications for new app versions"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    data class GitHubRelease(
        val tag_name: String,
        val prerelease: Boolean,
        val html_url: String
    )
}
