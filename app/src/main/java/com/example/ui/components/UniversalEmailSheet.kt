package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makerandreas.papirusoffice.data.framework.PapirusEmailEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalEmailSheet(
    activeModuleName: String = "Inky", // "Inky", "Cellina", "Slidia"
    docTitle: String = "Report.odt",
    docContent: String = "This is a sample document content to be shared.",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Email & Share, 1 = SDK Java Examples, 2 = Zawinski's Law Help

    // Input States
    var recipientEmail by remember { mutableStateOf("recipient@example.com") }
    var emailSubject by remember { mutableStateOf("Shared via Papirus Office ($activeModuleName)") }
    var emailBody by remember { mutableStateOf("Hi,\n\nPlease find my shared office document attached below.\n\nSent via Papirus Office ($activeModuleName Engine).") }

    // SMTP Config State
    var smtpHost by remember { mutableStateOf("smtp.gmail.com") }
    var smtpPort by remember { mutableStateOf("587") }
    var smtpUser by remember { mutableStateOf("user@gmail.com") }
    var smtpPass by remember { mutableStateOf("app_password") }
    var useSsl by remember { mutableStateOf(true) }

    // Fallbacks State
    var targetPhone by remember { mutableStateOf("+628123456789") }
    var selectedMessengerPackage by remember { mutableStateOf("com.whatsapp") } // com.whatsapp or org.telegram.messenger

    // SDK Code Examples
    val sdkExamples = remember {
        listOf(
            "SendEmailByClient.java" to "SimpleSystemMail and SimpleCommandMail examples utilizing system default email client.",
            "SendEmailByProvider.java" to "MailServiceProvider example establishing socket links directly with SMTP servers (SSL/TLS)."
        )
    }
    var selectedExampleFile by remember { mutableStateOf(sdkExamples.first().first) }
    var sdkCodeContent by remember { mutableStateOf("Loading SDK code example...") }

    // Live Logs State
    var liveLogs by remember { mutableStateOf(listOf<String>()) }

    // Read example content
    LaunchedEffect(selectedExampleFile) {
        sdkCodeContent = try {
            context.assets.open("sdk_examples/$selectedExampleFile").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "// Failed to load example: ${e.localizedMessage}"
        }
    }

    // Refresh Logs Helper
    val refreshLogs = {
        liveLogs = PapirusEmailEngine.getLogs()
    }

    // Initialize logs
    LaunchedEffect(Unit) {
        PapirusEmailEngine.clearLogs()
        refreshLogs()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Email,
                                    contentDescription = "Email Framework",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Papirus Email & Sharing Framework",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "LibreOffice SDK Ch.42 & Native Android Share Sheet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Email & Share") },
                        icon = { Icon(Icons.Rounded.Send, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("SDK Java Examples") },
                        icon = { Icon(Icons.Rounded.Code, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Zawinski's Law Help") },
                        icon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Contents
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> EmailAndShareTab(
                            context = context,
                            coroutineScope = coroutineScope,
                            activeModuleName = activeModuleName,
                            docTitle = docTitle,
                            docContent = docContent,
                            recipientEmail = recipientEmail,
                            onRecipientEmailChange = { recipientEmail = it },
                            emailSubject = emailSubject,
                            onEmailSubjectChange = { emailSubject = it },
                            emailBody = emailBody,
                            onEmailBodyChange = { emailBody = it },
                            smtpHost = smtpHost,
                            onSmtpHostChange = { smtpHost = it },
                            smtpPort = smtpPort,
                            onSmtpPortChange = { smtpPort = it },
                            smtpUser = smtpUser,
                            onSmtpUserChange = { smtpUser = it },
                            smtpPass = smtpPass,
                            onSmtpPassChange = { smtpPass = it },
                            useSsl = useSsl,
                            onUseSslChange = { useSsl = it },
                            targetPhone = targetPhone,
                            onTargetPhoneChange = { targetPhone = it },
                            selectedMessengerPackage = selectedMessengerPackage,
                            onSelectedMessengerPackageChange = { selectedMessengerPackage = it },
                            liveLogs = liveLogs,
                            onRefreshLogs = refreshLogs
                        )
                        1 -> SdkExamplesTab(
                            sdkExamples = sdkExamples,
                            selectedExampleFile = selectedExampleFile,
                            onSelectedExampleFileChange = { selectedExampleFile = it },
                            sdkCodeContent = sdkCodeContent,
                            onCopyClick = {
                                clipboardManager.setText(AnnotatedString(sdkCodeContent))
                                Toast.makeText(context, "SDK Example copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        2 -> ZawinskisLawHelpTab()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailAndShareTab(
    context: android.content.Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    activeModuleName: String,
    docTitle: String,
    docContent: String,
    recipientEmail: String,
    onRecipientEmailChange: (String) -> Unit,
    emailSubject: String,
    onEmailSubjectChange: (String) -> Unit,
    emailBody: String,
    onEmailBodyChange: (String) -> Unit,
    smtpHost: String,
    onSmtpHostChange: (String) -> Unit,
    smtpPort: String,
    onSmtpPortChange: (String) -> Unit,
    smtpUser: String,
    onSmtpUserChange: (String) -> Unit,
    smtpPass: String,
    onSmtpPassChange: (String) -> Unit,
    useSsl: Boolean,
    onUseSslChange: (Boolean) -> Unit,
    targetPhone: String,
    onTargetPhoneChange: (String) -> Unit,
    selectedMessengerPackage: String,
    onSelectedMessengerPackageChange: (String) -> Unit,
    liveLogs: List<String>,
    onRefreshLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Recipient Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Recipient & Message Payload",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = recipientEmail,
                    onValueChange = onRecipientEmailChange,
                    label = { Text("Recipient Email Address") },
                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emailSubject,
                    onValueChange = onEmailSubjectChange,
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emailBody,
                    onValueChange = onEmailBodyChange,
                    label = { Text("Email Body Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dispatch Options (The 3 main ways + fallbacks)
        Text(
            text = "2. Select Dispatch Method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        // Native Share Sheet Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    PapirusEmailEngine.triggerAndroidShareSheet(context, docTitle, emailBody, null)
                    onRefreshLogs()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Native Share Sheet")
            }

            Button(
                onClick = {
                    PapirusEmailEngine.sendEmailViaSystemClient(context, recipientEmail, emailSubject, emailBody, null)
                    onRefreshLogs()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.Mail, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("System Client")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Firebase Google Workspace
        Button(
            onClick = {
                coroutineScope.launch {
                    PapirusEmailEngine.sendEmailViaFirebaseWorkspace(
                        context,
                        recipientEmail,
                        emailSubject,
                        docTitle,
                        docContent,
                        activeModuleName.uppercase()
                    )
                    onRefreshLogs()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Rounded.Cloud, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send via Firebase Google Workspace Drive Link")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SMTP Connection Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SMTP Server Settings (MailServiceProvider)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = smtpHost,
                        onValueChange = onSmtpHostChange,
                        label = { Text("SMTP Host") },
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = smtpPort,
                        onValueChange = onSmtpPortChange,
                        label = { Text("Port") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = smtpUser,
                        onValueChange = onSmtpUserChange,
                        label = { Text("Username") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = smtpPass,
                        onValueChange = onSmtpPassChange,
                        label = { Text("Password / Token") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = useSsl, onCheckedChange = onUseSslChange)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Secure SSL/TLS Handshake", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = PapirusEmailEngine.sendEmailViaSMTP(
                                host = smtpHost,
                                port = smtpPort.toIntOrNull() ?: 587,
                                sslEnabled = useSsl,
                                user = smtpUser,
                                pass = smtpPass,
                                recipient = recipientEmail,
                                subject = emailSubject,
                                body = emailBody,
                                attachmentPath = null
                            )
                            onRefreshLogs()
                            if (success) {
                                Toast.makeText(context, "SMTP Dispatch successful!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "SMTP Connection Failed. Check console logs.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Execute Direct SMTP Handshake")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct Messenger Fallback Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Direct Chat & RCS Fallbacks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetPhone,
                    onValueChange = onTargetPhoneChange,
                    label = { Text("Target Phone Number / URI") },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedMessengerPackage == "com.whatsapp",
                        onClick = { onSelectedMessengerPackageChange("com.whatsapp") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = selectedMessengerPackage == "org.telegram.messenger",
                        onClick = { onSelectedMessengerPackageChange("org.telegram.messenger") }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Telegram", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            PapirusEmailEngine.triggerRcsFallback(context, targetPhone, emailBody, null)
                            onRefreshLogs()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.inverseSurface),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Sms, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Messages RCS")
                    }

                    Button(
                        onClick = {
                            PapirusEmailEngine.triggerChatAppFallback(context, selectedMessengerPackage, emailBody, null)
                            onRefreshLogs()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Direct Chat App")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Realtime Console Logs Card (Crucial for Galaxy A11 & Realme C3 optimization trace)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Realtime Terminal Logs",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Green,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            PapirusEmailEngine.clearLogs()
                            onRefreshLogs()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Clear logs", tint = Color.Green)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        if (liveLogs.isEmpty()) {
                            Text(
                                text = "No execution records found.",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color.LightGray
                            )
                        } else {
                            liveLogs.forEach { log ->
                                Text(
                                    text = log,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SdkExamplesTab(
    sdkExamples: List<Pair<String, String>>,
    selectedExampleFile: String,
    onSelectedExampleFileChange: (String) -> Unit,
    sdkCodeContent: String,
    onCopyClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Choose SDK Code Example:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ScrollableTabRow(
            selectedTabIndex = sdkExamples.indexOfFirst { it.first == selectedExampleFile },
            edgePadding = 0.dp
        ) {
            sdkExamples.forEach { (fileName, description) ->
                Tab(
                    selected = selectedExampleFile == fileName,
                    onClick = { onSelectedExampleFileChange(fileName) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = fileName, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected description
        val currentDesc = sdkExamples.firstOrNull { it.first == selectedExampleFile }?.second ?: ""
        Text(
            text = currentDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Code Inspector Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of inspector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedExampleFile,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onCopyClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy code",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Code contents
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = sdkCodeContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ZawinskisLawHelpTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Zawinski's Law of Software Envelopment",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\"Every program attempts to expand until it can read mail. Those programs which cannot so expand are replaced by ones which can.\"",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "6 Ways to Send Email in LibreOffice SDK",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        val ways = listOf(
            Triple("1. SimpleSystemMail (Windows)", "com.sun.star.system.SimpleSystemMail", "Accesses Windows MAPI default client. Launches confirmation modal and pops up GUI."),
            Triple("2. SimpleCommandMail (Linux/Mac/Android)", "com.sun.star.system.SimpleCommandMail", "Invokes client CLI commands. Hides major GUI elements, but prompts user confirmation."),
            Triple("3. MailServiceProvider", "com.sun.star.mail.MailServiceProvider", "Python-backed socket SMTP server connector. Supports SSL/TLS. Bypasses client UI entirely but firewalls can block it."),
            Triple("4. JavaMail (Joint Best Choice)", "javax.mail.* Session/Transport/Message", "Complete standalone SMTP, POP3, and IMAP wrapper. Bypasses LO engine, robust attachment parsing."),
            Triple("5. Desktop.mail() (Standard JRE)", "java.awt.Desktop.mail()", "Uses mailto system protocol handler. Unofficial attachments may fail on clients like Thunderbird."),
            Triple("6. MailMerge", "com.sun.star.text.MailMerge", "Combines templates and spreadsheets to bulk generate & send documents synchronously.")
        )

        ways.forEach { (num, name, desc) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = num, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(text = "Service: $name", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = desc, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Optimization Guide for Target Devices",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "• Samsung Galaxy A11 & Realme C3 (Low-end 3GB RAM):\n" +
                    "To minimize CPU/RAM overhead when compressing and encoding large office attachments (.odt, .ods, .odp), the Papirus Engine executes attachment conversions in background IO Coroutines (Dispatchers.IO).\n" +
                    "• Fallback Strategy:\n" +
                    "When direct socket connections are blocked by tight enterprise firewalls or slow connections, the engine dynamically triggers the Native Share Sheet or RCS direct dispatch as high-availability fallbacks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
