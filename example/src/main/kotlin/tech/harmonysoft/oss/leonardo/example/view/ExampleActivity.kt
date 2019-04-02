package tech.harmonysoft.oss.leonardo.example.view

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_example.*
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.model.util.LeonardoEvents
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil
import java.util.*

class ExampleActivity : FragmentActivity() {

    private val darkThemeActive: Boolean
        get() {
            return TypedValue().let {
                application.theme.resolveAttribute(R.attr.theme_name, it, true)
                "dark" == it.string?.toString()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        initThemeSwitcher()
    }

    private fun initThemeSwitcher() {
        theme_switcher.setOnClickListener {
            if (darkThemeActive) {
                application.setTheme(R.style.AppTheme_Light)
                theme_switcher.setImageResource(R.drawable.ic_moon)
            } else {
                application.setTheme(R.style.AppTheme_Dark)
                theme_switcher.setImageResource(R.drawable.ic_sun)
            }

            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(LeonardoEvents.THEME_CHANGED))

            val color = LeonardoUtil.getColor(applicationContext, android.R.attr.windowBackground)
            doForChildren {
                it.setBackgroundColor(color)
            }
        }
    }

    private fun doForChildren(action: (View) -> Unit) {
        val toProcess = Stack<ViewGroup>()
        toProcess.push(content)
        while (!toProcess.isEmpty()) {
            val group = toProcess.pop()
            action(group)
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child is ViewGroup) {
                    toProcess.add(child)
                } else {
                    action(child)
                }
            }
        }
    }
}