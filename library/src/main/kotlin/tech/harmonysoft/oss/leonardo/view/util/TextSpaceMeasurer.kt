package tech.harmonysoft.oss.leonardo.view.util

import tech.harmonysoft.oss.leonardo.model.text.TextWrapper

interface TextSpaceMeasurer {

    /**
     * @param text      target text
     * @return          visual space occupied by the given text. It might be either horizontal or vertical space
     */
    fun measureVisualSpace(text: String): Int

    fun measureVisualSpace(text: TextWrapper): Int
}