package com.music.mavimusic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.music.mavimusic.ui.icons.MaviMusicIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopSmartPlaylistDialog(
    initialPlaylist: DesktopPlaylist? = null,
    onSave: (title: String, ruleSet: SmartPlaylistRuleSet) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initialPlaylist?.title ?: "Nueva lista inteligente") }
    val initialRuleSet = remember {
        initialPlaylist?.smartRuleDefinition?.let { SmartRuleSerializer.decode(it) }
            ?: SmartPlaylistRuleSet(
                matchMode = SmartRuleMatchMode.ALL,
                conditions = listOf(
                    SmartRuleCondition(SmartRuleField.PLAY_COUNT, SmartRuleOperator.GREATER_THAN, "2")
                ),
                sortBy = SmartSortField.PLAY_COUNT,
                sortDescending = true,
                limit = 50,
                autoUpdate = true,
            )
    }

    var matchMode by remember { mutableStateOf(initialRuleSet.matchMode) }
    var sortBy by remember { mutableStateOf(initialRuleSet.sortBy) }
    var sortDescending by remember { mutableStateOf(initialRuleSet.sortDescending) }
    var limit by remember { mutableStateOf(initialRuleSet.limit) }
    var autoUpdate by remember { mutableStateOf(initialRuleSet.autoUpdate) }

    val conditions = remember {
        mutableStateListOf<SmartRuleCondition>().apply {
            addAll(initialRuleSet.conditions)
        }
    }

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 580) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(panelInset(22.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        MaviMusicIcons.Sparkles,
                        contentDescription = null,
                        tint = DesktopAccent,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (initialPlaylist == null) "Nueva lista inteligente" else "Editar reglas de lista",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(DesktopStrings["cancel", "Cancelar"], color = DesktopSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Title field
            Text(
                "Nombre de la lista",
                style = MaterialTheme.typography.labelSmall,
                color = DesktopSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    cursorBrush = SolidColor(DesktopAccent),
                    modifier = Modifier.weight(1f),
                )
                if (title.isNotBlank()) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).clickable { title = "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = DesktopSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Pre-built Templates
            Text(
                "Plantillas listas para usar",
                style = MaterialTheme.typography.labelSmall,
                color = DesktopSecondary,
            )
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmartPlaylistTemplates.TEMPLATES.forEach { template ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            if (title == "Nueva lista inteligente" || title.isBlank()) {
                                title = template.name
                            }
                            matchMode = template.ruleSet.matchMode
                            sortBy = template.ruleSet.sortBy
                            sortDescending = template.ruleSet.sortDescending
                            limit = template.ruleSet.limit
                            autoUpdate = template.ruleSet.autoUpdate
                            conditions.clear()
                            conditions.addAll(template.ruleSet.conditions)
                        },
                        label = { Text(template.name) },
                        colors = desktopChipColors(),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Match Mode (AND / OR)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Combinación de reglas",
                    style = MaterialTheme.typography.labelSmall,
                    color = DesktopSecondary,
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (matchMode == SmartRuleMatchMode.ALL) DesktopAccent else Color.Transparent)
                            .clickable { matchMode = SmartRuleMatchMode.ALL }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Y (Todas)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (matchMode == SmartRuleMatchMode.ALL) FontWeight.Bold else FontWeight.Normal,
                            color = if (matchMode == SmartRuleMatchMode.ALL) Color.Black else Color.White,
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (matchMode == SmartRuleMatchMode.ANY) DesktopAccent else Color.Transparent)
                            .clickable { matchMode = SmartRuleMatchMode.ANY }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "O (Cualquiera)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (matchMode == SmartRuleMatchMode.ANY) FontWeight.Bold else FontWeight.Normal,
                            color = if (matchMode == SmartRuleMatchMode.ANY) Color.Black else Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Rule Conditions List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                conditions.forEachIndexed { index, cond ->
                    ConditionEditorRow(
                        condition = cond,
                        onUpdate = { updated -> conditions[index] = updated },
                        onRemove = { conditions.removeAt(index) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Add condition button
            Row(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        conditions.add(
                            SmartRuleCondition(
                                field = SmartRuleField.ARTIST,
                                operator = SmartRuleOperator.CONTAINS,
                                value = "",
                            ),
                        )
                    }
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, null, tint = DesktopAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Añadir condición",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DesktopAccent,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Sort and Limit Options
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Ordenar por",
                        style = MaterialTheme.typography.labelSmall,
                        color = DesktopSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = sortBy == SmartSortField.PLAY_COUNT,
                            onClick = { sortBy = SmartSortField.PLAY_COUNT },
                            label = { Text("Reproducciones") },
                            colors = desktopChipColors(),
                        )
                        FilterChip(
                            selected = sortBy == SmartSortField.DATE_ADDED,
                            onClick = { sortBy = SmartSortField.DATE_ADDED },
                            label = { Text("Fecha adición") },
                            colors = desktopChipColors(),
                        )
                        FilterChip(
                            selected = sortBy == SmartSortField.BPM,
                            onClick = { sortBy = SmartSortField.BPM },
                            label = { Text("BPM") },
                            colors = desktopChipColors(),
                        )
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { sortDescending = !sortDescending }
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (sortDescending) "Descendente ↓" else "Ascendente ↑",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Auto-update switch
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Actualización automática",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                    Text(
                        "Recalcula la lista al reproducir, dar 'Me gusta' o añadir temas",
                        style = MaterialTheme.typography.bodySmall,
                        color = DesktopSecondary,
                    )
                }
                Switch(
                    checked = autoUpdate,
                    onCheckedChange = { autoUpdate = it },
                    colors = desktopSwitchColors(),
                )
            }

            Spacer(Modifier.height(20.dp))

            // Bottom action buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(DesktopStrings["cancel", "Cancelar"], color = DesktopSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DesktopAccent)
                        .clickable {
                            val ruleSet = SmartPlaylistRuleSet(
                                matchMode = matchMode,
                                conditions = conditions.toList(),
                                sortBy = sortBy,
                                sortDescending = sortDescending,
                                limit = limit,
                                autoUpdate = autoUpdate,
                            )
                            onSave(title.ifBlank { "Lista inteligente" }, ruleSet)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "Guardar lista inteligente",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConditionEditorRow(
    condition: SmartRuleCondition,
    onUpdate: (SmartRuleCondition) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Field selector pill
            Text(
                condition.field.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = DesktopAccent,
            )
            Box(
                Modifier.size(22.dp).clip(CircleShape).clickable { onRemove() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, null, tint = DesktopSecondary, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Operator buttons
            Row(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clickable {
                        val nextOp = when (condition.operator) {
                            SmartRuleOperator.IS -> SmartRuleOperator.IS_NOT
                            SmartRuleOperator.IS_NOT -> SmartRuleOperator.CONTAINS
                            SmartRuleOperator.CONTAINS -> SmartRuleOperator.GREATER_THAN
                            SmartRuleOperator.GREATER_THAN -> SmartRuleOperator.LESS_THAN
                            SmartRuleOperator.LESS_THAN -> SmartRuleOperator.IN_LAST_DAYS
                            SmartRuleOperator.IN_LAST_DAYS -> SmartRuleOperator.IS_EMPTY
                            else -> SmartRuleOperator.IS
                        }
                        onUpdate(condition.copy(operator = nextOp))
                    },
            ) {
                Text(
                    condition.operator.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }

            // Value field
            if (condition.operator != SmartRuleOperator.IS_EMPTY) {
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = condition.value,
                        onValueChange = { onUpdate(condition.copy(value = it)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        cursorBrush = SolidColor(DesktopAccent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Text(
                    "Sin valor / dato ausente",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesktopSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (condition.field == SmartRuleField.BPM) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Nota: si el BPM no está disponible, la pista se excluirá sin inventar valores.",
                style = MaterialTheme.typography.labelSmall,
                color = DesktopSecondary.copy(alpha = 0.8f),
            )
        }
    }
}
