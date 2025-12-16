package com.expense.tracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Period selection tabs for switching between Day/Week/Month/Year views
 */
enum class Period(val displayName: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

@Composable
fun PeriodSwitcher(
    selectedPeriod: Period,
    onPeriodSelected: (Period) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        Period.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = Period.entries.size
                )
            ) {
                Text(period.displayName)
            }
        }
    }
}
