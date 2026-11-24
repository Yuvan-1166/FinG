package com.example.ui.screens.gold

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.finance.MoneyFormatter
import com.example.domain.gold.WeightFormatter
import com.example.domain.model.AppPreferences
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.LocalGoldColors
import com.example.ui.theme.MoneyTextStyle
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun GoldWeightDonutChart(
    slices: List<GoldSlice>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGoldColors.current
    if (slices.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 18.dp),
        )
        return
    }

    var selectedIdx by remember(slices) { mutableIntStateOf(0) }
    val selected = slices.getOrNull(selectedIdx) ?: slices.first()
    val totalWeight = slices.sumOf { it.weightMcg }.coerceAtLeast(1L)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(142.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slices) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            if (hypot(dx, dy) > minOf(size.width, size.height) * 0.22f) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0f) angle += 360f
                                var acc = 0f
                                slices.forEachIndexed { idx, s ->
                                    val sweep = (s.weightMcg.toDouble() / totalWeight.toDouble()).toFloat() * 360f
                                    if (angle in acc..(acc + sweep)) {
                                        selectedIdx = idx
                                        return@detectTapGestures
                                    }
                                    acc += sweep
                                }
                            }
                        }
                    },
            ) {
                val strokeWidth = 22.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                var startAngle = -90f
                slices.forEachIndexed { idx, slice ->
                    val sweep = ((slice.weightMcg.toDouble() / totalWeight.toDouble()).toFloat() * 360f)
                        .coerceAtLeast(3f)
                    val color = gold.chartPalette[slice.colorIndex % gold.chartPalette.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(
                            width = if (idx == selectedIdx) strokeWidth + 4.dp.toPx() else strokeWidth,
                            cap = StrokeCap.Butt,
                        ),
                    )
                    startAngle += sweep
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val pct = ((selected.weightMcg * 100L) / totalWeight).toInt()
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                    color = gold.champagneBright,
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            slices.take(5).forEachIndexed { idx, slice ->
                val color = gold.chartPalette[slice.colorIndex % gold.chartPalette.size]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (idx == selectedIdx) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        )
                        .clickable { selectedIdx = idx }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = WeightFormatter.formatGrams(slice.weightMcg, decimals = 2),
                        style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                        color = gold.champagneBright,
                    )
                }
            }
        }
    }
}

@Composable
fun GoldValueComparisonBars(
    acquisitionCostPaise: Long,
    benchmarkValuePaise: Long,
    resaleValuePaise: Long,
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGoldColors.current
    val maxVal = maxOf(acquisitionCostPaise, benchmarkValuePaise, resaleValuePaise).coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ComparisonBarRow(
            label = "Acquisition Cost",
            valuePaise = acquisitionCostPaise,
            ratio = (acquisitionCostPaise.toDouble() / maxVal.toDouble()).toFloat(),
            color = gold.mutedGold,
            preferences = preferences,
        )
        ComparisonBarRow(
            label = "24K Fine Benchmark",
            valuePaise = benchmarkValuePaise,
            ratio = (benchmarkValuePaise.toDouble() / maxVal.toDouble()).toFloat(),
            color = gold.champagne,
            preferences = preferences,
        )
        ComparisonBarRow(
            label = "Est. Realizable Resale (ex-GST)",
            valuePaise = resaleValuePaise,
            ratio = (resaleValuePaise.toDouble() / maxVal.toDouble()).toFloat(),
            color = gold.gain,
            preferences = preferences,
        )
    }
}

