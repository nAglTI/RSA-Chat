package com.hypergonial.chat.model

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.platform.UriHandler
import com.hypergonial.chat.ContextHelper

/** A downloader implementation that delegates to the Android [DownloadManager] */
object AndroidDownloader : Downloader {
    override fun downloadFile(url: String, uriHandler: UriHandler, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        val dl = ContextHelper.retrieveAppContext()?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?

        dl?.enqueue(request)
    }
}

actual val downloader: Downloader = AndroidDownloader
