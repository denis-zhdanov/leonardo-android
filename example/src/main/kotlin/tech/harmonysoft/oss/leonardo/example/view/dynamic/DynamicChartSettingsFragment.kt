package tech.harmonysoft.oss.leonardo.example.view.dynamic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tech.harmonysoft.oss.leonardo.example.R
import tech.harmonysoft.oss.leonardo.example.data.ModelHolder
import tech.harmonysoft.oss.leonardo.example.util.Util

class DynamicChartSettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container).apply {
            val model = ModelHolder.model
            if (model == null) {
                ModelHolder.callback = {
                    if (it != null) {
                        Util.addSelectors(this as ViewGroup, it)
                    }
                }
            } else {
                Util.addSelectors(this as ViewGroup, model)
            }
        }
    }
}