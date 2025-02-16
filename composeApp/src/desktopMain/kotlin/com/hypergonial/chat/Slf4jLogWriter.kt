package com.hypergonial.chat

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

class Slf4jLogWriter : LogWriter() {
    @Suppress("CheckResult")
    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        val builder = LoggerFactory.getLogger(tag).atLevel(severity.slf4jLevel)

        if (throwable != null) {
            builder.setCause(throwable)
        }

        builder.log(message)
    }

    // No perfect match between the respective logging levels, but this might be close enough
    private val Severity.slf4jLevel: Level
        get() =
            when (this) {
                Severity.Verbose -> Level.TRACE
                Severity.Debug -> Level.DEBUG
                Severity.Info -> Level.INFO
                Severity.Warn -> Level.WARN
                Severity.Error -> Level.ERROR
                Severity.Assert -> Level.ERROR
            }
}
