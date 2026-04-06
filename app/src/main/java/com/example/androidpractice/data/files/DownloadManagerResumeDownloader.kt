package com.example.androidpractice.data.files

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.URLUtil
import com.example.androidpractice.domain.repository.ResumeDownloader
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DownloadManagerResumeDownloader(
    private val context: Context
) : ResumeDownloader {

    override suspend fun downloadResume(url: String): String {
        val downloadUrl = normalizeDownloadUrl(url)
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Resume download")
            setDescription(downloadUrl)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, buildFileName(downloadUrl))
        }

        val downloadId = downloadManager.enqueue(request)
        return waitForDownload(downloadManager, downloadId)
    }

    private suspend fun waitForDownload(
        downloadManager: DownloadManager,
        downloadId: Long
    ): String = suspendCancellableCoroutine { continuation ->
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (completedId != downloadId) return

                runCatching { this@DownloadManagerResumeDownloader.context.unregisterReceiver(this) }

                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query).use { cursor ->
                    if (!cursor.moveToFirst()) {
                        continuation.resumeWithException(IllegalStateException("Download not found"))
                        return
                    }

                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val uri = downloadManager.getUriForDownloadedFile(downloadId)
                            ?: cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))?.let(Uri::parse)

                        if (uri == null) {
                            continuation.resumeWithException(IllegalStateException("Could not resolve downloaded file URI"))
                        } else {
                            continuation.resume(uri.toString())
                        }
                    } else {
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        continuation.resumeWithException(
                            IllegalStateException("Download failed, reason=$reason")
                        )
                    }
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        continuation.invokeOnCancellation {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    private fun buildFileName(url: String): String {
        extractGoogleDriveFileId(url)?.let { fileId ->
            return "resume_${fileId}.pdf"
        }

        val guessed = URLUtil.guessFileName(url, null, "application/pdf")
        val withoutHtmlSuffix = guessed.removeSuffix(".html")

        return when {
            withoutHtmlSuffix.contains('.') -> withoutHtmlSuffix
            else -> "$withoutHtmlSuffix.pdf"
        }
    }

    private fun normalizeDownloadUrl(url: String): String {
        val uri = Uri.parse(url)
        if (!uri.host.orEmpty().contains("drive.google.com")) return url

        val fileId = extractGoogleDriveFileId(url) ?: return url
        return "https://drive.google.com/uc?export=download&id=$fileId"
    }

    private fun extractGoogleDriveFileId(url: String): String? {
        val uri = Uri.parse(url)
        uri.getQueryParameter("id")?.takeIf { it.isNotBlank() }?.let { return it }

        val segments = uri.pathSegments
        val fileSegmentIndex = segments.indexOf("file")
        if (fileSegmentIndex >= 0 && segments.getOrNull(fileSegmentIndex + 1) == "d") {
            return segments.getOrNull(fileSegmentIndex + 2)
        }

        return null
    }
}
