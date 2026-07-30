package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.makerandreas.papirusoffice.data.api.CloudDocument
import com.makerandreas.papirusoffice.data.api.FirebaseCloudManager
import kotlinx.coroutines.launch

@Composable
fun CloudSyncBar(
    currentDocumentTitle: String,
    currentDocumentContent: String,
    moduleType: String = "WRITER",
    onLoadDocumentFromCloud: (CloudDocument) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudManager = remember(context) { FirebaseCloudManager.getInstance(context) }

    val user by cloudManager.currentUser.collectAsState()
    val syncState by cloudManager.syncState.collectAsState()

    var showLoadDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // User / Account Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (user != null) MaterialTheme.colorScheme.primary else Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (user != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = "Cloud Status",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = user?.email ?: if (user != null) "Logged in (Anonymous)" else "Offline / Not Signed In",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (syncState) {
                            FirebaseCloudManager.SyncState.SYNCING -> "Syncing to Firebase & Drive..."
                            FirebaseCloudManager.SyncState.SUCCESS -> "Synced with Firebase Cloud"
                            FirebaseCloudManager.SyncState.ERROR -> "Cloud Sync Error"
                            else -> "Firestore & Google Drive Ready"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Cloud Actions (Save & Open)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (user == null) {
                    Button(
                        onClick = {
                            scope.launch {
                                val signedInUser = cloudManager.ensureSignedIn()
                                if (signedInUser != null) {
                                    Toast.makeText(context, "Signed in to Firebase Cloud!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sign In", fontSize = 11.sp)
                    }
                } else {
                    // Cloud Save
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val cloudDoc = CloudDocument(
                                    title = currentDocumentTitle.ifEmpty { "Untitled Document" },
                                    moduleType = moduleType,
                                    content = currentDocumentContent
                                )
                                val success = cloudManager.saveDocumentToCloud(cloudDoc)
                                if (success) {
                                    Toast.makeText(context, "Saved to Firebase Cloud & Drive!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Save failed. Please check network connection.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Cloud Save", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Cloud", fontSize = 11.sp)
                    }

                    // Cloud Load
                    Button(
                        onClick = { showLoadDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Cloud Load", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Cloud", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Cloud Document Load Dialog
    if (showLoadDialog) {
        CloudDocumentLoadDialog(
            cloudManager = cloudManager,
            onDismiss = { showLoadDialog = false },
            onSelectDocument = { doc ->
                onLoadDocumentFromCloud(doc)
                showLoadDialog = false
            }
        )
    }
}

@Composable
fun CloudDocumentLoadDialog(
    cloudManager: FirebaseCloudManager,
    onDismiss: () -> Unit,
    onSelectDocument: (CloudDocument) -> Unit
) {
    val scope = rememberCoroutineScope()
    var documents by remember { mutableStateOf<List<CloudDocument>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            documents = cloudManager.fetchCloudDocuments()
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Open from Firebase Cloud",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (documents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No documents saved in Cloud yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(documents) { doc ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDocument(doc) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Type: ${doc.moduleType} • ${doc.sizeBytes} bytes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Open")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
