package com.bookworms.app

import android.app.Application
import com.bookworms.app.data.auth.Session

// Runs before any screen, so the saved login is already there when the first screen asks for it.
class BookWormsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Session.init(this)
    }
}
