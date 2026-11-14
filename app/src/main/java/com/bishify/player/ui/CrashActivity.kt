package com.bishify.player.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorType = intent.getStringExtra("error_type") ?: "Unknown Error"
        val errorMessage = intent.getStringExtra("error_message") ?: "An unexpected error occurred"
        val stackTrace = intent.getStringExtra("stack_trace") ?: "No stack trace available"

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#09090B"))
            setPadding(48, 72, 48, 48)
        }

        // Header Title
        val titleView = TextView(this).apply {
            text = "⚠️ SoundWave Diagnostic Mode"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        rootLayout.addView(titleView)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = "The app encountered an exception during startup. Please copy this crash log so we can resolve it instantly."
            textSize = 13f
            setTextColor(Color.parseColor("#A1A1AA"))
            setPadding(0, 16, 0, 32)
        }
        rootLayout.addView(subtitleView)

        // Error Type Pill
        val errorBadge = TextView(this).apply {
            text = "Error: $errorType\nMessage: $errorMessage"
            textSize = 14f
            setTextColor(Color.parseColor("#EF4444"))
            setBackgroundColor(Color.parseColor("#18181B"))
            setPadding(24, 24, 24, 24)
            typeface = Typeface.MONOSPACE
        }
        rootLayout.addView(errorBadge)

        // Action Buttons Row
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 32, 0, 32)
        }

        val copyButton = Button(this).apply {
            text = "📋 Copy Error Details"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("SoundWave Crash Log", "Error: $errorType\n$errorMessage\n\nStack Trace:\n$stackTrace")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@CrashActivity, "Copied to clipboard! Please paste this in chat.", Toast.LENGTH_LONG).show()
            }
        }
        buttonRow.addView(copyButton)

        val restartButton = Button(this).apply {
            text = "🔄 Restart App"
            setBackgroundColor(Color.parseColor("#27272A"))
            setTextColor(Color.WHITE)
            setPadding(24, 0, 24, 0)
            setOnClickListener {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(launchIntent)
                finish()
            }
        }
        buttonRow.addView(restartButton)

        rootLayout.addView(buttonRow)

        // Stack Trace Scroll Area
        val traceLabel = TextView(this).apply {
            text = "Full Stack Trace:"
            textSize = 12f
            setTextColor(Color.parseColor("#71717A"))
            setPadding(0, 0, 0, 12)
        }
        rootLayout.addView(traceLabel)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
            setBackgroundColor(Color.parseColor("#121215"))
            setPadding(24, 24, 24, 24)
        }

        val traceView = TextView(this).apply {
            text = stackTrace
            textSize = 11f
            setTextColor(Color.parseColor("#E4E4E7"))
            typeface = Typeface.MONOSPACE
        }
        scrollView.addView(traceView)
        rootLayout.addView(scrollView)

        setContentView(rootLayout)
    }
}