@Composable
private fun ComparisonBarRow(
    label: String,
    valuePaise: Long,
    ratio: Float,
    color: Color,
    preferences: AppPreferences,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = MoneyFormatter.formatWithMode(valuePaise, preferences.currencyDisplayMode, preferences.showPaiseDecimals),
                style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle),
                color = color,
            )
        }
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun GoldMonthlyBuySellChart(
    points: List<GoldMonthlyFlowPoint>,
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGoldColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    if (points.isEmpty() || points.all { it.boughtPaise == 0L && it.soldPaise == 0L }) {
        Text(
            text = "No gold buys or sales in the last 6 months.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 18.dp),
        )
        return
    }

    var selectedIdx by remember(points) { mutableIntStateOf(points.lastIndex) }
    val selected = points.getOrNull(selectedIdx) ?: points.last()
    val maxVal = points.maxOf { maxOf(it.boughtPaise, it.soldPaise) }.coerceAtLeast(100L)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(selected.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Buy ${MoneyFormatter.formatWithMode(selected.boughtPaise, preferences.currencyDisplayMode, false)}",
                        style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                        color = gold.champagneBright,
                    )
                    Text(
                        text = "Sell ${MoneyFormatter.formatWithMode(selected.soldPaise, preferences.currencyDisplayMode, false)}",
                        style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                        color = gold.gain,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val gw = size.width.toFloat() / points.size.coerceAtLeast(1)
                        selectedIdx = (offset.x / gw).toInt().coerceIn(0, points.lastIndex)
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            for (s in 0..3) {
                val y = h * (s / 3f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }
            val gw = w / points.size
            val bw = (gw * 0.28f).coerceIn(6.dp.toPx(), 16.dp.toPx())
            val gap = 4.dp.toPx()

            points.forEachIndexed { idx, pt ->
                val cx = gw * idx + gw / 2f
                if (idx == selectedIdx) {
                    drawRoundRect(
                        color = gold.champagne.copy(alpha = 0.08f),
                        topLeft = Offset(gw * idx + 4.dp.toPx(), 0f),
                        size = Size(gw - 8.dp.toPx(), h),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    )
                }
                val buyH = (h * (pt.boughtPaise.toDouble() / maxVal).toFloat()).coerceAtLeast(if (pt.boughtPaise > 0L) 4.dp.toPx() else 0f)
                val sellH = (h * (pt.soldPaise.toDouble() / maxVal).toFloat()).coerceAtLeast(if (pt.soldPaise > 0L) 4.dp.toPx() else 0f)

                if (buyH > 0f) {
                    drawRoundRect(
                        color = gold.champagne,
                        topLeft = Offset(cx - bw - gap / 2f, h - buyH),
                        size = Size(bw, buyH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }
                if (sellH > 0f) {
                    drawRoundRect(
                        color = gold.gain,
                        topLeft = Offset(cx + gap / 2f, h - sellH),
                        size = Size(bw, sellH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEachIndexed { idx, pt ->
                Text(
                    text = pt.label,
                    modifier = Modifier.weight(1f).clickable { selectedIdx = idx },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (idx == selectedIdx) gold.champagneBright else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

data class GoldLinePoint(
    val epochDay: Long,
    val primaryValue: Long,
    val secondaryLabel: String = "",
)

@Composable
fun GoldTrendLineChart(
    points: List<GoldLinePoint>,
    valueFormatter: (Long) -> String,
    preferences: AppPreferences,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    val gold = LocalGoldColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    if (points.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 18.dp),
        )
        return
    }

    var selectedIdx by remember(points) { mutableIntStateOf(points.lastIndex) }
    val selected = points.getOrNull(selectedIdx) ?: points.last()
    val maxVal = points.maxOf { it.primaryValue }.coerceAtLeast(1L)
    val minVal = points.minOf { it.primaryValue }
    val span = (maxVal - minVal).coerceAtLeast(1L)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatEpochDay(selected.epochDay, preferences.dateFormat),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valueFormatter(selected.primaryValue),
                style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                color = gold.champagneBright,
            )
        }
        Spacer(Modifier.height(10.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.size > 1) {
                            val step = size.width.toFloat() / (points.size - 1)
                            selectedIdx = (offset.x / step + 0.5f).toInt().coerceIn(0, points.lastIndex)
                        }
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            for (s in 0..3) {
                val y = h * (s / 3f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }
            if (points.size == 1) {
                drawCircle(gold.champagne, radius = 6.dp.toPx(), center = Offset(w / 2f, h / 2f))
                return@Canvas
            }
            val stepX = w / (points.size - 1)
            val coords = points.mapIndexed { idx, pt ->
                val ratio = if (maxVal == minVal) 0.5f else ((pt.primaryValue - minVal).toDouble() / span.toDouble()).toFloat()
                val y = h - (ratio * (h - 18.dp.toPx())) - 9.dp.toPx()
                Offset(idx * stepX, y)
            }
            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    val prev = coords[i - 1]
                    val curr = coords[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, h)
                lineTo(coords.first().x, h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(gold.champagne.copy(alpha = 0.30f), gold.champagne.copy(alpha = 0.01f)),
                ),
            )
            drawPath(
                path = linePath,
                color = gold.champagne,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )
            val sel = coords[selectedIdx]
            drawLine(gold.champagne.copy(alpha = 0.45f), Offset(sel.x, 0f), Offset(sel.x, h), strokeWidth = 1.dp.toPx())
            drawCircle(Color.Black, radius = 6.dp.toPx(), center = sel)
            drawCircle(gold.champagneBright, radius = 4.dp.toPx(), center = sel)
        }
    }
}
