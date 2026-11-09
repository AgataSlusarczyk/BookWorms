package com.bookworms.app

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bookworms.app.ui.navigation.AppNavigation
import com.bookworms.app.ui.theme.BookWormsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark icons (clock, battery) on the status bar, because the app is always light.
        // Without this, they turn white in dark mode and you can't see them.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        hideNavigationBar()
        askForLocalNetworkInDebug()
        setContent {
            BookWormsTheme {
                AppNavigation()
            }
        }
    }

    // Debug builds talk to the server on our computer, which is on the local network. Since Android 16
    // that needs the user's permission (declared in src/debug/AndroidManifest.xml), or the connection
    // silently times out. scripts/run-android.sh grants it on its own; from Android Studio, tap Allow once.
    private fun askForLocalNetworkInDebug() {
        val permission = "android.permission.ACCESS_LOCAL_NETWORK"
        if (BuildConfig.DEBUG &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(permission), 0)
        }
    }

    // Hides the bar with the back/home buttons at the bottom of the screen.
    // A swipe up from the bottom edge shows it for a moment, then it hides again.
    private fun hideNavigationBar() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }
}
