package com.submask.app

import android.app.Activity
import android.os.Bundle
import com.submask.app.ui.MainScreen

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(MainScreen(this))
    }
}
