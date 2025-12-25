package com.github.soundxflow.utils

object LrcParser {
    private val timestampRegex = Regex("""\[(\d+):(\d+)(?:[:.](\d+))?\]""")
    private val offsetRegex = Regex("""\[offset:\s*(-?\d+)\s*\]""", RegexOption.IGNORE_CASE)

    fun parse(lrcContent: String): List<Pair<Long, String>> {
        val lines = lrcContent.lines()
        val sentences = mutableListOf<Pair<Long, String>>()
        var offset = 0L

        // Find offset first
        for (line in lines) {
            offsetRegex.find(line)?.let {
                offset = it.groupValues[1].toLongOrNull() ?: 0L
            }
        }

        for (line in lines) {
            val timestamps = timestampRegex.findAll(line).toList()
            if (timestamps.isEmpty()) continue

            // The text is what's left after all timestamps are removed
            var text = line
            for (ts in timestamps) {
                text = text.replace(ts.value, "")
            }
            text = text.trim()

            for (ts in timestamps) {
                val minutes = ts.groupValues[1].toLong()
                val seconds = ts.groupValues[2].toLong()
                val millisStr = ts.groupValues[3]
                
                var millis = when (millisStr.length) {
                    1 -> millisStr.toLong() * 100
                    2 -> millisStr.toLong() * 10
                    3 -> millisStr.toLong()
                    else -> 0L
                }

                val time = (minutes * 60 * 1000) + (seconds * 1000) + millis + offset
                sentences.add(time.coerceAtLeast(0L) to text)
            }
        }

        return sentences.sortedBy { it.first }
    }
}
