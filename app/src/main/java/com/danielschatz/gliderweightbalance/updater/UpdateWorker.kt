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
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.danielschatz.gliderweightbalance.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.ContextCompat

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val GITHUB_REPO = "SchatzDaniel/GliderWeightBalance"
        private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases"
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 1001
        
        const val KEY_ERROR_CODE = "error_code"
        const val ERROR_RATE_LIMIT = 403
    }

    override suspend fun doWork(): Result {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val channel = prefs.getString("update_channel", "stable") ?: "stable"

        try {
            val releases = fetchReleases()
            
            val packageInfo = applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
            val rawVersion = packageInfo.versionName ?: "0.0.0"
            val isCurrentBeta = rawVersion.contains("beta", ignoreCase = true) || rawVersion.contains("alpha", ignoreCase = true)
            
            val latestRelease = releases.firstOrNull { release ->
                if (channel == "stable" && !isCurrentBeta) !release.prerelease else true
            }

            if (latestRelease != null) {
                if (VersionComparator.isNewer(latestRelease.tag_name, rawVersion)) {
                    val apkAsset = latestRelease.assets.firstOrNull { it.name.endsWith(".apk") }
                    
                    prefs.edit(commit = true) {
                        putString("latest_available_version", latestRelease.tag_name)
                        putString("latest_available_url", latestRelease.html_url)
                        putString("latest_available_changelog", latestRelease.body)
                        putString("latest_available_apk_url", apkAsset?.browser_download_url)
                        putLong("last_update_check_timestamp", System.currentTimeMillis())
                        putString("last_checked_version_name", rawVersion)
                    }
                    showNotification(latestRelease)
                } else {
                    prefs.edit(commit = true) {
                        remove("latest_available_version")
                        remove("latest_available_url")
                        remove("latest_available_changelog")
                        remove("latest_available_apk_url")
                        putLong("last_update_check_timestamp", System.currentTimeMillis())
                        putString("last_checked_version_name", rawVersion)
                    }
                }
            }
        } catch (e: GitHubThrottledException) {
            val outputData = Data.Builder()
                .putInt(KEY_ERROR_CODE, ERROR_RATE_LIMIT)
                .build()
            return Result.failure(outputData)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }

        return Result.success()
    }

    private fun fetchReleases(): List<GitHubRelease> {
        val url = URL(API_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

        val responseCode = connection.responseCode
        if (responseCode == 403 || responseCode == 429) {
            throw GitHubThrottledException()
        }

        return if (responseCode == 200) {
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<GitHubRelease>>() {}.type
            Gson().fromJson(json, type)
        } else {
            throw Exception("GitHub API error: $responseCode")
        }
    }

    private fun showNotification(release: GitHubRelease) {
        createNotificationChannel()

        val intent = Intent(applicationContext, Class.forName("com.danielschatz.gliderweightbalance.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("SHOW_UPDATE_DIALOG", true)
        }

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

    class GitHubThrottledException : Exception()

    data class GitHubRelease(
        val tag_name: String,
        val body: String,
        val html_url: String,
        val assets: List<GitHubAsset>,
        val prerelease: Boolean
    )

    data class GitHubAsset(
        val name: String,
        val browser_download_url: String
    )
}
