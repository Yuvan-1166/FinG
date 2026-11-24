package com.example.ui.components

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.finance.CategoryBreakdownItem
import com.example.domain.finance.DailySpendPoint
import com.example.domain.finance.MoneyFormatter
import com.example.domain.finance.PeriodIncomeExpensePoint
import com.example.domain.model.AppPreferences
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.theme.MoneyTextStyle
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun IncomeExpenseBarChart(
    points: List<PeriodIncomeExpensePoint>,
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
) {
    val financeColors = LocalFinanceColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (points.isEmpty() || points.all { it.incomePaise == 0L && it.expensePaise == 0L }) {
        Text(
            text = "No income or expense activity recorded in this period.",
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
            modifier = modifier.padding(vertical = 24.dp),
        )
        return
    }

    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    val selected = points.getOrNull(selectedIndex) ?: points.last()

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip & Legend row
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "${selected.label}:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "+${MoneyFormatter.formatWithMode(selected.incomePaise, preferences.currencyDisplayMode, false)}",
                        style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                        color = financeColors.income,
                    )
                    Text(
                        text = "-${MoneyFormatter.formatWithMode(selected.expensePaise, preferences.currencyDisplayMode, false)}",
                        style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                        color = financeColors.expense,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LegendDot(color = financeColors.income, label = "In")
                LegendDot(color = financeColors.expense, label = "Out")
            }
        }

        Spacer(Modifier.height(14.dp))

        val maxVal = points.maxOf { maxOf(it.incomePaise, it.expensePaise) }.coerceAtLeast(100L)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val groupWidth = size.width.toFloat() / points.size.coerceAtLeast(1)
                        val idx = (offset.x / groupWidth).toInt().coerceIn(0, points.lastIndex)
                        selectedIndex = idx
                    }
                },
        ) {
            val h = size.height
            val w = size.width
            // Grid lines
            for (step in 0..3) {
                val y = h * (step / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val groupWidth = w / points.size
            val barWidth = (groupWidth * 0.28f).coerceIn(6.dp.toPx(), 18.dp.toPx())
            val gap = 4.dp.toPx()

            points.forEachIndexed { idx, pt ->
                val centerX = groupWidth * idx + groupWidth / 2f
                if (idx == selectedIndex) {
                    drawRoundRect(
                        color = gridColor.copy(alpha = 0.45f),
                        topLeft = Offset(groupWidth * idx + 4.dp.toPx(), 0f),
                        size = Size(groupWidth - 8.dp.toPx(), h),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    )
                }
                val incomeRatio = (pt.incomePaise.toDouble() / maxVal.toDouble()).toFloat().coerceIn(0f, 1f)
                val expenseRatio = (pt.expensePaise.toDouble() / maxVal.toDouble()).toFloat().coerceIn(0f, 1f)

                val incomeH = (h * incomeRatio).coerceAtLeast(if (pt.incomePaise > 0L) 4.dp.toPx() else 0f)
                val expenseH = (h * expenseRatio).coerceAtLeast(if (pt.expensePaise > 0L) 4.dp.toPx() else 0f)

                if (incomeH > 0f) {
                    drawRoundRect(
                        color = financeColors.income,
                        topLeft = Offset(centerX - barWidth - gap / 2f, h - incomeH),
                        size = Size(barWidth, incomeH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }
                if (expenseH > 0f) {
                    drawRoundRect(
                        color = financeColors.expense,
                        topLeft = Offset(centerX + gap / 2f, h - expenseH),
                        size = Size(barWidth, expenseH),
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
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedIndex = idx },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (idx == selectedIndex) MaterialTheme.colorScheme.primary else labelColor,
                    fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    items: List<CategoryBreakdownItem>,
    preferences: AppPreferences,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Text(
            text = "No expense breakdown available for this period.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 20.dp),
        )
        return
    }

    var selectedIndex by remember(items) { mutableIntStateOf(0) }
    val selected = items.getOrNull(selectedIndex) ?: items.first()
    val totalPaise = items.sumOf { it.amountPaise }.coerceAtLeast(1L)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(144.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            if (hypot(dx, dy) > minOf(size.width, size.height) * 0.22f) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0f) angle += 360f
                                var acc = 0f
                                items.forEachIndexed { index, item ->
                                    val sweep = (item.amountPaise.toDouble() / totalPaise.toDouble()).toFloat() * 360f
                                    if (angle in acc..(acc + sweep)) {
                                        selectedIndex = index
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
                items.forEachIndexed { idx, item ->
                    val sweep = ((item.amountPaise.toDouble() / totalPaise.toDouble()).toFloat() * 360f)
                        .coerceAtLeast(2f)
                    val color = parseHexColor(item.colorHex)
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(
                            width = if (idx == selectedIndex) strokeWidth + 4.dp.toPx() else strokeWidth,
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
                    text = selected.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${selected.shareBasisPoints / 100}%",
                    style = MaterialTheme.typography.titleMedium.merge(MoneyTextStyle),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.take(5).forEachIndexed { idx, item ->
                val color = parseHexColor(item.colorHex)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (idx == selectedIndex) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable { selectedIndex = idx }
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
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = MoneyFormatter.formatWithMode(
                            item.amountPaise,
                            preferences.currencyDisplayMode,
                            showDecimals = false,
                        ),
                        style = MaterialTheme.typography.labelMedium.merge(MoneyTextStyle),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingTrendChart(
    points: List<DailySpendPoint>,
    preferences: AppPreferences,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    if (points.isEmpty()) {
        Text(
            text = "Not enough data for trend view.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = 20.dp),
        )
        return
    }

    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    val selected = points.getOrNull(selectedIndex) ?: points.last()
    val maxVal = points.maxOf { it.amountPaise }.coerceAtLeast(100L)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatEpochDay(selected.date.toEpochDay(), preferences.dateFormat),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = MoneyFormatter.formatWithMode(
                    selected.amountPaise,
                    preferences.currencyDisplayMode,
                    preferences.showPaiseDecimals,
                ),
                style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                color = lineColor,
            )
        }
        Spacer(Modifier.height(10.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.size > 1) {
                            val step = size.width.toFloat() / (points.size - 1)
                            val idx = (offset.x / step + 0.5f).toInt().coerceIn(0, points.lastIndex)
                            selectedIndex = idx
                        }
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            if (points.size == 1) {
                drawCircle(lineColor, radius = 6.dp.toPx(), center = Offset(w / 2f, h / 2f))
                return@Canvas
            }

            val stepX = w / (points.size - 1)
            val coords = points.mapIndexed { idx, pt ->
                val x = idx * stepX
                val ratio = (pt.amountPaise.toDouble() / maxVal.toDouble()).toFloat().coerceIn(0f, 1f)
                val y = h - (ratio * (h - 14.dp.toPx())) - 6.dp.toPx()
                Offset(x, y)
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
                    colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.01f)),
                ),
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
            )

            val sel = coords[selectedIndex]
            drawLine(
                color = lineColor.copy(alpha = 0.4f),
                start = Offset(sel.x, 0f),
                end = Offset(sel.x, h),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = sel)
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = sel)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
