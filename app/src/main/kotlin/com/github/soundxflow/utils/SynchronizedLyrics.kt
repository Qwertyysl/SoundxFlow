package com.github.soundxflow.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

class SynchronizedLyrics(val sentences: List<Pair<Long, String>>, private val positionProvider: () -> Long) {
    var index by mutableIntStateOf(currentIndex)
        private set

    private val currentIndex: Int
        get() {
            val position = positionProvider()
            var low = 0
            var high = sentences.size - 1
            var result = -1

            while (low <= high) {
                val mid = (low + high) / 2
                if (sentences[mid].first <= position) {
                    result = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            return result
        }

    fun update(): Boolean {
        val newIndex = currentIndex
        return if (newIndex != index) {
            index = newIndex
            true
        } else {
            false
        }
    }
}
