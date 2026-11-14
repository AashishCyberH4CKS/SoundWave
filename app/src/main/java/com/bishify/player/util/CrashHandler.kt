package com.bishify.player.util

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import com.bishify.player.ui.CrashActivity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler {
    private const val TAG = "CrashHandler"

    fun init(app: Application) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val fullStackTrace = sw.toString()

                Log.e(TAG, "FATAL CRASH INTERCEPTED:\n$fullStackTrace")

                // Save crash log to internal filesDir
                try {
                    val logFile = File(app.filesDir, "crash_log.txt")
                    logFile.writeText(fullStackTrace)
                } catch (_: Exception) {}

                // Launch CrashActivity
                val intent = Intent(app, CrashActivity::class.java).apply {
                    putExtra("error_type", throwable.javaClass.name)
                    putExtra("error_message", throwable.message ?: "No error message provided")
                    putExtra("stack_trace", fullStackTrace)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                app.startActivity(intent)

                // Cleanly kill the crashed process so Android OS doesn't show an OS crash dialog
                Process.killProcess(Process.myPid())
                System.exit(10)
            } catch (e: Exception) {
                Log.e(TAG, "Error in CrashHandler: ${e.message}")
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}

