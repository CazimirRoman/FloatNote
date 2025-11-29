package dev.cazimir.floatnote.feature.home

import android.content.Context
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.animation.togetherWith
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.cazimir.floatnote.R
import dev.cazimir.floatnote.core.ui.theme.FloatNoteTheme

@Composable
fun MainScreen(
    hasPermission: Boolean,
    isServiceRunning: Boolean,
    recentNotes: List<String>,
    onDeleteNote: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        ) {
            Text(
                text = "🔵", // Placeholder for icon
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Service Control Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!hasPermission) {
                    Text(
                        text = "Overlay Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "FloatNote needs permission to display over other apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Grant Permission")
                    }
                } else {
                    Text(
                        text = if (isServiceRunning) "Service Active" else "Service Inactive",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isServiceRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isServiceRunning)
                            "The floating bubble is ready. Tap it to start recording."
                        else
                            "Start the service to enable the floating bubble.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = if (isServiceRunning) onStopService else onStartService,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning)
                                MaterialTheme.colorScheme.surfaceVariant
                            else
                                MaterialTheme.colorScheme.primary,
                            contentColor = if (isServiceRunning)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isServiceRunning) "Stop Service" else "Start Service",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Recent Notes Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.animation.AnimatedContent(
            targetState = recentNotes.isEmpty(),
            transitionSpec = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) togetherWith
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300))
            },
            label = "RecentNotesTransition"
        ) { isEmpty ->
            if (isEmpty) {
                EmptyRecentNotes(modifier = Modifier.fillMaxWidth().weight(1f))
            } else {
                RecentNotesList(notes = recentNotes, onDeleteNote = onDeleteNote, modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyRecentNotes(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No recent notes yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun RecentNotesList(notes: List<String>, onDeleteNote: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(
            items = notes,
            key = { it } // Use note content as key since we ensure uniqueness
        ) { note ->
            RecentNoteItem(
                note = note,
                onDelete = { onDeleteNote(note) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun RecentNoteItem(
    note: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            kotlinx.coroutines.delay(300) // Wait for animation
            onDelete()
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Copy button
                    FilledTonalIconButton(
                        onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("FloatNote", note)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(
                                context,
                                "Copied to clipboard",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Share button
                    FilledTonalIconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, note)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Delete button
                    FilledTonalIconButton(
                        onClick = { isVisible = false },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Previews
@Preview(showBackground = true, name = "Main - No Permission")
@Composable
private fun PreviewMainNoPermission() {
    FloatNoteTheme(darkTheme = false) {
        MainScreen(
            hasPermission = false,
            isServiceRunning = false,
            recentNotes = emptyList(),
            onDeleteNote = {},
            onRequestPermission = {},
            onStartService = {},
            onStopService = {}
        )
    }
}

@Preview(showBackground = true, name = "Main - Empty Notes")
@Composable
private fun PreviewMainEmptyNotes() {
    FloatNoteTheme(darkTheme = false) {
        MainScreen(
            hasPermission = true,
            isServiceRunning = false,
            recentNotes = emptyList(),
            onDeleteNote = {},
            onRequestPermission = {},
            onStartService = {},
            onStopService = {}
        )
    }
}

@Preview(showBackground = true, name = "Main - Service Running")
@Composable
private fun PreviewMainServiceRunning() {
    FloatNoteTheme(darkTheme = false) {
        MainScreen(
            hasPermission = true,
            isServiceRunning = true,
            recentNotes = listOf("Example note one", "Example note two"),
            onDeleteNote = {},
            onRequestPermission = {},
            onStartService = {},
            onStopService = {}
        )
    }
}

@Preview(showBackground = true, name = "Main - Populated")
@Composable
private fun PreviewMainPopulated() {
    FloatNoteTheme(darkTheme = false) {
        MainScreen(
            hasPermission = true,
            isServiceRunning = false,
            recentNotes = List(5) { "Sample note #$it with some longer content to show ellipsis behavior" },
            onDeleteNote = {},
            onRequestPermission = {},
            onStartService = {},
            onStopService = {}
        )
    }
}

@Preview(showBackground = true, name = "Main - Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewMainDark() {
    FloatNoteTheme(darkTheme = true) {
        MainScreen(
            hasPermission = true,
            isServiceRunning = false,
            recentNotes = emptyList(),
            onDeleteNote = {},
            onRequestPermission = {},
            onStartService = {},
            onStopService = {}
        )
    }
}
