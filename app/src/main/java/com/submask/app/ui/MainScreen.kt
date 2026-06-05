package com.submask.app.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainScreen(context: Context) : LinearLayout(context) {
    val opacitySeekBar: SeekBar
    val startButton: Button
    val statusText: TextView

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(48, 72, 48, 48)
        setBackgroundColor(Color.WHITE)

        addView(TextView(context).apply {
            text = "SubMask"
            textSize = 28f
            setTextColor(Color.rgb(24, 28, 32))
        })

        addView(TextView(context).apply {
            text = "字幕遮挡器"
            textSize = 16f
            setTextColor(Color.rgb(96, 104, 112))
        })

        opacitySeekBar = SeekBar(context).apply {
            max = 100
            progress = 72
        }
        addView(opacitySeekBar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 48
        })

        statusText = TextView(context).apply {
            text = "点击开始后会检查悬浮窗权限"
            textSize = 14f
            setTextColor(Color.rgb(96, 104, 112))
        }
        addView(statusText)

        startButton = Button(context).apply {
            text = "开始遮挡"
        }
        addView(startButton, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 32
        })
    }
}
