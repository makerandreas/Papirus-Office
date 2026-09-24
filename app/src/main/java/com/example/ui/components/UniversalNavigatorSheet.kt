package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.makerandreas.papirusoffice.data.navigation.*

/**
 * Standard Bottom Sheet - Navigator Content
 */
@Composable
fun NavigatorSheetContent(
    navEngine: NavigationEngine,
    isEditMode: Boolean,
    onOpenNavigateBy: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    canUndo: Boolean = true,
    canRedo: Boolean = true
) {
    val context = LocalContext.current
    val navState by navEngine.state.collectAsState()

    // Handle toast notifications from engine (e.g. "This object is hidden")
    LaunchedEffect(navState.notificationMessage) {
        val msg = navState.notificationMessage
        if (msg != null) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            navEngine.clearNotificationMessage()
        }
    }

    // Category expansion state - default all collapsed (+) for 'All' mode
    val expandedCategories = remember {
        mutableStateMapOf<String, Boolean>()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // --- TOP HEADER BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.navigator_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isEditMode) {
                    IconButton(onClick = onUndo, enabled = canUndo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = stringResource(R.string.options_done),
                            tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = stringResource(R.string.options_done),
                            tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.btn_open),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- NAVIGATE BY CONTROL ROW ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "Navigate by" selector button (2 icons wide)
            Surface(
                onClick = onOpenNavigateBy,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = getNavigateByIcon(navState.navigateBy),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = getNavigateByLabel(navState.navigateBy),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = stringResource(R.string.navigate_by_title),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Previous Button
            OutlinedIconButton(
                onClick = { navEngine.previous() },
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.nav_previous),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Next Button
            OutlinedIconButton(
                onClick = { navEngine.next() },
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = stringResource(R.string.nav_next),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // --- TREE / LIST OF NAVIGABLE ITEMS ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            val index = navState.index

            if (navState.navigateBy == NavigateBy.ALL) {
                // 1. Headings
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_headings),
                        icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                        count = countAllHeadings(index.headings),
                        isExpanded = expandedCategories["headings"] == true,
                        onToggleExpand = {
                            expandedCategories["headings"] = !(expandedCategories["headings"] ?: false)
                        }
                    )
                }
                if (expandedCategories["headings"] == true) {
                    if (index.headings.isEmpty()) {
                        item { NoneInDocumentRow(stringResource(R.string.navigate_by_headings)) }
                    } else {
                        items(index.headings) { heading ->
                            HeadingTreeItem(
                                node = heading,
                                activeId = navState.activeHeadingId ?: navState.activeItemId,
                                onHeadingClick = { id -> navEngine.goToHeading(id) },
                                onToggleFold = { id -> navEngine.toggleHeadingFolding(id) }
                            )
                        }
                    }
                }

                // 2. Tables
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_tables),
                        icon = Icons.Rounded.TableChart,
                        count = index.tables.size,
                        isExpanded = expandedCategories["tables"] == true,
                        onToggleExpand = {
                            expandedCategories["tables"] = !(expandedCategories["tables"] ?: false)
                        }
                    )
                }
                if (expandedCategories["tables"] == true) {
                    if (index.tables.isEmpty()) {
                        item { NoneInDocumentRow(stringResource(R.string.navigate_by_tables)) }
                    } else {
                        items(index.tables) { table ->
                            LeafItemRow(
                                name = table.tableName,
                                icon = Icons.Rounded.TableChart,
                                isSelected = navState.activeItemId == table.id,
                                isHidden = table.visibility == VisibilityState.HIDDEN,
                                onClick = {
                                    if (table.visibility == VisibilityState.HIDDEN) {
                                        Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                    }
                                    navEngine.goToTable(table.id)
                                }
                            )
                        }
                    }
                }

                // 3. Text Frames. The parser does not index frames yet, so a
                // header with a count would claim a number the app cannot see.
                // TODO(plan-19): frame identity parsing makes this readable.
                item {
                    UnavailableCategoryRow(
                        label = stringResource(R.string.navigate_by_frames),
                        icon = Icons.Rounded.CropFree
                    )
                }

                // 4. Images
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_images),
                        icon = Icons.Rounded.Image,
                        count = index.images.size,
                        isExpanded = expandedCategories["images"] == true,
                        onToggleExpand = {
                            expandedCategories["images"] = !(expandedCategories["images"] ?: false)
                        }
                    )
                }
                if (expandedCategories["images"] == true) {
                    if (index.images.isEmpty()) {
                        item { NoneInDocumentRow(stringResource(R.string.navigate_by_images)) }
                    } else {
                        items(index.images) { img ->
                            LeafItemRow(
                                name = img.imageName,
                                icon = Icons.Rounded.Image,
                                isSelected = navState.activeItemId == img.id,
                                isHidden = img.visibility == VisibilityState.HIDDEN,
                                onClick = {
                                    if (img.visibility == VisibilityState.HIDDEN) {
                                        Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                    }
                                    navEngine.goToImage(img.id)
                                }
                            )
                        }
                    }
                }

                // 4b. Hyperlinks (checklist item 7). Not parsed yet, so the
                // category exists in the honest not-yet-readable shape.
                // TODO(plan-18/20): hyperlink parsing fills this and adds the
                // Navigate-By filter option.
                item {
                    UnavailableCategoryRow(
                        label = stringResource(R.string.navigate_by_hyperlinks),
                        icon = Icons.Rounded.Link
                    )
                }

                // 5. OLE Objects. No OLE model exists yet.
                // TODO(unassigned): OLE parsing makes this readable.
                item {
                    UnavailableCategoryRow(
                        label = stringResource(R.string.navigate_by_ole),
                        icon = Icons.Rounded.Extension
                    )
                }

                // 6. Bookmarks. Not parsed yet (tokens exist, unconsumed).
                // TODO(plan-18/20): bookmark parsing fills this.
                item {
                    UnavailableCategoryRow(
                        label = stringResource(R.string.navigate_by_bookmarks),
                        icon = Icons.Rounded.Bookmark
                    )
                }

                // 7. Comments. No comment model exists yet.
                // TODO(plan-8): comment model makes this readable.
                item {
                    UnavailableCategoryRow(
                        label = stringResource(R.string.navigate_by_comments),
                        icon = Icons.AutoMirrored.Rounded.Comment
                    )
                }

                // 8. Sections. ODT section identity / DOCX sectPr not parsed yet.
                // TODO(plan-19/21): section parsing fills this.
                item {
                    UnavailableCategoryRow(
                        label = stringResource(R.string.navigate_by_sections),
                        icon = Icons.Rounded.ViewAgenda
                    )
                }

                // 9. Fields. Field instructions are not parsed yet.
                // TODO(plan-21): field model fills this.
                item {
                    UnavailableCategoryRow(
                        label = stringResource(R.string.navigate_by_fields),
                        icon = Icons.Rounded.TextFields
                    )
                }

                // 10. Footnotes. No footnote model exists yet.
                // TODO(unassigned): footnote parsing makes this readable.
                item {
                    UnavailableCategoryRow(
                        label = stringResource(R.string.navigate_by_footnotes),
                        icon = Icons.AutoMirrored.Rounded.Notes
                    )
                }

                // 11. Shapes / Drawing Objects. No shape model exists yet.
                // TODO(unassigned): shape parsing makes this readable.
                item {
                    UnavailableCategoryRow(
                        label = stringResource(R.string.navigate_by_drawing),
                        icon = Icons.Rounded.Category
                    )
                }

                // 12. Pages
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_page),
                        icon = Icons.Rounded.Description,
                        count = navState.totalPages,
                        isExpanded = expandedCategories["pages"] == true,
                        onToggleExpand = {
                            expandedCategories["pages"] = !(expandedCategories["pages"] ?: false)
                        }
                    )
                }
                if (expandedCategories["pages"] == true) {
                    if (navState.totalPages <= 0) {
                        item { NoneInDocumentRow(stringResource(R.string.navigate_by_page)) }
                    } else {
                        items((1..navState.totalPages).toList()) { p ->
                            LeafItemRow(
                                name = stringResource(R.string.navigator_page_item, p),
                                icon = Icons.Rounded.Description,
                                isSelected = navState.currentPage == p,
                                onClick = { navEngine.goToPage(p) }
                            )
                        }
                    }
                }
            } else {
                // FILTERED MODE: Group headers hidden, only display selected category objects
                when (navState.navigateBy) {
                    NavigateBy.HEADING -> {
                        if (index.headings.isEmpty()) {
                            item { NoneInDocumentRow(stringResource(R.string.navigate_by_headings)) }
                        } else {
                            items(index.headings) { heading ->
                                HeadingTreeItem(
                                    node = heading,
                                    activeId = navState.activeHeadingId ?: navState.activeItemId,
                                    onHeadingClick = { id -> navEngine.goToHeading(id) },
                                    onToggleFold = { id -> navEngine.toggleHeadingFolding(id) }
                                )
                            }
                        }
                    }
                    NavigateBy.TABLE -> {
                        if (index.tables.isEmpty()) {
                            item { NoneInDocumentRow(stringResource(R.string.navigate_by_tables)) }
                        } else {
                            items(index.tables) { table ->
                                LeafItemRow(
                                    name = table.tableName,
                                    icon = Icons.Rounded.TableChart,
                                    isSelected = navState.activeItemId == table.id,
                                    isHidden = table.visibility == VisibilityState.HIDDEN,
                                    startPadding = 16.dp,
                                    onClick = {
                                        if (table.visibility == VisibilityState.HIDDEN) {
                                            Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                        }
                                        navEngine.goToTable(table.id)
                                    }
                                )
                            }
                        }
                    }
                    // TODO(plan-19): frame identity parsing makes this readable.
                    NavigateBy.FRAME -> {
                        item {
                            UnavailableCategoryRow(
                                label = stringResource(R.string.navigate_by_frames),
                                icon = Icons.Rounded.CropFree
                            )
                        }
                    }
                    NavigateBy.IMAGE -> {
                        if (index.images.isEmpty()) {
                            item { NoneInDocumentRow(stringResource(R.string.navigate_by_images)) }
                        } else {
                            items(index.images) { img ->
                                LeafItemRow(
                                    name = img.imageName,
                                    icon = Icons.Rounded.Image,
                                    isSelected = navState.activeItemId == img.id,
                                    isHidden = img.visibility == VisibilityState.HIDDEN,
                                    startPadding = 16.dp,
                                    onClick = {
                                        if (img.visibility == VisibilityState.HIDDEN) {
                                            Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                        }
                                        navEngine.goToImage(img.id)
                                    }
                                )
                            }
                        }
                    }
                    // TODO(unassigned): OLE parsing makes this readable.
                    NavigateBy.OLE -> {
                        item {
                            UnavailableCategoryRow(
                                label = stringResource(R.string.navigate_by_ole),
                                icon = Icons.Rounded.Extension
                            )
                        }
                    }
                    // TODO(plan-18/20): bookmark parsing makes this readable.
                    NavigateBy.BOOKMARK -> {
                        item {
                            UnavailableCategoryRow(
                                label = stringResource(R.string.navigate_by_bookmarks),
                                icon = Icons.Rounded.Bookmark
                            )
                        }
                    }
                    // TODO(plan-8): comment model makes this readable.
                    NavigateBy.COMMENT -> {
                        item {
                            UnavailableCategoryRow(
                                label = stringResource(R.string.navigate_by_comments),
                                icon = Icons.AutoMirrored.Rounded.Comment
                            )
                        }
                    }
                    // TODO(plan-19/21): section parsing makes this readable.
                    NavigateBy.SECTION -> {
                        item {
                            UnavailableCategoryRow(
                                label = stringResource(R.string.navigate_by_sections),
                                icon = Icons.Rounded.ViewAgenda
                            )
                        }
                    }
                    // TODO(plan-21): field model makes this readable.
                    NavigateBy.FIELD -> {
                        item {
                            UnavailableCategoryRow(
                                label = stringResource(R.string.navigate_by_fields),
                                icon = Icons.Rounded.TextFields
                            )
                        }
                    }
                    // TODO(unassigned): footnote parsing makes this readable.
                    NavigateBy.FOOTNOTE -> {
                        item {
                            UnavailableCategoryRow(
                                label = stringResource(R.string.navigate_by_footnotes),
                                icon = Icons.AutoMirrored.Rounded.Notes
                            )
                        }
                    }
                    // TODO(unassigned): shape parsing makes this readable.
                    NavigateBy.DRAWING, NavigateBy.SHAPE -> {
                        item {
                            UnavailableCategoryRow(
                                label = stringResource(R.string.navigate_by_drawing),
                                icon = Icons.Rounded.Category
                            )
                        }
                    }
                    NavigateBy.PAGE -> {
                        if (navState.totalPages <= 0) {
                            item { NoneInDocumentRow(stringResource(R.string.navigate_by_page)) }
                        } else {
                            items((1..navState.totalPages).toList()) { p ->
                                LeafItemRow(
                                    name = stringResource(R.string.navigator_page_item, p),
                                    icon = Icons.Rounded.Description,
                                    isSelected = navState.currentPage == p,
                                    startPadding = 16.dp,
                                    onClick = { navEngine.goToPage(p) }
                                )
                            }
                        }
                    }
                    NavigateBy.REMINDER -> {
                        if (index.reminders.isEmpty()) {
                            item { EmptyCategoryRow() }
                        } else {
                            items(index.reminders) { rem ->
                                LeafItemRow(
                                    name = rem.note,
                                    icon = Icons.Rounded.Alarm,
                                    isSelected = navState.activeItemId == rem.id,
                                    startPadding = 16.dp,
                                    onClick = { navEngine.goToReminder(rem.id) }
                                )
                            }
                        }
                    }
                    // TODO(plan-19): TOC/index snapshot makes this readable.
                    NavigateBy.INDEX -> {
                        item {
                            UnavailableCategoryRow(
                                label = stringResource(R.string.navigate_by_indexes),
                                icon = Icons.AutoMirrored.Rounded.Toc
                            )
                        }
                    }
                    // Session-only targets (Selection, Recency): no document
                    // content, so the generic message is accurate here.
                    else -> {
                        item { EmptyCategoryRow() }
                    }
                }
            }
        }
    }
}

