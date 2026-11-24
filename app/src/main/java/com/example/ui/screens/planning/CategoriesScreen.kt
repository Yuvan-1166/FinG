package com.example.ui.screens.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CategoryEntity
import com.example.ui.components.CategoryIconMapper
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.parseHexColor
import com.example.ui.theme.CategoryPaletteHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditor by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var parentForNewSub by remember { mutableStateOf<CategoryEntity?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val parents = state.categories.filter { it.parentCategoryId == null }
    val childrenByParent = state.categories.filter { it.parentCategoryId != null }.groupBy { it.parentCategoryId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    parentForNewSub = null
                    showEditor = true
                },
                modifier = Modifier.testTag("add_category_fab"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add category")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = state.selectedType == "EXPENSE",
                    onClick = { viewModel.setSelectedType("EXPENSE") },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Expense") }
                SegmentedButton(
                    selected = state.selectedType == "INCOME",
                    onClick = { viewModel.setSelectedType("INCOME") },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Income") }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(parents, key = { it.id }) { parent ->
                    val subs = childrenByParent[parent.id].orEmpty()
                    val color = parseHexColor(parent.colorHex)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = if (parent.isArchived) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = CategoryIconMapper.iconFor(parent.iconName),
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = parent.name + if (parent.isArchived) " (Archived)" else "",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (subs.isNotEmpty()) {
                                        Text(
                                            text = "${subs.size} subcategor${if (subs.size == 1) "y" else "ies"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    editingCategory = null
                                    parentForNewSub = parent
                                    showEditor = true
                                }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add subcategory", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    editingCategory = parent
                                    parentForNewSub = null
                                    showEditor = true
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit category", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.toggleArchive(parent) }) {
                                    Icon(
                                        imageVector = if (parent.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                        contentDescription = "Archive category",
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                if (!parent.isDefault) {
                                    IconButton(onClick = { deletingCategory = parent }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Delete category",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }

                            if (subs.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                subs.forEach { sub ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, start = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Rounded.SubdirectoryArrowRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = sub.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f),
                                        )
                                        IconButton(onClick = {
                                            editingCategory = sub
                                            parentForNewSub = parent
                                            showEditor = true
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Edit subcategory", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { deletingCategory = sub }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete subcategory", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        CategoryEditorDialog(
            initial = editingCategory,
            parentCategory = parentForNewSub,
            type = state.selectedType,
            onDismiss = { showEditor = false },
            onSave = { name, iconName, colorHex ->
                viewModel.saveCategory(
                    editingId = editingCategory?.id,
                    name = name,
                    type = state.selectedType,
                    parentCategoryId = editingCategory?.parentCategoryId ?: parentForNewSub?.id,
                    iconName = iconName,
                    colorHex = colorHex,
                )
                showEditor = false
            },
        )
    }

    deletingCategory?.let { cat ->
        ConfirmDeleteDialog(
            title = "Delete '${cat.name}'?",
            message = "Categories used by historical transactions cannot be deleted; archive them instead so history stays intact.",
            onConfirm = {
                viewModel.deleteCategory(cat.id)
                deletingCategory = null
            },
            onDismiss = { deletingCategory = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    initial: CategoryEntity?,
    parentCategory: CategoryEntity?,
    type: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var iconName by remember { mutableStateOf(initial?.iconName ?: parentCategory?.iconName ?: "category") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: parentCategory?.colorHex ?: CategoryPaletteHex.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    initial != null -> "Edit Category"
                    parentCategory != null -> "New Subcategory in ${parentCategory.name}"
                    else -> "New $type Category"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input"),
                )
                Text("Color", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPaletteHex.forEach { hex ->
                        val c = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (colorHex == hex) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier,
                                )
                                .clickable { colorHex = hex },
                        )
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryIconMapper.selectableIcons.take(12).forEach { (key, label) ->
                        FilterChip(
                            selected = iconName == key,
                            onClick = { iconName = key },
                            leadingIcon = {
                                Icon(CategoryIconMapper.iconFor(key), contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, iconName, colorHex) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("category_save_button"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
