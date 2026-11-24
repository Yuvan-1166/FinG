package com.example.ui.screens.gold

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.GoldCategoryEntity
import com.example.data.local.GoldItemEntity
import com.example.domain.finance.MoneyFormatter
import com.example.domain.gold.GoldItemStatus
import com.example.domain.gold.GoldPurity
import com.example.domain.gold.WeightFormatter
import com.example.ui.components.formatEpochDay
import com.example.ui.theme.Charcoal950
import com.example.ui.theme.GoldEmptyCard
import com.example.ui.theme.GoldPurityBadge
import com.example.ui.theme.GoldTreasuryTheme
import com.example.ui.theme.LocalGoldColors
import com.example.ui.theme.MoneyTextStyle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldInventoryScreen(
    viewModel: GoldInventoryViewModel,
    onNavigateBack: () -> Unit,
    onOpenItemDetail: (Long) -> Unit,
    onOpenBuyGold: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    GoldTreasuryTheme {
        val gold = LocalGoldColors.current
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Gold Inventory", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showCategoryDialog = true }) {
                            Icon(Icons.Rounded.Category, contentDescription = "Gold Categories", tint = gold.champagne)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    containerColor = gold.champagne,
                    contentColor = Charcoal950,
                    modifier = Modifier.testTag("gold_add_item_fab"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add gold item")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Search Box
                item {
                    OutlinedTextField(
                        value = state.filter.searchQuery,
                        onValueChange = viewModel::setSearch,
                        placeholder = { Text("Search items, hallmarks, vault location...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.filter.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearch("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                }

                // Purity Filter Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.filter.purity == null,
                            onClick = { viewModel.setPurityFilter(null) },
                            label = { Text("All Purity") },
                        )
                        GoldPurity.entries.forEach { purity ->
                            FilterChip(
                                selected = state.filter.purity == purity,
                                onClick = {
                                    viewModel.setPurityFilter(if (state.filter.purity == purity) null else purity)
                                },
                                label = { Text(purity.shortLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gold.champagne.copy(alpha = 0.2f),
                                    selectedLabelColor = gold.champagneBright,
                                ),
                            )
                        }
                        FilterChip(
                            selected = state.filter.includeClosed,
                            onClick = viewModel::toggleIncludeClosed,
                            label = { Text("Include Sold/Archived") },
                        )
                    }
                }

                // Category Filter Row
                item {
                    val parentCategories = state.categories.filter { it.parentCategoryId == null && !it.isArchived }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.filter.categoryId == null,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text("All Categories") },
                        )
                        parentCategories.forEach { cat ->
                            FilterChip(
                                selected = state.filter.categoryId == cat.id,
                                onClick = {
                                    viewModel.setCategoryFilter(if (state.filter.categoryId == cat.id) null else cat.id)
                                },
                                label = { Text(cat.name) },
                            )
                        }
                    }
                }

                if (state.items.isEmpty()) {
                    item {
                        GoldEmptyCard(
                            title = "No Gold Holdings Match",
                            subtitle = "Add an existing vault item or record a new gold purchase.",
                            actionLabel = "Buy Gold",
                            onActionClick = onOpenBuyGold,
                        )
                    }
                } else {
                    items(state.items, key = { it.item.id }) { row ->
                        val item = row.item
                        val purity = runCatching { GoldPurity.valueOf(item.purity) }.getOrDefault(GoldPurity.K22)
                        val hasRate = state.latestRate != null
                        val pnl = row.valuation.unrealizedGainLossPaise
                        val pnlColor = if (pnl >= 0L) gold.gain else gold.loss

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenItemDetail(item.id) }
                                .testTag("gold_item_card_${item.id}"),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, gold.goldBorder.copy(alpha = 0.65f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (item.itemPhotoUri != null) {
                                    AsyncImage(
                                        model = item.itemPhotoUri,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(gold.champagne.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Diamond, contentDescription = null, tint = gold.champagne)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        GoldPurityBadge(purity, item.purityPercentText)
                                    }
                                    Text(
                                        text = buildString {
                                            append(row.categoryName)
                                            row.subcategoryName?.let { append(" • $it") }
                                            if (item.isBulkLot) append(" • Bulk") else if (item.quantity > 1) append(" • Qty ${item.quantity}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Net ${WeightFormatter.formatGrams(item.netWeightMcg, 3)}  •  Fine ${WeightFormatter.formatGrams(row.valuation.fineGoldMcg, 4)}",
                                        style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                                        color = gold.mutedGold,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (hasRate) {
                                            MoneyFormatter.formatWithMode(row.valuation.benchmark24kValuePaise, state.preferences.currencyDisplayMode, false)
                                        } else {
                                            MoneyFormatter.formatWithMode(item.acquisitionCostPaise, state.preferences.currencyDisplayMode, false)
                                        },
                                        style = MaterialTheme.typography.titleSmall.merge(MoneyTextStyle),
                                        color = gold.champagneBright,
                                    )
                                    Text(
                                        text = if (hasRate) {
                                            MoneyFormatter.formatWithMode(pnl, state.preferences.currencyDisplayMode, false, showPlusSign = true)
                                        } else "Cost",
                                        style = MaterialTheme.typography.labelSmall.merge(MoneyTextStyle),
                                        color = if (hasRate) pnlColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (item.status != GoldItemStatus.ACTIVE.name) {
                                        Text(
                                            text = item.status.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddItemDialog) {
            GoldItemEditorDialog(
                categories = state.categories.filter { !it.isArchived },
                existing = null,
                onSave = {
                    viewModel.saveItem(it)
                    showAddItemDialog = false
                },
                onDismiss = { showAddItemDialog = false },
            )
        }

        if (showCategoryDialog) {
            GoldCategoriesManagerDialog(
                categories = state.categories,
                onCreateCategory = viewModel::saveCategory,
                onDeleteOrArchive = viewModel::deleteOrArchiveCategory,
                onDismiss = { showCategoryDialog = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldItemEditorDialog(
    categories: List<GoldCategoryEntity>,
    existing: GoldItemEntity?,
    onSave: (GoldItemEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val parentCategories = categories.filter { it.parentCategoryId == null }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var selectedCategoryId by remember { mutableStateOf(existing?.categoryId ?: parentCategories.firstOrNull()?.id ?: 0L) }
    var selectedSubcategoryId by remember { mutableStateOf(existing?.subcategoryId) }
    var purity by remember { mutableStateOf(existing?.let { runCatching { GoldPurity.valueOf(it.purity) }.getOrNull() } ?: GoldPurity.K22) }
    var customPurityPct by remember { mutableStateOf(existing?.purityPercentText ?: purity.defaultPercentText) }
    var isBulkLot by remember { mutableStateOf(existing?.isBulkLot ?: false) }
    var quantityText by remember { mutableStateOf((existing?.quantity ?: 1).toString()) }
    var grossWeightText by remember { mutableStateOf(existing?.let { WeightFormatter.plainInput(it.grossWeightMcg) } ?: "") }
    var stoneWeightText by remember { mutableStateOf(existing?.let { WeightFormatter.plainInput(it.stoneWeightMcg) } ?: "0") }
    var costText by remember { mutableStateOf(existing?.let { MoneyFormatter.paiseToPlainInput(it.acquisitionCostPaise) } ?: "") }
    var merchantText by remember { mutableStateOf(existing?.merchantName ?: "") }
    var hallmarkText by remember { mutableStateOf(existing?.hallmarkCode ?: "") }
    var locationText by remember { mutableStateOf(existing?.storageLocation ?: "") }
    var photoUri by remember { mutableStateOf(existing?.itemPhotoUri) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            photoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Vault Gold Item" else "Edit Gold Item") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    parentCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = {
                                selectedCategoryId = cat.id
                                selectedSubcategoryId = null
                            },
                            label = { Text(cat.name) },
                        )
                    }
                }
                val subs = categories.filter { it.parentCategoryId == selectedCategoryId }
                if (subs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        subs.forEach { sub ->
                            FilterChip(
                                selected = selectedSubcategoryId == sub.id,
                                onClick = { selectedSubcategoryId = if (selectedSubcategoryId == sub.id) null else sub.id },
                                label = { Text(sub.name) },
                            )
                        }
                    }
                }

                Text("Purity", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GoldPurity.entries.forEach { p ->
                        FilterChip(
                            selected = purity == p,
                            onClick = {
                                purity = p
                                if (p != GoldPurity.CUSTOM) customPurityPct = p.defaultPercentText
                            },
                            label = { Text(p.shortLabel) },
                        )
                    }
                }
                if (purity == GoldPurity.CUSTOM) {
                    OutlinedTextField(
                        value = customPurityPct,
                        onValueChange = { customPurityPct = it },
                        label = { Text("Purity % (e.g. 91.6)") },
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
                    Text("Track as Bulk Lot", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isBulkLot, onCheckedChange = { isBulkLot = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grossWeightText,
                        onValueChange = { grossWeightText = it },
                        label = { Text("Gross g *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = stoneWeightText,
                        onValueChange = { stoneWeightText = it },
                        label = { Text("Stone g") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.6f),
                    )
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Total Cost (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.4f),
                    )
                }
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Jeweller / Source") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hallmarkText,
                        onValueChange = { hallmarkText = it },
                        label = { Text("HUID / Hallmark") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it },
                        label = { Text("Storage Vault") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (photoUri != null) "Photo Attached (Tap to change)" else "Attach Item Photo")
                }

                errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val grossMcg = WeightFormatter.parseGramsToMicrograms(grossWeightText)
                    val stoneMcg = WeightFormatter.parseGramsToMicrograms(stoneWeightText.ifBlank { "0" }) ?: 0L
                    val costPaise = MoneyFormatter.parseRupeesToPaise(costText)
                    val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1

                    if (cleanTitle.isEmpty()) {
                        errorMsg = "Enter a title for this item."
                        return@Button
                    }
                    if (selectedCategoryId <= 0L) {
                        errorMsg = "Select a gold category."
                        return@Button
                    }
                    if (grossMcg == null || grossMcg <= 0L) {
                        errorMsg = "Enter a valid positive gross weight."
                        return@Button
                    }
                    val netMcg = grossMcg - stoneMcg
                    if (netMcg <= 0L) {
                        errorMsg = "Net gold weight (gross - stone) must be positive."
                        return@Button
                    }
                    if (costPaise == null || costPaise <= 0L) {
                        errorMsg = "Enter a valid acquisition cost."
                        return@Button
                    }
                    val pctText = if (purity == GoldPurity.CUSTOM) customPurityPct.trim() else purity.defaultPercentText
                    val base = existing ?: GoldItemEntity(
                        title = cleanTitle,
                        categoryId = selectedCategoryId,
                        purity = purity.name,
                        purityPercentText = pctText,
                        grossWeightMcg = grossMcg,
                        stoneWeightMcg = stoneMcg,
                        netWeightMcg = netMcg,
                        originalNetWeightMcg = netMcg,
                        acquisitionCostPaise = costPaise,
                        originalAcquisitionCostPaise = costPaise,
                        purchaseDateEpochDay = LocalDate.now().toEpochDay(),
                    )
                    onSave(
                        base.copy(
                            title = cleanTitle,
                            categoryId = selectedCategoryId,
                            subcategoryId = selectedSubcategoryId,
                            purity = purity.name,
                            purityPercentText = pctText,
                            isBulkLot = isBulkLot,
                            quantity = qty,
                            originalQuantity = existing?.originalQuantity ?: qty,
                            grossWeightMcg = grossMcg,
                            stoneWeightMcg = stoneMcg,
                            netWeightMcg = netMcg,
                            originalNetWeightMcg = existing?.originalNetWeightMcg ?: netMcg,
                            acquisitionCostPaise = costPaise,
                            originalAcquisitionCostPaise = existing?.originalAcquisitionCostPaise ?: costPaise,
                            merchantName = merchantText.trim(),
                            hallmarkCode = hallmarkText.trim(),
                            storageLocation = locationText.trim(),
                            itemPhotoUri = photoUri,
                        )
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GoldCategoriesManagerDialog(
    categories: List<GoldCategoryEntity>,
    onCreateCategory: (String, Long?) -> Unit,
    onDeleteOrArchive: (GoldCategoryEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var newCatName by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }
    val parents = categories.filter { it.parentCategoryId == null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gold Categories & Subcategories") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("New Category / Subcategory Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Parent (optional, makes it a subcategory)", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = selectedParentId == null,
                        onClick = { selectedParentId = null },
                        label = { Text("Top-level") },
                    )
                    parents.filter { !it.isArchived }.forEach { parent ->
                        FilterChip(
                            selected = selectedParentId == parent.id,
                            onClick = { selectedParentId = parent.id },
                            label = { Text(parent.name) },
                        )
                    }
                }
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            onCreateCategory(newCatName, selectedParentId)
                            newCatName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add Category")
                }

                HorizontalDivider()
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name + if (cat.isArchived) " (Archived)" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (cat.parentCategoryId == null) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            cat.parentCategoryId?.let { pid ->
                                Text(
                                    text = "Subcategory of ${parents.find { it.id == pid }?.name ?: "Parent"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (!cat.isPredefined) {
                            TextButton(onClick = { onDeleteOrArchive(cat) }) {
                                Text(if (cat.isArchived) "Restore" else "Delete/Archive")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
