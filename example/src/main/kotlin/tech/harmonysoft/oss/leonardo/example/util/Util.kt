package tech.harmonysoft.oss.leonardo.example.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import tech.harmonysoft.oss.leonardo.example.view.CheckBoxView
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel

object Util {

    fun addSelectors(contentHolder: ViewGroup, model: ChartModel) {
        contentHolder.removeAllViews()
        model.registeredDataSources.sortedBy { it.legend }.forEach { dataSource ->
            val view = CheckBoxView(contentHolder.context).apply {
                color = dataSource.color
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                                         ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            view.color = dataSource.color
            view.callback = { enabled ->
                if (enabled) {
                    model.enableDataSource(dataSource)
                } else {
                    model.disableDataSource(dataSource)
                }
            }
            contentHolder.addView(createFiller(contentHolder.context))
            contentHolder.addView(view)
        }
        contentHolder.addView(createFiller(contentHolder.context))
    }

    private fun createFiller(context: Context): View {
        return Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
    }
}