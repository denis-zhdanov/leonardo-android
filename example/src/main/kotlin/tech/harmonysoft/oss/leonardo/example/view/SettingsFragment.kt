package tech.harmonysoft.oss.leonardo.example.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import androidx.fragment.app.Fragment
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.ModelHolder
import tech.harmonysoft.oss.leonardo.model.runtime.ChartModel

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container).apply {
            val model = ModelHolder.model
            if (model == null) {
                ModelHolder.callback = {
                    if (it != null) {
                        setupDataSourceControls(this as ViewGroup, it)
                    }
                }
            } else {
                setupDataSourceControls(this as ViewGroup, model)
            }
        }
    }

    private fun setupDataSourceControls(content: ViewGroup, model: ChartModel) {
        content.removeAllViews()
        model.registeredDataSources.sortedBy { it.legend }.forEach { dataSource ->
            val view = CheckBoxView(context!!).apply {
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
            content.addView(createFiller())
            content.addView(view)
        }
        content.addView(createFiller())
    }

    private fun createFiller(): View {
        return Space(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
    }
}