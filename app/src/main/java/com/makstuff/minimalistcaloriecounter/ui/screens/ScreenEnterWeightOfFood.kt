package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.makstuff.minimalistcaloriecounter.R
import com.makstuff.minimalistcaloriecounter.ui.reused.Grid
import com.makstuff.minimalistcaloriecounter.ui.reused.RowOfButtonText
import com.makstuff.minimalistcaloriecounter.ui.reused.TextField


@Composable
fun ScreenEnterWeightOfFood(
    currentWeight: String,
    onWeightChange: (String) -> Unit,
    onConfirm: () -> Unit,
    listOfTextButtons: List<Pair<String, () -> Unit>>,
    listOfItems: List<Pair<Int, @Composable () -> Unit>>,
    listOfQSItems: List<Pair<Int, @Composable () -> Unit>>
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LaunchedEffect(Unit) {
                if (currentWeight == "") focusRequester.requestFocus()
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextField(
                    value = currentWeight,
                    onValueChange = { onWeightChange(it) },
                    label = stringResource(R.string.weight_of_food),
                    placeholder = "g",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                RowOfButtonText(list = listOfTextButtons)
            }
            val combinedItems = remember(listOfItems, listOfQSItems) {
                // App.kt sends QS items reversed.
                // In a reversed grid (reverseUpDown=true, reverseLeftRight=true),
                // the first item in the list is at the bottom-right visually.
                val qsProcessed = listOfQSItems.map { Pair(2, it.second) }
                val qsPaddingCount = (3 - (qsProcessed.size % 3)) % 3
                val qsPadding = List<Pair<Int, @Composable () -> Unit>>(qsPaddingCount) { Pair(2, @Composable {}) }

                // To have normal items on top and LTR in a reversed grid, we reverse the list.
                val normalProcessed = listOfItems.reversed()
                val nPaddingCount = (6 - (normalProcessed.size % 6)) % 6
                val nPadding = List<Pair<Int, @Composable () -> Unit>>(nPaddingCount) { Pair(1, @Composable {}) }

                // Order in list: [Bottom-most] ... [Top-most]
                qsProcessed + qsPadding + nPadding + normalProcessed
            }

            Grid(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                columns = 6,
                reverseUpDown = true,
                reverseLeftRight = true,
                items = combinedItems
            )
        }
    }
}
