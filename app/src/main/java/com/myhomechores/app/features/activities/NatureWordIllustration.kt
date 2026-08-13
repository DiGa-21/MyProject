package com.myhomechores.app.features.activities

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun NatureWordIllustration(wordId: String, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (wordId) {
            "sun" -> {
                val center = center
                val radius = size.minDimension * .22f
                drawCircle(Color(0xFFFFC83D), radius, center)
                repeat(8) { index ->
                    val angle = index * Math.PI / 4
                    val start = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * radius * 1.35f,
                        center.y + kotlin.math.sin(angle).toFloat() * radius * 1.35f,
                    )
                    val end = Offset(
                        center.x + kotlin.math.cos(angle).toFloat() * radius * 1.85f,
                        center.y + kotlin.math.sin(angle).toFloat() * radius * 1.85f,
                    )
                    drawLine(Color(0xFFFFB300), start, end, 5.dp.toPx())
                }
            }
            "tree" -> {
                drawRoundRect(
                    Color(0xFF936B45),
                    topLeft = Offset(size.width * .43f, size.height * .48f),
                    size = Size(size.width * .14f, size.height * .42f),
                )
                drawCircle(Color(0xFF62B873), size.minDimension * .23f, Offset(size.width * .5f, size.height * .35f))
                drawCircle(Color(0xFF76C987), size.minDimension * .17f, Offset(size.width * .34f, size.height * .43f))
                drawCircle(Color(0xFF76C987), size.minDimension * .17f, Offset(size.width * .66f, size.height * .43f))
            }
            "flower" -> {
                drawLine(Color(0xFF5FAF6D), Offset(size.width * .5f, size.height * .48f), Offset(size.width * .5f, size.height * .9f), 7.dp.toPx())
                val petals = listOf(
                    Offset(.5f, .25f), Offset(.35f, .37f), Offset(.65f, .37f),
                    Offset(.41f, .53f), Offset(.59f, .53f),
                )
                petals.forEach { drawCircle(Color(0xFFFF8EB5), size.minDimension * .12f, Offset(size.width * it.x, size.height * it.y)) }
                drawCircle(Color(0xFFFFC83D), size.minDimension * .11f, Offset(size.width * .5f, size.height * .4f))
            }
            "river" -> {
                val path = Path().apply {
                    moveTo(size.width * .18f, size.height * .1f)
                    cubicTo(size.width * .78f, size.height * .27f, size.width * .2f, size.height * .62f, size.width * .76f, size.height * .92f)
                }
                drawPath(path, Color(0xFF62BDEB), style = Stroke(width = size.minDimension * .22f))
                drawLine(Color(0xFF7BCB75), Offset(0f, size.height * .93f), Offset(size.width, size.height * .93f), size.height * .14f)
            }
        }
    }
}
