package org.unicornradio.app

import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Le squelette : une ligne au milieu de l'écran, pour prouver que la chaîne de build tient. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vue = TextView(this).apply {
            text = getString(R.string.bonjour, BuildConfig.VERSION_NAME)
            textSize = 20f
            gravity = Gravity.CENTER
        }
        setContentView(vue)
    }
}
