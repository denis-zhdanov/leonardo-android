package tech.harmonysoft.oss.leonardo.example.view

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_example.*
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.event.LeonardoEvents
import tech.harmonysoft.oss.leonardo.example.event.LeonardoKey
import tech.harmonysoft.oss.leonardo.example.settings.PREFERENCE_NAME
import tech.harmonysoft.oss.leonardo.example.settings.PreferenceKey
import tech.harmonysoft.oss.leonardo.example.settings.PreferenceValue
import tech.harmonysoft.oss.leonardo.model.util.LeonardoUtil

@SuppressLint("InflateParams")
class ExampleActivity : AppCompatActivity() {

    private val staticChartView: View by lazy {
        layoutInflater.inflate(R.layout.layout_static_chart, null)
    }

    private val infiniteChartView: View by lazy {
        layoutInflater.inflate(R.layout.layout_infinite_chart, null)
    }

    private val darkThemeActive: Boolean
        get() {
            return TypedValue().let {
                theme.resolveAttribute(R.attr.theme_name, it, true)
                "dark" == it.string?.toString()
            }
        }

    private val preferences: SharedPreferences get() = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)
        restoreState()
        setupActionBar()
        initDrawer()
        initThemeSwitcher()
        initThemeReload()
    }

    private fun setupActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }
    }

    private fun initDrawer() {
        navigation.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            drawer_layout.closeDrawers()
            val chartViewType = when (menuItem.itemId) {
                R.id.static_chart -> PreferenceValue.STATIC_CHART
                R.id.infinite_chart -> PreferenceValue.INFINITE_CHART
                else -> null
            }
            if (chartViewType != null) {
                mayBeChangeChartContent(chartViewType)
            }
            true
        }
    }

    private fun mayBeChangeChartContent(viewType: String) {
        val viewToShow = if (viewType == PreferenceValue.INFINITE_CHART) {
            infiniteChartView
        } else {
            staticChartView
        }
        if (chart_content.childCount == 0) {
            chart_content.addView(viewToShow)
        } else if (chart_content.getChildAt(0) != viewToShow) {
            chart_content.removeAllViews()
            chart_content.addView(viewToShow)
        }

        val menuItemId = if (viewType == PreferenceValue.STATIC_CHART) {
            R.id.static_chart
        } else {
            R.id.infinite_chart
        }
        navigation.menu.findItem(menuItemId).isChecked = true

        preferences.edit {
            putString(PreferenceKey.ACTIVE_CHART, viewType)
        }
    }

    private fun initThemeSwitcher() {
        theme_switcher.setOnClickListener {
            val currentStyle = if (darkThemeActive) {
                setTheme(R.style.AppTheme_Light)
                theme_switcher.setImageResource(R.drawable.ic_moon)
                R.style.Charts_Light
            } else {
                setTheme(R.style.AppTheme_Dark)
                theme_switcher.setImageResource(R.drawable.ic_sun)
                R.style.Charts_Dark
            }

            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(LeonardoEvents.THEME_CHANGED).apply {
                putExtra(LeonardoKey.CHART_STYLE, currentStyle)
            })
        }
    }

    private fun initThemeReload() {
        val activity = this
        LocalBroadcastManager.getInstance(this).registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val style = intent.getIntExtra(LeonardoKey.CHART_STYLE, R.style.Charts_Light)
                drawer_layout.setBackgroundColor(LeonardoUtil.getColor(activity, style, android.R.attr.windowBackground))
                toolbar.setTitleTextColor(LeonardoUtil.getColor(activity, style, R.attr.action_bar_text_color))
                toolbar.setBackgroundColor(LeonardoUtil.getColor(activity, style, R.attr.action_bar_background_color))
            }
        }, IntentFilter(LeonardoEvents.THEME_CHANGED))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        restoreState()
    }

    private fun restoreState() {
        val viewType = preferences.getString(PreferenceKey.ACTIVE_CHART, PreferenceValue.STATIC_CHART)
                       ?: PreferenceValue.STATIC_CHART
        mayBeChangeChartContent(viewType)
    }
}