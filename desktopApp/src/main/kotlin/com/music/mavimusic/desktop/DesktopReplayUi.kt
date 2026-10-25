package com.music.mavimusic.desktop

import com.music.mavimusic.ui.replay.ReplayCreditCard
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.data.model.artworkAt
import com.music.mavimusic.ui.icons.MaviMusicIcons
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── The banner on the Library page ──────────────────────────────────────────

// ── The page ────────────────────────────────────────────────────────────────

@Composable
internal fun DesktopReplayPage(
    summary: DesktopReplaySummary,
    period: DesktopReplayPeriod,
    holder: String,
    onPeriodChange: (DesktopReplayPeriod) -> Unit,
    onPlaySong: (Song) -> Unit,
    onOpenArtist: (String) -> Unit,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit = {},
) {
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var exportToast by remember { mutableStateOf<String?>(null) }

    // The mesh behind this page is painted by the frame, so the chrome is tinted by it too.
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
            item("heading") {
                // No arrow of its own: the top bar's back is the way out, as the gesture is on Android.
                Spacer(Modifier.height(8.dp))
                Column {
                    Column(Modifier.padding(horizontal = GUTTER)) {
                        Spacer(Modifier.height(20.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    DesktopStrings["replay", "Estadísticas y Replay"],
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.W800,
                                    color = Color.White,
                                )
                                Text(
                                    summary.label.ifBlank { period.chip },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (!summary.isEmpty) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.12f))
                                            .clickable {
                                                val exported = DesktopWrappedExporter.promptAndExport(summary, period)
                                                if (exported != null) {
                                                    exportToast = "Tarjeta guardada en: ${exported.name}"
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(MaviMusicIcons.Sparkles, contentDescription = null, tint = DesktopAccent, modifier = Modifier.size(16.dp))
                                            Text("Exportar Wrapped", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .clickable { showPrivacyDialog = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                        Text("Privacidad", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        if (exportToast != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                exportToast!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = DesktopAccent,
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        PeriodPicker(period, onPeriodChange)
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }

            if (summary.isEmpty) {
                item("empty") { EmptyReplay(period) }
                return@LazyColumn
            }

            item("cards") {
                DesktopScrollableRow(gutter = GUTTER, spacing = 12.dp) {
                    items(summary.heroCards(), key = { it.label }) { card ->
                        ReplayCreditCard(
                            label = card.label,
                            value = card.value,
                            detail = card.detail,
                            artworkUrl = card.artworkUrl,
                            holder = holder,
                            memberSince = summary.memberSince(),
                            onClick = {},
                            modifier = Modifier.width(300.dp),
                        )
                    }
                }
            }

            chart(
                key = "songs",
                title = DesktopStrings["top_songs", "Top Songs"],
                rows = summary.songRows(CHART_LENGTH),
                onClick = { index -> summary.songs.getOrNull(index)?.let { onPlaySong(it.song) } },
            )
            chart(
                key = "artists",
                title = DesktopStrings["top_artists", "Top Artists"],
                rows = summary.artistRows(CHART_LENGTH),
                circular = true,
                onClick = { index -> summary.artists.getOrNull(index)?.let { onOpenArtist(it.title) } },
            )
            chart(
                key = "albums",
                title = DesktopStrings["top_albums", "Top Albums"],
                rows = summary.albumRows(CHART_LENGTH),
                onClick = {},
            )

            item("habits") { Habits(summary) }
            item("tail") { Spacer(Modifier.height(32.dp)) }
        }

        if (showPrivacyDialog) {
            StatsPrivacyDialog(
                onDismiss = { showPrivacyDialog = false },
                onRefresh = onRefresh,
            )
        }
    }
}

@Composable
private fun PeriodPicker(selected: DesktopReplayPeriod, onSelect: (DesktopReplayPeriod) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        DesktopReplayPeriod.entries.forEach { period ->
            val active = period == selected
            val background by animateColorAsState(
                if (active) Color.White.copy(alpha = 0.92f) else Color.Transparent,
                tween(160),
                label = "periodChip",
            )
            Text(
                period.chip,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W700,
                color = if (active) Color.Black else Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(background)
                    .clickable { onSelect(period) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

// ── The credit card ─────────────────────────────────────────────────────────

// ── Charts ──────────────────────────────────────────────────────────────────

private fun LazyListScope.chart(
    key: String,
    title: String,
    rows: List<DesktopReplayRow>,
    onClick: (Int) -> Unit,
    circular: Boolean = false,
) {
    if (rows.isEmpty()) return
    item("$key-title") {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W800,
            color = Color.White,
            modifier = Modifier.padding(start = GUTTER, end = GUTTER, top = 8.dp, bottom = 10.dp),
        )
    }
    items(rows, key = { "$key-${it.key}" }) { row ->
        ChartRow(row, circular) { onClick(row.rank - 1) }
    }
    item("$key-gap") { Spacer(Modifier.height(20.dp)) }
}

@Composable
private fun ChartRow(row: DesktopReplayRow, circular: Boolean, onClick: () -> Unit) {
    val shape = if (circular) CircleShape else RoundedCornerShape(6.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = GUTTER, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(row.rank)
        if (row.artworkUrl != null) {
            DesktopArtwork(row.artworkUrl, Modifier.size(48.dp).clip(shape), px = ROW_ART_PX)
        } else {
            InitialTile(row.title, 48.dp, shape)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                row.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.W600,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(row.subtitle, formatListening(row.ms)).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (row.plays > 0) {
            Text(
                row.plays.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.45f),
            )
        }
    }
}

/** Number one is the accent; the rest are quiet, because a chart where every position shouts has
 * no first place. */
@Composable
private fun RankBadge(rank: Int) {
    Text(
        rank.toString(),
        style = if (rank == 1) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.W800,
        color = if (rank == 1) DesktopAccent else Color.White.copy(alpha = 0.45f),
        modifier = Modifier.width(28.dp),
    )
}

/** A stand-in cover for a row that has no artwork, coloured from the word itself. */
@Composable
private fun InitialTile(text: String, size: Dp, shape: Shape) {
    val hue = (text.hashCode().toFloat() % 360f + 360f) % 360f
    val color = Color.hsl(hue, 0.55f, 0.45f)
    Box(
        Modifier.size(size).clip(shape)
            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f)))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text.take(1).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W800,
            color = Color.White,
        )
    }
}

// ── Tail ────────────────────────────────────────────────────────────────────

@Composable
private fun Habits(summary: DesktopReplaySummary) {
    Column(Modifier.padding(horizontal = GUTTER)) {
        Text(
            DesktopStrings["d_listening_shape", "Listening shape"],
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W800,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("CANCIONES", summary.distinctSongs.toString(), Modifier.weight(1f))
            StatTile("ARTISTAS", summary.distinctArtists.toString(), Modifier.weight(1f))
            StatTile("ÁLBUMES", summary.distinctAlbums.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
            StatTile("RACHA ACTIVA", "${summary.streakDays} ${if (summary.streakDays == 1) "día" else "días"}", Modifier.weight(1f))
            val comp = summary.comparisonPercent
            val compText = if (comp != null) (if (comp >= 0) "+$comp%" else "$comp%") else "—"
            StatTile("VS PERÍODO ANTERIOR", compText, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        WeeklyHeatmap(summary.dayOfWeekDistribution)

        summary.peakHour?.let {
            Spacer(Modifier.height(10.dp))
            Note("Escuchas más música alrededor de las ${formatHour(it)}.")
        }
        summary.busiestDay?.let {
            Spacer(Modifier.height(6.dp))
            Note("Tu día récord fue ${formatDay(it)} — ${formatListening(summary.busiestDayMs)}.")
        }
    }
}

@Composable
private fun WeeklyHeatmap(distribution: List<Long>) {
    val dayNames = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    val maxMs = (distribution.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(16.dp),
    ) {
        Text(
            "DISTRIBUCIÓN SEMANAL",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().height(90.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            distribution.forEachIndexed { index, ms ->
                val fraction = (ms.toFloat() / maxMs.toFloat()).coerceIn(0.06f, 1f)
                val day = dayNames.getOrElse(index) { "" }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        Modifier
                            .width(22.dp)
                            .height((64.dp * fraction).coerceAtLeast(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (ms > 0L) DesktopAccent.copy(alpha = (0.4f + 0.6f * fraction).coerceAtMost(1f))
                                else Color.White.copy(alpha = 0.08f)
                            ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        day,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ms > 0L) Color.White else Color.White.copy(alpha = 0.4f),
                        fontWeight = if (ms > 0L) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsPrivacyDialog(
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
) {
    var tracking by remember { mutableStateOf(DesktopListeningStats.isTrackingEnabled()) }
    var confirmingClear by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 480) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = DesktopAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Privacidad de Estadísticas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Tus estadísticas se calculan y almacenan exclusivamente en tu Mac de forma 100% privada y local. MaviMusic no envía telemetría a servidores remotos.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
            )

            Spacer(Modifier.height(18.dp))

            // Tracking Switch Row
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        "Registrar estadísticas locales",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Text(
                        "Al apagarlo, la app no registrará nuevas canciones reproducidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
                Switch(
                    checked = tracking,
                    onCheckedChange = {
                        tracking = it
                        DesktopListeningStats.setTrackingEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DesktopAccent,
                    ),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Export Actions
            Text(
                "EXPORTAR TUS DATOS",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val exported = DesktopWrappedExporter.promptAndExportData(
                            DesktopListeningStats.exportJson(),
                            "MaviMusic-Stats.json",
                            "Guardar estadísticas (JSON)"
                        )
                        if (exported != null) toast = "Exportado: ${exported.name}"
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Exportar JSON", color = Color.White)
                }
                OutlinedButton(
                    onClick = {
                        val exported = DesktopWrappedExporter.promptAndExportData(
                            DesktopListeningStats.exportCsv(),
                            "MaviMusic-Stats.csv",
                            "Guardar estadísticas (CSV)"
                        )
                        if (exported != null) toast = "Exportado: ${exported.name}"
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Exportar CSV", color = Color.White)
                }
            }

            Spacer(Modifier.height(18.dp))

            // Clear stats
            if (!confirmingClear) {
                OutlinedButton(
                    onClick = { confirmingClear = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(255, 90, 90),
                    ),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Borrar todo el historial de estadísticas")
                }
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(255, 50, 50).copy(alpha = 0.15f))
                        .padding(14.dp),
                ) {
                    Text(
                        "¿Estás seguro de que deseas borrar tus estadísticas?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        "Esta acción es irreversible y reseteará tu Replay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                DesktopListeningStats.clear()
                                onRefresh()
                                toast = "Historial eliminado correctamente"
                                confirmingClear = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(220, 50, 50)),
                        ) {
                            Text("Sí, borrar todo", color = Color.White)
                        }
                        TextButton(onClick = { confirmingClear = false }) {
                            Text("Cancelar", color = Color.White)
                        }
                    }
                }
            }

            if (toast != null) {
                Spacer(Modifier.height(12.dp))
                Text(toast!!, color = DesktopAccent, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.W800,
            color = Color.White,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun Note(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.6f),
        modifier = modifier,
    )
}

@Composable
private fun EmptyReplay(period: DesktopReplayPeriod) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.Schedule,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            DesktopStrings["not_enough_listening", "Not enough listening yet"],
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when (period) {
                DesktopReplayPeriod.THIS_MONTH -> "Nothing this month. Play some music and it will fill up."
                else -> "Play some music and your Replay will build itself."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

// ── The mesh behind all of it ───────────────────────────────────────────────

/** The artwork's own colours, averaged into a mesh and held still. */
@Composable
internal fun DesktopMesh(url: String?) {
    val mesh by produceState<ImageBitmap?>(initialValue = null, key1 = url) {
        value = withContext(Dispatchers.IO) {
            DesktopArtworkCache.load(url.artworkAt(MESH_SOURCE_PX) ?: url)
                ?.let { DesktopArtworkMesh.of(it, seed = url.hashCode()) }
        }
    }
    val texture = mesh
    if (texture == null) {
        Box(Modifier.fillMaxSize().background(DesktopBackground))
        return
    }
    Canvas(Modifier.fillMaxSize()) {
        drawImage(
            image = texture,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Low,
        )
    }
}

// ── Rows and cards, derived from a summary ──────────────────────────────────

/** One row of a chart. */
data class DesktopReplayRow(
    val key: String,
    val rank: Int,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val ms: Long,
    val plays: Int,
)

/** One of the cards along the top of the page. */
data class DesktopReplayHeroCard(
    val label: String,
    val value: String,
    val detail: String?,
    val artworkUrl: String?,
)

internal fun DesktopReplaySummary.songRows(limit: Int): List<DesktopReplayRow> =
    songs.take(limit).mapIndexed { index, entry ->
        DesktopReplayRow(
            key = entry.song.videoId,
            rank = index + 1,
            title = entry.song.title,
            subtitle = entry.song.artist.ifBlank { null },
            artworkUrl = entry.song.thumbnailUrl,
            ms = entry.listenedMs,
            plays = entry.plays,
        )
    }

internal fun DesktopReplaySummary.artistRows(limit: Int): List<DesktopReplayRow> =
    artists.take(limit).mapIndexed { index, entry -> entry.toRow(index) }

internal fun DesktopReplaySummary.albumRows(limit: Int): List<DesktopReplayRow> =
    albums.take(limit).mapIndexed { index, entry -> entry.toRow(index) }

private fun DesktopRankedEntry.toRow(index: Int) = DesktopReplayRow(
    key = title,
    rank = index + 1,
    title = title,
    subtitle = subtitle,
    artworkUrl = artworkUrl,
    ms = ms,
    plays = plays,
)

/**
 * The headline facts, in the order they are dealt.
 *
 * Minutes leads because it is the one figure that needs no context to mean something. A category
 * with nothing in it is left out rather than shown empty.
 */
internal fun DesktopReplaySummary.heroCards(): List<DesktopReplayHeroCard> = buildList {
    add(
        DesktopReplayHeroCard(
            label = DesktopStrings["minutes_listened", "Minutes listened"],
            value = grouped(minutes),
            detail = "${plays(totalPlays)} · ${label.ifBlank { period.chip }}",
            artworkUrl = songs.firstOrNull()?.song?.thumbnailUrl,
        ),
    )
    artists.firstOrNull()?.let {
        add(
            DesktopReplayHeroCard(
                label = DesktopStrings["top_artist", "Top artist"],
                value = it.title,
                detail = "${formatListening(it.ms)} · ${plays(it.plays)}",
                artworkUrl = it.artworkUrl,
            ),
        )
    }
    songs.firstOrNull()?.let {
        add(
            DesktopReplayHeroCard(
                label = DesktopStrings["top_song", "Top song"],
                value = it.song.title,
                detail = "${it.song.artist} · ${plays(it.plays)}",
                artworkUrl = it.song.thumbnailUrl,
            ),
        )
    }
    albums.firstOrNull()?.let {
        add(
            DesktopReplayHeroCard(
                label = DesktopStrings["top_album", "Top album"],
                value = it.title,
                detail = listOfNotNull(it.subtitle, formatListening(it.ms)).joinToString(" · "),
                artworkUrl = it.artworkUrl,
            ),
        )
    }
}

/** `MM/YY` of the first month with anything in it. */
internal fun DesktopReplaySummary.memberSince(): String? = since
    ?.let { runCatching { java.time.YearMonth.parse(it) }.getOrNull() }
    ?.let { "%02d/%02d".format(it.monthValue, it.year % 100) }

// ── Formatting ──────────────────────────────────────────────────────────────

internal fun grouped(value: Long): String = String.format(Locale.US, "%,d", value)

private fun plays(count: Int): String = if (count == 1) "1 play" else "${grouped(count.toLong())} plays"

internal fun formatListening(ms: Long): String {
    val minutes = ms / 60_000L
    if (minutes < 60) return if (minutes == 1L) "1 min" else "$minutes mins"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0L) "${grouped(hours)} hr" else "${grouped(hours)} hr $rest min"
}

private fun formatHour(hour: Int): String = when (hour) {
    0 -> "midnight"
    12 -> "midday"
    in 1..11 -> "$hour am"
    else -> "${hour - 12} pm"
}

private fun formatDay(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
}.getOrDefault(iso)

/** How far down each chart the page goes. A top five is a result; a top ten is a list. */
private const val CHART_LENGTH = 5

private const val ROW_ART_PX = 120
private const val MESH_SOURCE_PX = 120
/** The page margin, shared with every other page — see [DesktopPageGutter]. */
private val GUTTER = DesktopPageGutter
