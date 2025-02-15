package com.hypergonial.chat.model

import androidx.compose.ui.platform.UriHandler
import io.ktor.http.Url

/** A downloader that can instruct the given platform to download a file from an HTTP url. */
interface Downloader {
    /**
     * Download a file from the given url
     *
     * @param url The url to download the file from
     * @param uriHandler The handler to use to download the file (This may or may not be used depending on the platform)
     * @param fileName The name of the file to save the downloaded file as
     */
    fun downloadFile(url: String, uriHandler: UriHandler, fileName: String = url.resolveFileName())
}

/** A downloader that delegates downloading to the browser of the platform */
object CommonDownloader : Downloader {
    override fun downloadFile(url: String, uriHandler: UriHandler, fileName: String) {
        uriHandler.openUri(url)
    }
}

/** Access the platform's [Downloader] */
expect val downloader: Downloader

private fun String.resolveFileName(): String {
    return Url(this).segments.last()
}
