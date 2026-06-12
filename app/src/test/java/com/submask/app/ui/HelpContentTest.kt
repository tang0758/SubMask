package com.submask.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class HelpContentTest {
    @Test
    fun `help content contains quick start and faq sections`() {
        assertTrue(HelpContent.quickStartSteps.any { it.contains("开启悬浮窗") })
        assertTrue(HelpContent.quickStartSteps.any { it.contains("开始遮挡") })
        assertTrue(HelpContent.faqItems.any { it.question.contains("运行中怎么调透明度") })
        assertTrue(HelpContent.faqItems.any { it.question.contains("横竖屏会互相影响吗") })
    }
}
