package com.example.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

class OpenDocumentWithUri : ActivityResultContract<Uri?, Uri?>() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            if (input != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, input)
            }
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
    }
}

@Composable
fun FilesSubPage(
    onNavigateToModule: (String) -> Unit
) {
    val context = LocalContext.current
    
    val openDocumentLauncher = rememberLauncherForActivityResult(OpenDocumentWithUri()) { uri ->
        uri?.let {
            try {
                // Copy to cache dir to ensure LibreOfficeKit can read it as a local File
                val cacheFile = File(context.cacheDir, "imported_document_tmp")
                context.contentResolver.openInputStream(it)?.use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                var displayName = "document"
                val cursor = context.contentResolver.query(it, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex)
                    }
                    cursor.close()
                }
                
                val lowerName = displayName.lowercase()
                val fileType = when {
                    lowerName.endsWith(".ods") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || lowerName.endsWith(".csv") -> "Cellina"
                    lowerName.endsWith(".odp") || lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt") -> "Slidia"
                    lowerName.endsWith(".pdf") -> "Pagella"
                    else -> "Inky" // ODT, DOCX, DOC, TXT, etc
                }
                
                val targetFile = File(context.cacheDir, displayName)
                if (targetFile.exists()) targetFile.delete()
                cacheFile.renameTo(targetFile)
                
                RecentFilesTracker.addFile(context, targetFile.absolutePath, fileType)
                com.example.MainActivity.openedFilePath = targetFile.absolutePath
                com.example.MainActivity.openedFileType = fileType
                onNavigateToModule(fileType)
                Toast.makeText(context, "Opening $displayName...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val externalStorageDirs = remember { 
        androidx.core.content.ContextCompat.getExternalFilesDirs(context, null) 
    }
    val hasRemovableStorage = externalStorageDirs.size > 1 && externalStorageDirs[1] != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "STORAGE SHORTCUTS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ShortcutCard(
            title = "Internal Storage",
            path = "/storage/emulated/0",
            description = "Main device storage directory",
            icon = Icons.Rounded.Folder
        ) {
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3A")
            openDocumentLauncher.launch(uri)
        }

        ShortcutCard(
            title = "Documents",
            path = "/storage/emulated/0/Documents",
            description = "Default document drafts and sheets",
            icon = Icons.Rounded.Article
        ) {
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments")
            openDocumentLauncher.launch(uri)
        }

        ShortcutCard(
            title = "Downloads",
            path = "/storage/emulated/0/Downloads",
            description = "Exported files and web downloads",
            icon = Icons.Rounded.Download
        ) {
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
            openDocumentLauncher.launch(uri)
        }

        if (hasRemovableStorage) {
            ShortcutCard(
                title = "External Storage (SD Card)",
                path = "Removable Volume",
                description = "Removable secondary storage volume",
                icon = Icons.Rounded.SdCard
            ) {
                openDocumentLauncher.launch(null) // Launch without initial URI
            }
        }
    }
}
