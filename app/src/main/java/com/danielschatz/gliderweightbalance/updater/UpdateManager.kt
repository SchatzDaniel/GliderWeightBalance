package com.danielschatz.gliderweightbalance.updater

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import androidx.work.*
import com.danielschatz.gliderweightbalance.R
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

object UpdateManager {

    private const val WORK_NAME = "UpdateCheckWork"
    private const val CHECK_COOLDOWN_MS = 60 * 1000 // 1 Minute Cooldown für manuelle Checks

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

    fun runUpdateCheckNow(context: Context, lifecycleOwner: LifecycleOwner, onUpdateFound: (String, String, String) -> Unit) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lastCheck = prefs.getLong("last_update_check_timestamp", 0L)
        val lastCheckVersion = prefs.getString("last_checked_version_name", "")
        val now = System.currentTimeMillis()

        val currentVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: Exception) { "0.0.0" }

        // Spam-Schutz Logik:
        // Wir ignorieren den Cooldown, wenn sich die installierte Version geändert hat (für Tests wichtig!)
        val isDifferentVersion = currentVersion != lastCheckVersion

        if (now - lastCheck < CHECK_COOLDOWN_MS && !isDifferentVersion) {
            val version = prefs.getString("latest_available_version", null)
            val changelog = prefs.getString("latest_available_changelog", null)
            val apkUrl = prefs.getString("latest_available_apk_url", null)

            if (version != null && changelog != null && apkUrl != null && 
                VersionComparator.isNewer(version, currentVersion)) {
                onUpdateFound(version, changelog, apkUrl)
                return
            } else {
                Toast.makeText(context, R.string.update_latest, Toast.LENGTH_SHORT).show()
                return
            }
        }

        Toast.makeText(context, R.string.update_checking, Toast.LENGTH_SHORT).show()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
            .setConstraints(constraints)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(updateRequest)

        workManager.getWorkInfoByIdLiveData(updateRequest.id).observe(lifecycleOwner) { workInfo ->
            if (workInfo != null && workInfo.state.isFinished) {
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val version = prefs.getString("latest_available_version", null)
                    val changelog = prefs.getString("latest_available_changelog", null)
                    val apkUrl = prefs.getString("latest_available_apk_url", null)

                    if (version != null && changelog != null && apkUrl != null) {
                        onUpdateFound(version, changelog, apkUrl)
                    } else {
                        Toast.makeText(context, R.string.update_latest, Toast.LENGTH_SHORT).show()
                    }
                } else if (workInfo.state == WorkInfo.State.FAILED) {
                    val errorCode = workInfo.outputData.getInt(UpdateWorker.KEY_ERROR_CODE, 0)
                    if (errorCode == UpdateWorker.ERROR_RATE_LIMIT) {
                        Toast.makeText(context, R.string.update_error_rate_limit, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, R.string.update_error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadAndInstallApk(context: Context, url: String, fileName: String, onPermissionNeeded: (File) -> Unit) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        if (isDownloadInProgress(context, fileName)) {
            Toast.makeText(context, R.string.update_downloading, Toast.LENGTH_SHORT).show()
            return
        }

        val request = DownloadManager.Request(url.toUri())
            .setTitle(context.getString(R.string.update_downloading))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    val downloadedFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    checkPermissionAndInstall(context, downloadedFile, onPermissionNeeded)
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {}
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
        
        Toast.makeText(context, R.string.update_downloading, Toast.LENGTH_SHORT).show()
    }

    private fun isDownloadInProgress(context: Context, fileName: String): Boolean {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PENDING)
        val cursor = downloadManager.query(query)
        var inProgress = false
        if (cursor != null && cursor.moveToFirst()) {
            val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
            if (titleIndex != -1) {
                do {
                    val title = cursor.getString(titleIndex)
                    if (title.contains(fileName) || title == context.getString(R.string.update_downloading)) {
                        inProgress = true
                        break
                    }
                } while (cursor.moveToNext())
            }
        }
        cursor?.close()
        return inProgress
    }

    fun checkPermissionAndInstall(context: Context, apkFile: File, onPermissionNeeded: (File) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            onPermissionNeeded(apkFile)
        } else {
            installApk(context, apkFile)
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return
        
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setDataAndType(contentUri, "application/vnd.android.package-archive")
        }
        context.startActivity(installIntent)
    }
}
