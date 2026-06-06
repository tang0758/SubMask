package com.submask.app.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class MainScreen(
    context: Context,
    initialOpacityPercent: Int,
    onOpacityChanged: (Int) -> Unit,
    onStartClicked: () -> Unit
) : LinearLayout(context) {
    private val statusText: TextView

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

        addView(TextView(context).apply {
            text = "遮挡透明度"
            textSize = 15f
            setTextColor(Color.rgb(24, 28, 32))
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 48
        })

        addView(SeekBar(context).apply {
            max = 100
            progress = initialOpacityPercent
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) onOpacityChanged(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        statusText = TextView(context).apply {
            text = "点击开始后会检查悬浮窗权限"
            textSize = 14f
            setTextColor(Color.rgb(96, 104, 112))
        }
        addView(statusText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 16
        })

        addView(Button(context).apply {
            text = "开始遮挡"
            setOnClickListener { onStartClicked() }
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = 32
        })
    }

    fun setPermissionStatus(hasPermission: Boolean) {
        statusText.text = if (hasPermission) {
            "悬浮窗权限已开启"
        } else {
            "需要开启“显示在其他应用上层”权限"
        }
    }
}
