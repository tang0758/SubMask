package com.submask.app.ui

object HelpContent {
    val quickStartSteps = listOf(
        "1. 开启悬浮窗和通知权限。",
        "2. 选择 TikTok 全屏、YouTube 半屏或横屏全屏，也可以手动调整宽度、高度和位置。",
        "3. 点击开始遮挡，然后回到视频 App。",
        "4. 调整好遮挡条后点击锁定，避免误拖动。"
    )

    val faqItems = listOf(
        FaqItem(
            question = "运行中怎么调透明度？",
            answer = "解锁遮挡条，点击百分比按钮，拖动滑杆即可。"
        ),
        FaqItem(
            question = "怎么关闭遮挡？",
            answer = "可以点遮挡条左上角关闭，或用通知栏关闭。"
        ),
        FaqItem(
            question = "快捷设置磁贴有什么用？",
            answer = "添加 SubMask 磁贴后，可以从下拉快捷设置快速开关遮挡。"
        ),
        FaqItem(
            question = "横竖屏会互相影响吗？",
            answer = "不会，竖屏和横屏会分别保存位置和大小。"
        )
    )
}

data class FaqItem(
    val question: String,
    val answer: String
)
