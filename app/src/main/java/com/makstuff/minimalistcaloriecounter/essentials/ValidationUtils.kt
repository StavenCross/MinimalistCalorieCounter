package com.makstuff.minimalistcaloriecounter.essentials

import android.content.Context
import com.makstuff.minimalistcaloriecounter.R

fun checkValidNumber(value: String, nameOfField: String, context: Context) {
    check(value.toDoubleOrNull() != null) {
        if (value.contains(",")) {
            context.getString(R.string.use_period_instead_of_comma)
        } else {
            nameOfField + " " + context.getString(R.string.must_be_a_valid_number) + "."
        }
    }
}
