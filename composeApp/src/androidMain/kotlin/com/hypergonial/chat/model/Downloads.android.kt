package com.hypergonial.chat.model

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.compose.ui.platform.UriHandler
import androidx.core.net.toUri
import com.hypergonial.chat.ContextHelper

/** A downloader implementation that delegates to the Android [DownloadManager] */
object AndroidDownloader : Downloader {
    override fun downloadFile(url: String, uriHandler: UriHandler, fileName: String) {
        val request =
            DownloadManager.Request(url.toUri()).apply {
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }

        val dl = ContextHelper.retrieveAppContext()?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?

        dl?.enqueue(request)
    }
}

actual val downloader: Downloader = AndroidDownloader
