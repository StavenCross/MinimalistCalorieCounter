package com.makstuff.minimalistcaloriecounter.classes

import android.content.Context
import com.makstuff.minimalistcaloriecounter.R

class CustomWeights(
    val inputString: String = "",
    context: Context
) {
    val listOfStrings: MutableList<Pair<String, String>> = mutableListOf()

    init {
        val isValid = this.inputString.isEmpty() || Regex("""^\d+(\.\d+)?:[^\n\r,:-]+(-\d+(\.\d+)?:[^\n\r,:-]+)*$""").containsMatchIn(
            this.inputString
        )
        check(isValid) {
            context.getString(R.string.custom_weights_format)
        }
        
        if (this.inputString.isNotEmpty()) {
            this.inputString.split("-").forEach {
                val splitList = it.split(":")
                if (splitList.size >= 2) {
                    val weight = splitList[0]
                    val name = splitList[1]
                    this.listOfStrings.add(Pair(weight, name))
                }
            }
        }
    }
}
