package com.example.ui.screens.gold

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.finance.MoneyFormatter
import com.example.domain.gold.GoldCalculator
import com.example.domain.gold.GoldPurity
import com.example.domain.gold.WeightFormatter
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.Charcoal950
import com.example.ui.theme.GoldTreasuryTheme
import com.example.ui.theme.LocalGoldColors
import com.example.ui.theme.MoneyTextStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyGoldScreen(
    viewModel: GoldTradeViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) onNavigateBack()
    }

    val parentCategories = state.categories.filter { it.parentCategoryId == null }
    val defaultCatId = parentCategories.firstOrNull()?.id ?: 0L

    var sellerName by remember { mutableStateOf("") }
    var invoiceRef by remember { mutableStateOf("") }
    var purchaseDateEpochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var paymentAccountId by remember { mutableStateOf<Long?>(null) }
    var receiptUri by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }

    var nextLineId by remember { mutableIntStateOf(2) }
    val lines = remember {
        mutableStateListOf(BuyLineInput(localId = 1, categoryId = defaultCatId))
    }

    LaunchedEffect(defaultCatId, state.latestRate) {
        if (defaultCatId > 0L) {
            lines.forEachIndexed { i, line ->
                val updatedCat = if (line.categoryId == 0L) defaultCatId else line.categoryId
                val updatedRate = if (line.ratePerGramText.isEmpty()) {
                    val suggested = viewModel.suggestedRatePerGramPaise(line.purity, line.customPurityPercent)
                    if (suggested > 0L) MoneyFormatter.paiseToPlainInput(suggested) else ""
                } else line.ratePerGramText
                if (updatedCat != line.categoryId || updatedRate != line.ratePerGramText) {
                    lines[i] = line.copy(categoryId = updatedCat, ratePerGramText = updatedRate)
                }
            }
        }
    }

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            receiptUri = uri.toString()
        }
    }

    val summaryNetMcg = lines.sumOf { line ->
        val gross = WeightFormatter.parseGramsToMicrograms(line.grossWeightText) ?: 0L
        val stone = WeightFormatter.parseGramsToMicrograms(line.stoneWeightText.ifBlank { "0" }) ?: 0L
        (gross - stone).coerceAtLeast(0L)
    }
    val summaryTotalPaise = lines.sumOf { line ->
        val gross = WeightFormatter.parseGramsToMicrograms(line.grossWeightText) ?: 0L
        val stone = WeightFormatter.parseGramsToMicrograms(line.stoneWeightText.ifBlank { "0" }) ?: 0L
        val net = (gross - stone).coerceAtLeast(0L)
        val rate = MoneyFormatter.parseRupeesToPaise(line.ratePerGramText) ?: 0L
        val base = GoldCalculator.calculateGoldValuePaise(net, rate)
        val making = MoneyFormatter.parseRupeesToPaise(line.makingText.ifBlank { "0" }) ?: 0L
        val wastage = MoneyFormatter.parseRupeesToPaise(line.wastageText.ifBlank { "0" }) ?: 0L
        val gst = MoneyFormatter.parseRupeesToPaise(line.gstText.ifBlank { "0" }) ?: 0L
        val other = MoneyFormatter.parseRupeesToPaise(line.otherChargesText.ifBlank { "0" }) ?: 0L
        base + making + wastage + gst + other
    }

    GoldTreasuryTheme {
        val gold = LocalGoldColors.current
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Buy Gold", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            bottomBar = {
                Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        state.errorMessage?.let { err ->
                            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Total Net: ${WeightFormatter.formatGrams(summaryNetMcg, 3)}",
                                style = MaterialTheme.typography.labelLarge.merge(MoneyTextStyle),
                                color = gold.mutedGold,
                            )
                            Text(
                                text = "Payable: ${MoneyFormatter.format(summaryTotalPaise)}",
                                style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                color = gold.champagneBright,
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.submitPurchase(
                                    purchaseDateEpochDay = purchaseDateEpochDay,
                                    sellerName = sellerName,
                                    invoiceRef = invoiceRef,
                                    paymentAccountId = paymentAccountId,
                                    receiptUri = receiptUri,
                                    notes = notes,
                                    lines = lines.toList(),
                                    defaultCategoryId = defaultCatId,
                                )
                            },
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("buy_gold_confirm_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = gold.champagne, contentColor = Charcoal950),
                        ) {
                            Text(if (state.isSaving) "Saving..." else "Record Gold Purchase", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Purchase Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = gold.champagneBright)
                            OutlinedTextField(
                                value = sellerName,
                                onValueChange = { sellerName = it },
                                label = { Text("Jeweller / Seller") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = invoiceRef,
                                    onValueChange = { invoiceRef = it },
                                    label = { Text("Invoice No.") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(formatEpochDay(purchaseDateEpochDay, state.preferences.dateFormat), maxLines = 1)
                                }
                            }
                            Text("Payment Account (Optional — moves funds as Asset Purchase)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(
                                    selected = paymentAccountId == null,
                                    onClick = { paymentAccountId = null },
                                    label = { Text("No Account Link") },
                                )
                                state.accounts.forEach { acc ->
                                    FilterChip(
                                        selected = paymentAccountId == acc.id,
                                        onClick = { paymentAccountId = acc.id },
                                        label = { Text(acc.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = gold.champagne.copy(alpha = 0.2f),
                                            selectedLabelColor = gold.champagneBright,
                                        ),
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { receiptPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (receiptUri != null) "Invoice Photo Attached (Change)" else "Attach Invoice Photo")
                            }
                        }
                    }
                }

                itemsIndexed(lines, key = { _, item -> item.localId }) { index, line ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, gold.goldBorder.copy(alpha = 0.7f)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Gold Item #${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = gold.champagneBright)
                                if (lines.size > 1) {
                                    IconButton(onClick = { lines.removeAt(index) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Remove Item", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = line.title,
                                onValueChange = { lines[index] = line.copy(title = it) },
                                label = { Text("Item Title (e.g. 22K Sovereign Coin) *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("Category", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                parentCategories.forEach { cat ->
                                    FilterChip(
                                        selected = line.categoryId == cat.id,
                                        onClick = { lines[index] = line.copy(categoryId = cat.id, subcategoryId = null) },
                                        label = { Text(cat.name) },
                                    )
                                }
                            }
                            Text("Purity", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                GoldPurity.entries.forEach { p ->
                                    FilterChip(
                                        selected = line.purity == p,
                                        onClick = {
                                            val suggested = viewModel.suggestedRatePerGramPaise(p, p.defaultPercentText)
                                            val rateStr = if (suggested > 0L) MoneyFormatter.paiseToPlainInput(suggested) else line.ratePerGramText
                                            lines[index] = line.copy(
                                                purity = p,
                                                customPurityPercent = if (p == GoldPurity.CUSTOM) line.customPurityPercent else p.defaultPercentText,
                                                ratePerGramText = rateStr,
                                            )
                                        },
                                        label = { Text(p.shortLabel) },
                                    )
                                }
                            }
                            if (line.purity == GoldPurity.CUSTOM) {
                                OutlinedTextField(
                                    value = line.customPurityPercent,
                                    onValueChange = { lines[index] = line.copy(customPurityPercent = it) },
                                    label = { Text("Purity %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Bulk Lot / Weight-Based", style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = line.isBulkLot,
                                    onCheckedChange = { lines[index] = line.copy(isBulkLot = it) },
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = line.grossWeightText,
                                    onValueChange = { lines[index] = line.copy(grossWeightText = it) },
                                    label = { Text("Gross g *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = line.stoneWeightText,
                                    onValueChange = { lines[index] = line.copy(stoneWeightText = it) },
                                    label = { Text("Stone g") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = line.quantityText,
                                    onValueChange = { lines[index] = line.copy(quantityText = it) },
                                    label = { Text("Qty") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f),
                                )
                            }

                            val netMcg = ((WeightFormatter.parseGramsToMicrograms(line.grossWeightText) ?: 0L) -
                                (WeightFormatter.parseGramsToMicrograms(line.stoneWeightText.ifBlank { "0" }) ?: 0L)).coerceAtLeast(0L)
                            val fineMcg = GoldCalculator.calculateFineGoldMicrograms(netMcg, line.purity, line.customPurityPercent)
                            Text(
                                text = "Net: ${WeightFormatter.formatGrams(netMcg, 3)}  •  Fine Gold: ${WeightFormatter.formatFineGrams(fineMcg)}",
                                style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                                color = gold.mutedGold,
                            )

                            OutlinedTextField(
                                value = line.ratePerGramText,
                                onValueChange = { lines[index] = line.copy(ratePerGramText = it) },
                                label = { Text("Gold Rate / gram (₹) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = line.makingText,
                                    onValueChange = { lines[index] = line.copy(makingText = it) },
                                    label = { Text("Making ₹") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = line.wastageText,
                                    onValueChange = { lines[index] = line.copy(wastageText = it) },
                                    label = { Text("Wastage ₹") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = line.gstText,
                                    onValueChange = { lines[index] = line.copy(gstText = it) },
                                    label = { Text("GST ₹") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = line.otherChargesText,
                                    onValueChange = { lines[index] = line.copy(otherChargesText = it) },
                                    label = { Text("Other ₹") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = line.hallmarkCode,
                                    onValueChange = { lines[index] = line.copy(hallmarkCode = it) },
                                    label = { Text("Hallmark / HUID") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = line.storageLocation,
                                    onValueChange = { lines[index] = line.copy(storageLocation = it) },
                                    label = { Text("Storage Location") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = {
                            val suggested = viewModel.suggestedRatePerGramPaise(GoldPurity.K22, GoldPurity.K22.defaultPercentText)
                            lines.add(
                                BuyLineInput(
                                    localId = nextLineId++,
                                    categoryId = defaultCatId,
                                    ratePerGramText = if (suggested > 0L) MoneyFormatter.paiseToPlainInput(suggested) else "",
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, gold.goldBorder),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = gold.champagne)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Another Gold Item", color = gold.champagneBright)
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Purchase Notes") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (showDatePicker) {
            val dpState = rememberDatePickerState(
                initialSelectedDateMillis = LocalDate.ofEpochDay(purchaseDateEpochDay)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dpState.selectedDateMillis?.let { millis ->
                            purchaseDateEpochDay = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                        }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
            ) { DatePicker(state = dpState) }
        }
    }
}
