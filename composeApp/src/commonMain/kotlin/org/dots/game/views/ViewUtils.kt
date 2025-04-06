package org.dots.game.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.round

@Composable
fun IntegerSlider(name: String, currentValue: Int, minValue: Int, maxValue: Int, onValueChange: (Int) -> Unit) {
    val range = maxValue - minValue

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.fillMaxWidth(textFraction))
        Slider(
            value = (currentValue - minValue).toFloat() / range,
            onValueChange = {
                onValueChange(round(it * range + minValue).toInt())
            },
            steps = range - 1,
            modifier = Modifier.width(150.dp)
        )
        Text(currentValue.toString())
    }
}

internal const val textFraction = 0.4f