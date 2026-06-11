package com.submask.app.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.submask.app.config.MaskRect
import com.submask.app.config.OverlayConfigStore
import com.submask.app.orientation.OrientationResolver
import com.submask.app.orientation.ScreenBounds
import com.submask.app.orientation.ScreenOrientation

class MainScreen(
    context: Context,
    private val configStore: OverlayConfigStore,
    private val screenBounds: ScreenBounds,
    private val onOverlayPermissionClicked: () -> Unit,
    private val onNotificationPermissionClicked: () -> Unit,
    private val onStartClicked: () -> Unit,
    private val onStopClicked: () -> Unit
) : FrameLayout(context) {

    private var currentOrientation = ScreenOrientation.PORTRAIT
    private lateinit var activeRect: MaskRect

    // UI elements
    private val overlayPermRow: LinearLayout
    private val overlayPermStatusText: TextView

    private val notificationPermRow: LinearLayout
    private val notificationPermStatusText: TextView
    private val notificationPermChevron: ImageView
    
    private val tabPortrait: TextView
    private val tabLandscape: TextView
    private val resetBtnLayout: LinearLayout
    
    private val previewContainer: MaskPreviewView
    
    private val opacityLabel: TextView
    private val opacitySeekBar: SeekBar

    // Sliders & Steppers
    private val widthSeekBar: SeekBar
    private val widthValueText: TextView
    private val widthMinusBtn: FrameLayout
    private val widthPlusBtn: FrameLayout
    
    private val heightSeekBar: SeekBar
    private val heightValueText: TextView
    private val heightMinusBtn: FrameLayout
    private val heightPlusBtn: FrameLayout
    
    private val bottomMarginSeekBar: SeekBar
    private val bottomMarginValueText: TextView
    private val bottomMarginMinusBtn: FrameLayout
    private val bottomMarginPlusBtn: FrameLayout
    
    private val xSeekBar: SeekBar
    private val xAlignDropdownText: TextView

    // Quick Layout Views and State
    private enum class QuickLayoutType {
        NONE,
        TIKTOK,
        YOUTUBE,
        LANDSCAPE_FULL
    }
    private var selectedQuickLayout = QuickLayoutType.NONE
    
    private val chipTiktok: TextView
    private val badgeTiktok: TextView
    private val chipYoutube: TextView
    private val badgeYoutube: TextView
    private val chipLandscape: TextView
    private val badgeLandscape: TextView

    private var isUpdatingSliders = false

    init {
        // Load initial active rect
        val config = configStore.loadOrientationConfig(currentOrientation, screenBounds)
        activeRect = config.rect

        // Root ScrollView
        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.rgb(247, 248, 250))
            isVerticalScrollBarEnabled = false
            fitsSystemWindows = true
        }
        addView(scrollView)

        // Main vertical container
        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(24))
        }
        scrollView.addView(mainContainer)

        // 1. App Bar
        val appBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(8))
        }
        mainContainer.addView(appBar)

        val titleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleContainer.addView(TextView(context).apply {
            text = "SubMask"
            textSize = 28f
            setTextColor(Color.rgb(32, 33, 36))
            paint.isFakeBoldText = true
        })
        appBar.addView(titleContainer)

        val menuButton = TextView(context).apply {
            text = "⋮"
            textSize = 24f
            setTextColor(Color.rgb(95, 99, 104))
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { showOverflowMenu(it) }
        }
        appBar.addView(menuButton)

        // 2. Permissions Card (Horizontal split)
        val permCard = createCard(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
        }
        mainContainer.addView(permCard, createCardLayoutParams())

        // Left Column: Overlay Permission
        overlayPermRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true
            isFocusable = true
            setOnClickListener { onOverlayPermissionClicked() }
        }
        permCard.addView(overlayPermRow)

        val overlayIcon = createCircularIcon(
            context,
            android.R.drawable.ic_menu_view,
            Color.rgb(32, 115, 102), // Teal color
            Color.WHITE
        )
        overlayPermRow.addView(overlayIcon)

        val overlayTextLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dpToPx(12)
            }
        }
        overlayTextLayout.addView(TextView(context).apply {
            text = "悬浮窗"
            textSize = 14f
            setTextColor(Color.rgb(32, 33, 36))
            paint.isFakeBoldText = true
        })
        overlayPermStatusText = TextView(context).apply {
            textSize = 12f
            paint.isFakeBoldText = true
        }
        overlayTextLayout.addView(overlayPermStatusText)
        overlayPermRow.addView(overlayTextLayout)

        // Thin vertical divider line
        val permDivider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(1), dpToPx(28)).apply {
                leftMargin = dpToPx(8)
                rightMargin = dpToPx(8)
            }
            setBackgroundColor(Color.rgb(230, 235, 240))
        }
        permCard.addView(permDivider)

        // Right Column: Notification Permission
        notificationPermRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f).apply {
                leftMargin = dpToPx(4)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onNotificationPermissionClicked() }
        }
        permCard.addView(notificationPermRow)

        val notifyIcon = createCircularIcon(
            context,
            android.R.drawable.ic_popup_reminder,
            Color.rgb(217, 142, 36), // Orange color
            Color.WHITE
        )
        notificationPermRow.addView(notifyIcon)

        val notifyTextLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(12)
                rightMargin = dpToPx(4)
            }
        }
        notifyTextLayout.addView(TextView(context).apply {
            text = "通知"
            textSize = 14f
            setTextColor(Color.rgb(32, 33, 36))
            paint.isFakeBoldText = true
        })
        notificationPermStatusText = TextView(context).apply {
            textSize = 12f
            paint.isFakeBoldText = true
        }
        notifyTextLayout.addView(notificationPermStatusText)
        notificationPermRow.addView(notifyTextLayout)

        notificationPermChevron = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(16), dpToPx(16))
            setImageResource(android.R.drawable.ic_media_play)
            setColorFilter(Color.rgb(128, 134, 139))
        }
        notificationPermRow.addView(notificationPermChevron)

        // 3. Action Buttons Section (开启遮挡 & 关闭)
        val actionButtonsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        mainContainer.addView(actionButtonsContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dpToPx(16)
        })

        // 开启遮挡 (Weight 3)
        val startButton = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackground(GradientDrawable().apply {
                setColor(Color.rgb(32, 142, 121))
                cornerRadius = dpToPx(8).toFloat()
            })
            setPadding(0, dpToPx(14), 0, dpToPx(14))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f).apply {
                rightMargin = dpToPx(8)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onStartClicked() }
        }
        actionButtonsContainer.addView(startButton)

        val playIcon = TextView(context).apply {
            text = "▶"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 0, dpToPx(8), 0)
        }
        startButton.addView(playIcon)

        startButton.addView(TextView(context).apply {
            text = "开启遮挡"
            textSize = 18f
            setTextColor(Color.WHITE)
            paint.isFakeBoldText = true
        })

        // 关闭 (Weight 1)
        val stopButton = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackground(GradientDrawable().apply {
                setColor(Color.rgb(197, 34, 31)) // Red color
                cornerRadius = dpToPx(8).toFloat()
            })
            setPadding(0, dpToPx(14), 0, dpToPx(14))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true
            isFocusable = true
            setOnClickListener { onStopClicked() }
        }
        actionButtonsContainer.addView(stopButton)

        stopButton.addView(TextView(context).apply {
            text = "关闭"
            textSize = 18f
            setTextColor(Color.WHITE)
            paint.isFakeBoldText = true
        })

        // 4. Opacity Card
        val opacityCard = createCard(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
        }
        mainContainer.addView(opacityCard, createCardLayoutParams())

        val opacityHeaderRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        opacityCard.addView(opacityHeaderRow)

        opacityLabel = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.rgb(32, 33, 36))
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        opacityHeaderRow.addView(opacityLabel)

        val resetOpacityLink = TextView(context).apply {
            text = "重置"
            textSize = 13f
            setTextColor(Color.rgb(32, 142, 121))
            paint.isFakeBoldText = true
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener {
                configStore.setOpacity(0.72f)
                opacitySeekBar.progress = ((0.72f - 0.20f) * 100).toInt()
                opacityLabel.text = "遮挡透明度  72%"
                updatePreview()
            }
        }
        opacityHeaderRow.addView(resetOpacityLink)

        val sliderWrapper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(6), 0, dpToPx(2))
        }
        opacityCard.addView(sliderWrapper)

        sliderWrapper.addView(TextView(context).apply {
            text = "0%"
            textSize = 12f
            setTextColor(Color.rgb(128, 134, 139))
        })

        opacitySeekBar = SeekBar(context).apply {
            max = 80 // (100% - 20%)
            progress = ((configStore.getOpacity() - 0.20f) * 100).toInt()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val op = (progress / 100f) + 0.20f
                    opacityLabel.text = "遮挡透明度  ${(op * 100).toInt()}%"
                    if (fromUser) {
                        configStore.setOpacity(op)
                        updatePreview()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
        sliderWrapper.addView(opacitySeekBar)

        sliderWrapper.addView(TextView(context).apply {
            text = "100%"
            textSize = 12f
            setTextColor(Color.rgb(128, 134, 139))
        })

        opacityLabel.text = "遮挡透明度  ${(configStore.getOpacity() * 100).toInt()}%"

        // 5. Tabs & Reset Row (Segmented tab selector and refresh next to it)
        val tabsRowLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        mainContainer.addView(tabsRowLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dpToPx(12)
        })

        val tabsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackground(GradientDrawable().apply {
                setColor(Color.rgb(241, 243, 244))
                cornerRadius = dpToPx(8).toFloat()
            })
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(44), 1f)
        }
        tabsRowLayout.addView(tabsLayout)

        tabPortrait = TextView(context).apply {
            text = "📱 竖屏"
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener { selectOrientation(ScreenOrientation.PORTRAIT) }
        }
        tabsLayout.addView(tabPortrait)

        tabLandscape = TextView(context).apply {
            text = "▭ 横屏"
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener { selectOrientation(ScreenOrientation.LANDSCAPE) }
        }
        tabsLayout.addView(tabLandscape)

        // Reset Button next to Tabs
        resetBtnLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), 0, dpToPx(8), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { resetToDefault() }
        }
        tabsRowLayout.addView(resetBtnLayout)

        resetBtnLayout.addView(TextView(context).apply {
            text = "↻"
            textSize = 18f
            setTextColor(Color.rgb(32, 33, 36))
            paint.isFakeBoldText = true
        })
        resetBtnLayout.addView(TextView(context).apply {
            text = "重置"
            textSize = 10f
            setTextColor(Color.rgb(32, 33, 36))
            paint.isFakeBoldText = true
        })

        // 6. Interactive Preview Card (Spacious widescreen aspect ratio)
        val previewCard = createCard(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }
        mainContainer.addView(previewCard, createCardLayoutParams())

        previewContainer = MaskPreviewView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(210))
            onRectUpdated = { newRect ->
                activeRect = newRect
                selectedQuickLayout = QuickLayoutType.NONE
                updateQuickLayoutChipStyles()
                // Save config immediately
                val config = configStore.loadOrientationConfig(currentOrientation, screenBounds)
                configStore.saveOrientationConfig(currentOrientation, config.copy(rect = newRect))
                // Update sliders to match
                updateSlidersAndLabels()
            }
        }
        previewCard.addView(previewContainer)

        // 7. Direct Sliders Control Panel Card
        val controlCard = createCard(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }
        mainContainer.addView(controlCard, createCardLayoutParams())

        // Width Row
        val widthRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controlCard.addView(widthRow, createSliderLayoutParams())

        widthRow.addView(TextView(context).apply {
            text = "宽度"
            textSize = 14f
            setTextColor(Color.rgb(60, 64, 67))
            layoutParams = LinearLayout.LayoutParams(dpToPx(64), LinearLayout.LayoutParams.WRAP_CONTENT)
        })

        widthSeekBar = SeekBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(GeometryChangeListener())
        }
        widthRow.addView(widthSeekBar)

        val widthStepper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        widthRow.addView(widthStepper)

        widthMinusBtn = createStepperButton(context, "-") { adjustWidth(-1) }
        widthStepper.addView(widthMinusBtn)

        widthValueText = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.rgb(32, 33, 36))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), LinearLayout.LayoutParams.WRAP_CONTENT)
            paint.isFakeBoldText = true
        }
        widthStepper.addView(widthValueText)

        widthPlusBtn = createStepperButton(context, "+") { adjustWidth(1) }
        widthStepper.addView(widthPlusBtn)

        // Height Row
        val heightRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controlCard.addView(heightRow, createSliderLayoutParams())

        heightRow.addView(TextView(context).apply {
            text = "高度"
            textSize = 14f
            setTextColor(Color.rgb(60, 64, 67))
            layoutParams = LinearLayout.LayoutParams(dpToPx(64), LinearLayout.LayoutParams.WRAP_CONTENT)
        })

        heightSeekBar = SeekBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(GeometryChangeListener())
        }
        heightRow.addView(heightSeekBar)

        val heightStepper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        heightRow.addView(heightStepper)

        heightMinusBtn = createStepperButton(context, "-") { adjustHeight(-1) }
        heightStepper.addView(heightMinusBtn)

        heightValueText = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.rgb(32, 33, 36))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), LinearLayout.LayoutParams.WRAP_CONTENT)
            paint.isFakeBoldText = true
        }
        heightStepper.addView(heightValueText)

        heightPlusBtn = createStepperButton(context, "+") { adjustHeight(1) }
        heightStepper.addView(heightPlusBtn)

        // Bottom Margin Row
        val bottomMarginRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controlCard.addView(bottomMarginRow, createSliderLayoutParams())

        bottomMarginRow.addView(TextView(context).apply {
            text = "底部距离"
            textSize = 14f
            setTextColor(Color.rgb(60, 64, 67))
            layoutParams = LinearLayout.LayoutParams(dpToPx(64), LinearLayout.LayoutParams.WRAP_CONTENT)
        })

        bottomMarginSeekBar = SeekBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(GeometryChangeListener())
        }
        bottomMarginRow.addView(bottomMarginSeekBar)

        val marginStepper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        bottomMarginRow.addView(marginStepper)

        bottomMarginMinusBtn = createStepperButton(context, "-") { adjustMargin(-1) }
        marginStepper.addView(bottomMarginMinusBtn)

        bottomMarginValueText = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.rgb(32, 33, 36))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(44), LinearLayout.LayoutParams.WRAP_CONTENT)
            paint.isFakeBoldText = true
        }
        marginStepper.addView(bottomMarginValueText)

        bottomMarginPlusBtn = createStepperButton(context, "+") { adjustMargin(1) }
        marginStepper.addView(bottomMarginPlusBtn)

        // Horizontal Position Row
        val xRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controlCard.addView(xRow, createSliderLayoutParams())

        xRow.addView(TextView(context).apply {
            text = "水平位置"
            textSize = 14f
            setTextColor(Color.rgb(60, 64, 67))
            layoutParams = LinearLayout.LayoutParams(dpToPx(64), LinearLayout.LayoutParams.WRAP_CONTENT)
        })

        xSeekBar = SeekBar(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(GeometryChangeListener())
        }
        xRow.addView(xSeekBar)

        // Align Dropdown spinner/text button
        xAlignDropdownText = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.rgb(32, 33, 36))
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            paint.isFakeBoldText = true
            isClickable = true
            isFocusable = true
            setOnClickListener { showAlignDropdownMenu(it) }
        }
        xRow.addView(xAlignDropdownText, LinearLayout.LayoutParams(dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT))

        // Divider line
        controlCard.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)).apply {
                topMargin = dpToPx(12)
                bottomMargin = dpToPx(12)
            }
            setBackgroundColor(Color.rgb(241, 243, 244))
        })

        controlCard.addView(TextView(context).apply {
            text = "快捷布局"
            textSize = 14f
            setTextColor(Color.rgb(32, 33, 36))
            paint.isFakeBoldText = true
            setPadding(0, 0, 0, dpToPx(8))
        })

        val chipsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controlCard.addView(chipsLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dpToPx(8)
        })

        // 1. TikTok Chip
        val flTiktok = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        chipsLayout.addView(flTiktok)

        chipTiktok = TextView(context).apply {
            text = "TikTok 全屏"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(8), 0, dpToPx(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { selectQuickLayout(QuickLayoutType.TIKTOK) }
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(38)).apply {
                topMargin = dpToPx(6)
                rightMargin = dpToPx(6)
            }
        }
        flTiktok.addView(chipTiktok)

        badgeTiktok = createCheckmarkBadge(context)
        flTiktok.addView(badgeTiktok)

        // 2. YouTube Chip
        val flYoutube = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        chipsLayout.addView(flYoutube)

        chipYoutube = TextView(context).apply {
            text = "YouTube 半屏"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(8), 0, dpToPx(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { selectQuickLayout(QuickLayoutType.YOUTUBE) }
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(38)).apply {
                topMargin = dpToPx(6)
                rightMargin = dpToPx(6)
            }
        }
        flYoutube.addView(chipYoutube)

        badgeYoutube = createCheckmarkBadge(context)
        flYoutube.addView(badgeYoutube)

        // 3. Landscape Chip
        val flLandscape = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        chipsLayout.addView(flLandscape)

        chipLandscape = TextView(context).apply {
            text = "横屏全屏"
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(8), 0, dpToPx(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { selectQuickLayout(QuickLayoutType.LANDSCAPE_FULL) }
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(38)).apply {
                topMargin = dpToPx(6)
                rightMargin = dpToPx(6)
            }
        }
        flLandscape.addView(chipLandscape)

        badgeLandscape = createCheckmarkBadge(context)
        flLandscape.addView(badgeLandscape)

        // Helper label at the bottom of the card
        controlCard.addView(TextView(context).apply {
            text = "拖拽预览中的遮挡区域或手柄可调整大小和位置"
            textSize = 11f
            setTextColor(Color.rgb(150, 155, 160))
            gravity = Gravity.CENTER_HORIZONTAL
        })

        // Initial tab load
        selectOrientation(ScreenOrientation.PORTRAIT)
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(context, anchor)
        popup.menu.add("设置")
        popup.menu.add("使用帮助")
        popup.menu.add("关于")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "设置" -> {
                    Toast.makeText(context, "设置功能将在后续版本中支持", Toast.LENGTH_SHORT).show()
                }
                "使用帮助" -> {
                    AlertDialog.Builder(context)
                        .setTitle("使用帮助")
                        .setMessage("1. 首次使用需要开启“悬浮窗权限”及“通知权限”。\n2. 点击“开始遮挡”后，屏幕将浮现遮挡条。\n3. 您可以直接在预览框中拖动遮挡区或手柄来调整它的大小与位置，也可以在下方使用滑条和加减按钮精调。\n4. 点击右上角“重置”即可复原当前的配置。\n5. 点击遮挡条左上角的关闭按钮或点击通知栏“关闭遮挡”即可退出。")
                        .setPositiveButton("确定", null)
                        .show()
                }
                "关于" -> {
                    AlertDialog.Builder(context)
                        .setTitle("关于 SubMask")
                        .setMessage("SubMask 字幕遮挡器\n版本: 0.1.0\n\n一款轻量快捷的本地视频字幕遮挡工具，帮助大家在看视频学习英语时进行听力锻炼。")
                        .setPositiveButton("确定", null)
                        .show()
                }
            }
            true
        }
        popup.show()
    }

    private fun selectOrientation(orientation: ScreenOrientation) {
        currentOrientation = orientation
        selectedQuickLayout = QuickLayoutType.NONE
        updateQuickLayoutChipStyles()
        updateTabStyles()

        // Reload configuration
        val config = configStore.loadOrientationConfig(currentOrientation, screenBounds)
        activeRect = config.rect
        updateSlidersAndLabels()
    }

    private fun updateTabStyles() {
        val activeBg = GradientDrawable().apply {
            setColor(Color.rgb(230, 242, 240))
            cornerRadius = dpToPx(6).toFloat()
            setStroke(dpToPx(1), Color.rgb(32, 142, 121))
        }
        val inactiveBg = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
        }
        if (currentOrientation == ScreenOrientation.PORTRAIT) {
            tabPortrait.background = activeBg
            tabPortrait.setTextColor(Color.rgb(32, 142, 121))
            tabPortrait.paint.isFakeBoldText = true
            
            tabLandscape.background = inactiveBg
            tabLandscape.setTextColor(Color.rgb(95, 99, 104))
            tabLandscape.paint.isFakeBoldText = false
        } else {
            tabLandscape.background = activeBg
            tabLandscape.setTextColor(Color.rgb(32, 142, 121))
            tabLandscape.paint.isFakeBoldText = true
            
            tabPortrait.background = inactiveBg
            tabPortrait.setTextColor(Color.rgb(95, 99, 104))
            tabPortrait.paint.isFakeBoldText = false
        }
    }

    private fun resetToDefault() {
        val simW = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.max(screenBounds.width, screenBounds.height)
        } else {
            Math.min(screenBounds.width, screenBounds.height)
        }
        val simH = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.min(screenBounds.width, screenBounds.height)
        } else {
            Math.max(screenBounds.width, screenBounds.height)
        }
        val simBounds = ScreenBounds(simW, simH)
        val defaultRect = OrientationResolver.defaultRect(currentOrientation, simBounds)
        
        activeRect = defaultRect
        
        val config = configStore.loadOrientationConfig(currentOrientation, screenBounds)
        configStore.saveOrientationConfig(currentOrientation, config.copy(rect = defaultRect))
        
        updateSlidersAndLabels()
        val name = if (currentOrientation == ScreenOrientation.PORTRAIT) "竖屏" else "横屏"
        Toast.makeText(context, "已重置${name}位置大小", Toast.LENGTH_SHORT).show()
    }

    private fun updateSlidersAndLabels() {
        isUpdatingSliders = true
        
        val simW = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.max(screenBounds.width, screenBounds.height)
        } else {
            Math.min(screenBounds.width, screenBounds.height)
        }
        val simH = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.min(screenBounds.width, screenBounds.height)
        } else {
            Math.max(screenBounds.width, screenBounds.height)
        }

        // 1. Width
        val maxW = simW - OverlayConfigStore.MIN_MASK_WIDTH
        widthSeekBar.max = maxW
        val progW = (activeRect.width - OverlayConfigStore.MIN_MASK_WIDTH).coerceIn(0, maxW)
        widthSeekBar.progress = progW
        val wPercent = (activeRect.width.toFloat() / simW * 100).toInt().coerceIn(0, 100)
        widthValueText.text = "${wPercent}%"

        // 2. Height
        val maxH = simH - OverlayConfigStore.MIN_MASK_HEIGHT
        heightSeekBar.max = maxH
        val progH = (activeRect.height - OverlayConfigStore.MIN_MASK_HEIGHT).coerceIn(0, maxH)
        heightSeekBar.progress = progH
        val hPercent = (activeRect.height.toFloat() / simH * 100).toInt().coerceIn(0, 100)
        heightValueText.text = "${hPercent}%"

        // 3. Bottom Margin
        val maxBottom = (simH - activeRect.height).coerceAtLeast(0)
        bottomMarginSeekBar.max = maxBottom
        val bottomVal = (simH - activeRect.y - activeRect.height).coerceIn(0, maxBottom)
        bottomMarginSeekBar.progress = bottomVal
        val mPercent = (bottomVal.toFloat() / simH * 100).toInt().coerceIn(0, 100)
        bottomMarginValueText.text = "${mPercent}%"

        // 4. X Horizontal Position
        val maxX = (simW - activeRect.width).coerceAtLeast(0)
        xSeekBar.max = maxX
        xSeekBar.progress = activeRect.x.coerceIn(0, maxX)

        val centerDiff = Math.abs(activeRect.x - (simW - activeRect.width) / 2)
        xAlignDropdownText.text = if (centerDiff <= 8) {
            "居中  ▾"
        } else if (activeRect.x <= 8) {
            "靠左  ▾"
        } else if (activeRect.x >= maxX - 8) {
            "靠右  ▾"
        } else {
            "自定义  ▾"
        }

        isUpdatingSliders = false

        updatePreview()
    }

    private fun updatePreview() {
        val op = (opacitySeekBar.progress / 100f) + 0.20f
        previewContainer.update(
            rect = activeRect,
            bounds = screenBounds,
            landscape = (currentOrientation == ScreenOrientation.LANDSCAPE),
            currentOpacity = op
        )
    }

    private fun showAlignDropdownMenu(anchor: View) {
        val simW = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.max(screenBounds.width, screenBounds.height)
        } else {
            Math.min(screenBounds.width, screenBounds.height)
        }
        val maxX = (simW - activeRect.width).coerceAtLeast(0)

        val popup = PopupMenu(context, anchor)
        popup.menu.add("居中")
        popup.menu.add("靠左")
        popup.menu.add("靠右")
        popup.setOnMenuItemClickListener { item ->
            val newX = when (item.title) {
                "居中" -> (simW - activeRect.width) / 2
                "靠左" -> 0
                "靠右" -> maxX
                else -> activeRect.x
            }
            
            val updatedRect = activeRect.copy(x = newX)
            val config = configStore.loadOrientationConfig(currentOrientation, screenBounds)
            configStore.saveOrientationConfig(currentOrientation, config.copy(rect = updatedRect))
            
            activeRect = updatedRect
            updateSlidersAndLabels()
            true
        }
        popup.show()
    }

    // Adjustments with +/- steppers by 1% of screen size
    private fun adjustWidth(step: Int) {
        val simW = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.max(screenBounds.width, screenBounds.height)
        } else {
            Math.min(screenBounds.width, screenBounds.height)
        }
        val delta = (simW * 0.01f).toInt().coerceAtLeast(1) * step
        val newW = (activeRect.width + delta).coerceIn(OverlayConfigStore.MIN_MASK_WIDTH, simW)
        
        val updated = activeRect.copy(width = newW).clampTo(
            bounds = ScreenBounds(simW, screenBounds.height), // Keep sim height safe
            minWidth = OverlayConfigStore.MIN_MASK_WIDTH,
            minHeight = OverlayConfigStore.MIN_MASK_HEIGHT
        )
        
        saveGeometryUpdate(updated)
    }

    private fun adjustHeight(step: Int) {
        val simH = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.min(screenBounds.width, screenBounds.height)
        } else {
            Math.max(screenBounds.width, screenBounds.height)
        }
        val delta = (simH * 0.01f).toInt().coerceAtLeast(1) * step
        val newH = (activeRect.height + delta).coerceIn(OverlayConfigStore.MIN_MASK_HEIGHT, simH)
        
        val updated = activeRect.copy(height = newH).clampTo(
            bounds = ScreenBounds(screenBounds.width, simH),
            minWidth = OverlayConfigStore.MIN_MASK_WIDTH,
            minHeight = OverlayConfigStore.MIN_MASK_HEIGHT
        )
        
        saveGeometryUpdate(updated)
    }

    private fun adjustMargin(step: Int) {
        val simH = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.min(screenBounds.width, screenBounds.height)
        } else {
            Math.max(screenBounds.width, screenBounds.height)
        }
        val delta = (simH * 0.01f).toInt().coerceAtLeast(1) * step
        
        val currentMargin = simH - activeRect.y - activeRect.height
        val newMargin = (currentMargin + delta).coerceIn(0, simH - activeRect.height)
        val newY = simH - activeRect.height - newMargin
        
        val updated = activeRect.copy(y = newY).clampTo(
            bounds = ScreenBounds(screenBounds.width, simH),
            minWidth = OverlayConfigStore.MIN_MASK_WIDTH,
            minHeight = OverlayConfigStore.MIN_MASK_HEIGHT
        )
        
        saveGeometryUpdate(updated)
    }

    private fun saveGeometryUpdate(updated: MaskRect) {
        activeRect = updated
        selectedQuickLayout = QuickLayoutType.NONE
        updateQuickLayoutChipStyles()
        val config = configStore.loadOrientationConfig(currentOrientation, screenBounds)
        configStore.saveOrientationConfig(currentOrientation, config.copy(rect = updated))
        updateSlidersAndLabels()
    }

    private inner class GeometryChangeListener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (!fromUser || isUpdatingSliders) return

            val simW = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
                Math.max(screenBounds.width, screenBounds.height)
            } else {
                Math.min(screenBounds.width, screenBounds.height)
            }
            val simH = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
                Math.min(screenBounds.width, screenBounds.height)
            } else {
                Math.max(screenBounds.width, screenBounds.height)
            }

            var w = activeRect.width
            var h = activeRect.height
            var x = activeRect.x
            var y = activeRect.y

            when (seekBar) {
                widthSeekBar -> {
                    w = progress + OverlayConfigStore.MIN_MASK_WIDTH
                }
                heightSeekBar -> {
                    h = progress + OverlayConfigStore.MIN_MASK_HEIGHT
                }
                bottomMarginSeekBar -> {
                    val bottomMargin = progress
                    y = simH - h - bottomMargin
                }
                xSeekBar -> {
                    x = progress
                }
            }

            val rawRect = MaskRect(x = x, y = y, width = w, height = h)
            val clamped = rawRect.clampTo(ScreenBounds(simW, simH), OverlayConfigStore.MIN_MASK_WIDTH, OverlayConfigStore.MIN_MASK_HEIGHT)

            activeRect = clamped
            selectedQuickLayout = QuickLayoutType.NONE
            updateQuickLayoutChipStyles()

            // Save config
            val config = configStore.loadOrientationConfig(currentOrientation, screenBounds)
            configStore.saveOrientationConfig(currentOrientation, config.copy(rect = clamped))

            updateSlidersAndLabels()
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    fun setPermissionStatus(hasOverlayPermission: Boolean, hasNotificationPermission: Boolean) {
        val activeGreen = Color.rgb(19, 115, 51)
        val inactiveOrange = Color.rgb(217, 119, 6)

        // Overlay status
        if (hasOverlayPermission) {
            overlayPermStatusText.text = "已授权"
            overlayPermStatusText.setTextColor(activeGreen)
        } else {
            overlayPermStatusText.text = "未授权"
            overlayPermStatusText.setTextColor(inactiveOrange)
        }

        // Notification status
        if (hasNotificationPermission) {
            notificationPermStatusText.text = "已授权"
            notificationPermStatusText.setTextColor(activeGreen)
            notificationPermChevron.visibility = View.GONE
        } else {
            notificationPermStatusText.text = "未授权"
            notificationPermStatusText.setTextColor(inactiveOrange)
            notificationPermChevron.visibility = View.VISIBLE
        }
    }

    // Helper functions
    private fun createStepperButton(context: Context, label: String, onClick: () -> Unit): FrameLayout {
        val container = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28))
            setBackground(GradientDrawable().apply {
                setColor(Color.rgb(241, 243, 244))
                cornerRadius = dpToPx(4).toFloat()
            })
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val textView = TextView(context).apply {
            text = label
            textSize = 15f
            setTextColor(Color.rgb(95, 99, 104))
            gravity = Gravity.CENTER
            paint.isFakeBoldText = true
        }
        container.addView(textView)
        return container
    }

    private fun createCircularIcon(context: Context, resId: Int, bgColor: Int, iconColor: Int): FrameLayout {
        val container = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(38), dpToPx(38))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }
        }
        val imageView = ImageView(context).apply {
            setImageResource(resId)
            setColorFilter(iconColor)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }
        container.addView(imageView)
        return container
    }

    private fun createCheckmarkBadge(context: Context): TextView {
        return TextView(context).apply {
            text = "✓"
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(32, 142, 121))
            }
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(dpToPx(16), dpToPx(16)).apply {
                gravity = Gravity.TOP or Gravity.END
            }
        }
    }

    private fun selectQuickLayout(type: QuickLayoutType) {
        if (selectedQuickLayout == type) return
        selectedQuickLayout = type

        // Parameters map:
        // TikTok: Portrait, Width 92%, Height 14%, Bottom Margin 8%, Centered
        // YouTube: Portrait, Width 88%, Height 10%, Bottom Margin 52%, Centered
        // Landscape Full: Landscape, Width 86%, Height 12%, Bottom Margin 8%, Centered

        val targetOrientation = when (type) {
            QuickLayoutType.TIKTOK -> ScreenOrientation.PORTRAIT
            QuickLayoutType.YOUTUBE -> ScreenOrientation.PORTRAIT
            QuickLayoutType.LANDSCAPE_FULL -> ScreenOrientation.LANDSCAPE
            QuickLayoutType.NONE -> currentOrientation
        }

        currentOrientation = targetOrientation
        updateTabStyles()

        val simW = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.max(screenBounds.width, screenBounds.height)
        } else {
            Math.min(screenBounds.width, screenBounds.height)
        }
        val simH = if (currentOrientation == ScreenOrientation.LANDSCAPE) {
            Math.min(screenBounds.width, screenBounds.height)
        } else {
            Math.max(screenBounds.width, screenBounds.height)
        }

        val (wPct, hPct, mPct) = when (type) {
            QuickLayoutType.TIKTOK -> Triple(0.92f, 0.14f, 0.08f)
            QuickLayoutType.YOUTUBE -> Triple(0.88f, 0.10f, 0.52f)
            QuickLayoutType.LANDSCAPE_FULL -> Triple(0.86f, 0.12f, 0.08f)
            QuickLayoutType.NONE -> return
        }

        val targetW = (simW * wPct).toInt().coerceIn(OverlayConfigStore.MIN_MASK_WIDTH, simW)
        val targetH = (simH * hPct).toInt().coerceIn(OverlayConfigStore.MIN_MASK_HEIGHT, simH)
        val targetMargin = (simH * mPct).toInt().coerceIn(0, simH - targetH)

        val targetX = (simW - targetW) / 2
        val targetY = simH - targetH - targetMargin

        val newRect = MaskRect(x = targetX, y = targetY, width = targetW, height = targetH)
        activeRect = newRect

        val config = configStore.loadOrientationConfig(currentOrientation, screenBounds)
        configStore.saveOrientationConfig(currentOrientation, config.copy(rect = newRect))

        updateSlidersAndLabels()
        updateQuickLayoutChipStyles()
    }

    private fun updateQuickLayoutChipStyles() {
        val activeBg = GradientDrawable().apply {
            setColor(Color.rgb(32, 115, 102)) // Teal background
            cornerRadius = dpToPx(6).toFloat()
        }
        val inactiveBg = GradientDrawable().apply {
            setColor(Color.rgb(241, 243, 244))
            cornerRadius = dpToPx(6).toFloat()
        }

        // Apply background and text color, update badge visibility
        if (selectedQuickLayout == QuickLayoutType.TIKTOK) {
            chipTiktok.background = activeBg
            chipTiktok.setTextColor(Color.WHITE)
            badgeTiktok.visibility = View.VISIBLE
        } else {
            chipTiktok.background = inactiveBg
            chipTiktok.setTextColor(Color.rgb(95, 99, 104))
            badgeTiktok.visibility = View.GONE
        }

        if (selectedQuickLayout == QuickLayoutType.YOUTUBE) {
            chipYoutube.background = activeBg
            chipYoutube.setTextColor(Color.WHITE)
            badgeYoutube.visibility = View.VISIBLE
        } else {
            chipYoutube.background = inactiveBg
            chipYoutube.setTextColor(Color.rgb(95, 99, 104))
            badgeYoutube.visibility = View.GONE
        }

        if (selectedQuickLayout == QuickLayoutType.LANDSCAPE_FULL) {
            chipLandscape.background = activeBg
            chipLandscape.setTextColor(Color.WHITE)
            badgeLandscape.visibility = View.VISIBLE
        } else {
            chipLandscape.background = inactiveBg
            chipLandscape.setTextColor(Color.rgb(95, 99, 104))
            badgeLandscape.visibility = View.GONE
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun createCard(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            setBackground(GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dpToPx(8).toFloat()
            })
            val strokeColor = Color.rgb(230, 235, 240)
            (background as GradientDrawable).setStroke(dpToPx(1), strokeColor)
        }
    }

    private fun createCardLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dpToPx(12)
        }
    }

    private fun createSliderLayoutParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(6)
            bottomMargin = dpToPx(6)
        }
    }
}