/**
 * Standard Bottom Sheet - Navigate By... Content
 */
@Composable
fun NavigateBySheetContent(
    navEngine: NavigationEngine,
    isEditMode: Boolean,
    onBackToNavigator: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    canUndo: Boolean = true,
    canRedo: Boolean = true
) {
    val navState by navEngine.state.collectAsState()

    val options = remember {
        listOf(
            NavigateBy.ALL to (R.string.navigate_by_all to Icons.Rounded.AllInclusive),
            NavigateBy.HEADING to (R.string.navigate_by_headings to Icons.AutoMirrored.Rounded.FormatListBulleted),
            NavigateBy.TABLE to (R.string.navigate_by_tables to Icons.Rounded.TableChart),
            NavigateBy.IMAGE to (R.string.navigate_by_images to Icons.Rounded.Image),
            NavigateBy.BOOKMARK to (R.string.navigate_by_bookmarks to Icons.Rounded.Bookmark),
            NavigateBy.COMMENT to (R.string.navigate_by_comments to Icons.AutoMirrored.Rounded.Comment),
            NavigateBy.SECTION to (R.string.navigate_by_sections to Icons.Rounded.ViewAgenda),
            NavigateBy.FRAME to (R.string.navigate_by_frames to Icons.Rounded.CropFree),
            NavigateBy.FIELD to (R.string.navigate_by_fields to Icons.Rounded.TextFields),
            NavigateBy.FOOTNOTE to (R.string.navigate_by_footnotes to Icons.AutoMirrored.Rounded.Notes),
            NavigateBy.OLE to (R.string.navigate_by_ole to Icons.Rounded.Extension),
            NavigateBy.DRAWING to (R.string.navigate_by_drawing to Icons.Rounded.Category),
            NavigateBy.PAGE to (R.string.navigate_by_page to Icons.Rounded.Description),
            NavigateBy.REMINDER to (R.string.navigate_by_reminder to Icons.Rounded.Alarm),
            NavigateBy.INDEX to (R.string.navigate_by_indexes to Icons.AutoMirrored.Rounded.Toc),
            NavigateBy.SELECTION to (R.string.navigate_by_selection to Icons.Rounded.SelectAll),
            NavigateBy.RECENT to (R.string.navigate_by_recency to Icons.Rounded.History)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // --- HEADER BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onBackToNavigator) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.options_done),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(R.string.navigate_by_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isEditMode) {
                    IconButton(onClick = onUndo, enabled = canUndo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = stringResource(R.string.options_done),
                            tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = stringResource(R.string.options_done),
                            tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.btn_open),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- OBJECT TYPE ENTRY LIST ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            items(options) { (type, pair) ->
                val (stringRes, icon) = pair
                val isSelected = navState.navigateBy == type

                Surface(
                    onClick = {
                        navEngine.setNavigateBy(type)
                        onBackToNavigator()
                    },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(stringRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TREE COMPONENT HELPER FUNCTIONS & COMPOSABLES
// ==========================================

@Composable
private fun CategoryHeaderRow(
    title: String,
    icon: ImageVector,
    count: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Surface(
        onClick = onToggleExpand,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.Remove else Icons.Rounded.Add,
                contentDescription = stringResource(if (isExpanded) R.string.cd_collapse else R.string.cd_expand),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = "$title ($count)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun countAllHeadings(list: List<HeadingNode>): Int {
    var count = 0
    fun recurse(node: HeadingNode) {
        count++
        node.children.forEach { recurse(it) }
    }
    list.forEach { recurse(it) }
    return count
}

@Composable
private fun HeadingTreeItem(
    node: HeadingNode,
    activeId: String?,
    onHeadingClick: (String) -> Unit,
    onToggleFold: (String) -> Unit,
    indentDepth: Int = 0
) {
    val isSelected = node.id == activeId

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            onClick = { onHeadingClick(node.id) },
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = (16 + indentDepth * 16).dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (node.children.isNotEmpty()) {
                    IconButton(
                        onClick = { onToggleFold(node.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (node.collapsed) Icons.Rounded.Add else Icons.Rounded.Remove,
                            contentDescription = stringResource(if (node.collapsed) R.string.cd_expand else R.string.cd_collapse),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = node.title.trim().removePrefix("\u200B").trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (!node.collapsed && node.children.isNotEmpty()) {
            node.children.forEach { child ->
                HeadingTreeItem(
                    node = child,
                    activeId = activeId,
                    onHeadingClick = onHeadingClick,
                    onToggleFold = onToggleFold,
                    indentDepth = indentDepth + 1
                )
            }
        }
    }
}

// Empty-state rows (plan 3B, R-26). Two different facts, two different
// messages: "no X in this document" is only true for classes the parser
// fully reads; for classes the parser cannot read yet the honest state is
// "not yet available", never a fabricated zero.
@Composable
private fun NoneInDocumentRow(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.navigator_none_in_document, label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UnavailableCategoryRow(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = stringResource(R.string.navigator_not_yet_available),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// Neutral fallback for session-only Navigate By targets (Selection, Recency)
// that are not document content at all.
@Composable
private fun EmptyCategoryRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_objects_to_navigate),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LeafItemRow(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    isHidden: Boolean = false,
    startPadding: Dp = 40.dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isHidden) Color.Gray else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isHidden) Color.Gray else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isHidden) {
                Text(
                    text = "Hidden",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun getNavigateByIcon(type: NavigateBy): ImageVector {
    return when (type) {
        NavigateBy.ALL -> Icons.Rounded.AllInclusive
        NavigateBy.HEADING -> Icons.AutoMirrored.Rounded.FormatListBulleted
        NavigateBy.TABLE -> Icons.Rounded.TableChart
        NavigateBy.IMAGE -> Icons.Rounded.Image
        NavigateBy.BOOKMARK -> Icons.Rounded.Bookmark
        NavigateBy.COMMENT -> Icons.AutoMirrored.Rounded.Comment
        NavigateBy.SECTION -> Icons.Rounded.ViewAgenda
        NavigateBy.FRAME -> Icons.Rounded.CropFree
        NavigateBy.FIELD -> Icons.Rounded.TextFields
        NavigateBy.FOOTNOTE -> Icons.AutoMirrored.Rounded.Notes
        NavigateBy.OLE -> Icons.Rounded.Extension
        NavigateBy.DRAWING, NavigateBy.SHAPE -> Icons.Rounded.Category
        NavigateBy.PAGE -> Icons.Rounded.Description
        NavigateBy.REMINDER -> Icons.Rounded.Alarm
        NavigateBy.INDEX -> Icons.AutoMirrored.Rounded.Toc
        NavigateBy.SELECTION -> Icons.Rounded.SelectAll
        NavigateBy.RECENT -> Icons.Rounded.History
    }
}

@Composable
private fun getNavigateByLabel(type: NavigateBy): String {
    return when (type) {
        NavigateBy.ALL -> stringResource(R.string.navigate_by_all)
        NavigateBy.HEADING -> stringResource(R.string.navigate_by_headings)
        NavigateBy.TABLE -> stringResource(R.string.navigate_by_tables)
        NavigateBy.IMAGE -> stringResource(R.string.navigate_by_images)
        NavigateBy.BOOKMARK -> stringResource(R.string.navigate_by_bookmarks)
        NavigateBy.COMMENT -> stringResource(R.string.navigate_by_comments)
        NavigateBy.SECTION -> stringResource(R.string.navigate_by_sections)
        NavigateBy.FRAME -> stringResource(R.string.navigate_by_frames)
        NavigateBy.FIELD -> stringResource(R.string.navigate_by_fields)
        NavigateBy.FOOTNOTE -> stringResource(R.string.navigate_by_footnotes)
        NavigateBy.OLE -> stringResource(R.string.navigate_by_ole)
        NavigateBy.DRAWING, NavigateBy.SHAPE -> stringResource(R.string.navigate_by_drawing)
        NavigateBy.PAGE -> stringResource(R.string.navigate_by_page)
        NavigateBy.REMINDER -> stringResource(R.string.navigate_by_reminder)
        NavigateBy.INDEX -> stringResource(R.string.navigate_by_indexes)
        NavigateBy.SELECTION -> stringResource(R.string.navigate_by_selection)
        NavigateBy.RECENT -> stringResource(R.string.navigate_by_recency)
    }
}
