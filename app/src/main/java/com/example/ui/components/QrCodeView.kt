package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.security.MessageDigest
import kotlin.math.abs

@Composable
fun QrCodeView(
    dataString: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    codeColor: Color = Color(0xFF001D36)
) {
    val matrixSize = 25
    val grid = remember(dataString) {
        generateDeterministicQrGrid(dataString, matrixSize)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / matrixSize
            val cellHeight = size.height / matrixSize

            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    if (grid[row][col]) {
                        drawRect(
                            color = codeColor,
                            topLeft = Offset(col * cellWidth, row * cellHeight),
                            size = Size(cellWidth, cellHeight)
                        )
                    }
                }
            }
        }
    }
}

private fun generateDeterministicQrGrid(data: String, size: Int): Array<BooleanArray> {
    val grid = Array(size) { BooleanArray(size) }

    // Finder patterns in top-left, top-right, bottom-left
    fun drawFinder(startRow: Int, startCol: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = r == 0 || r == 6 || c == 0 || c == 6
                val isInner = r in 2..4 && c in 2..4
                grid[startRow + r][startCol + c] = isOuter || isInner
            }
        }
    }

    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    // Timing patterns
    for (i in 7 until size - 7) {
        grid[6][i] = (i % 2 == 0)
        grid[i][6] = (i % 2 == 0)
    }

    // Hash data deterministically to fill remaining grid cells
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(data.toByteArray())

    var bitIndex = 0
    for (r in 0 until size) {
        for (c in 0 until size) {
            val isFinderZone = (r < 8 && c < 8) || (r < 8 && c >= size - 8) || (r >= size - 8 && c < 8) || (r == 6 || c == 6)
            if (!isFinderZone) {
                val byteVal = abs(hash[bitIndex % hash.size].toInt())
                val bitVal = (byteVal shr (bitIndex % 8)) and 1
                grid[r][c] = (bitVal == 1)
                bitIndex++
            }
        }
    }

    return grid
}
