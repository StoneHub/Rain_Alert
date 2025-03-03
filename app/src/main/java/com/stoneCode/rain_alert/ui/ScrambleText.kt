package com.stoneCode.rain_alert.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.text.font.FontFamily
import kotlin.random.Random

// --- Adjustable Parameters ---
private const val SCRAMBLE_INTERVAL_MS = 150L // Interval between chunk shifts
private const val MIN_CHUNK_SIZE = 1
private const val MAX_CHUNK_SIZE = 3

@Composable
fun ScrambleText(
    text: String,
    isStatic: Boolean,
    onAnimationFinished: () -> Unit = {}
) {
    var scrambleText by remember { mutableStateOf(text) }
    var isAnimationRunning by remember { mutableStateOf(false) }
    val fontSize = 18.sp // Constant font size

    // Get the color from the MaterialTheme outside of remember
    val textColor = MaterialTheme.colorScheme.onPrimary

    // Define the TextStyle but don't use MaterialTheme inside remember
    val textStyle = remember(textColor) {
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Default
        )
    }

    LaunchedEffect(text, isStatic) {
        if (text.isNotEmpty() && !isAnimationRunning) {
            isAnimationRunning = true
            val originalText = text.toCharArray()
            val textLength = originalText.size

            if (isStatic) {
                // Static scrambling (only scramble once)
                scrambleText = String(originalText.clone().apply { shuffle() })
            } else {
                // Dynamic scrambling (continuous chunk shifting)
                launch {
                    while (isAnimationRunning) {
                        val tempText = scrambleText.toCharArray()
                        for (i in 0 until textLength) {
                            val chunkSize = Random.nextInt(MIN_CHUNK_SIZE, MAX_CHUNK_SIZE + 1)
                            val endIndex = minOf(i + chunkSize, textLength)

                            if (Random.nextBoolean()) {
                                // Scramble the chunk
                                val scrambledChunk = tempText.copyOfRange(i, endIndex).apply { shuffle() }
                                for (j in i until endIndex) {
                                    tempText[j] = scrambledChunk[j - i]
                                }
                            } else {
                                // Unscramble the chunk (progressively)
                                for (j in i until endIndex) {
                                    tempText[j] = originalText[j]
                                }
                            }
                            scrambleText = String(tempText)
                            delay(SCRAMBLE_INTERVAL_MS / chunkSize)
                        }
                        delay(SCRAMBLE_INTERVAL_MS)
                    }
                }

                // Unscramble progressively after the loop
                for (i in 0 until textLength) {
                    val chunkSize = Random.nextInt(MIN_CHUNK_SIZE, MAX_CHUNK_SIZE + 1)
                    val endIndex = minOf(i + chunkSize, textLength)

                    val tempText = scrambleText.toCharArray()
                    for (j in i until endIndex) {
                        tempText[j] = originalText[j]
                    }

                    scrambleText = String(tempText)
                    delay(SCRAMBLE_INTERVAL_MS / chunkSize)
                }
            }

            // If not static, ensure text is fully unscrambled at the end
            if (!isStatic) {
                scrambleText = text
            }

            isAnimationRunning = false
            onAnimationFinished()
        }
    }

    Text(
        text = scrambleText,
        style = textStyle, // Use the defined TextStyle
        modifier = Modifier.width(IntrinsicSize.Max)
    )
}