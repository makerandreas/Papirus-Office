package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Notes
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
                            imageVector = Icons.Rounded.Undo,
                            contentDescription = stringResource(R.string.options_done),
                            tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(
                            imageVector = Icons.Rounded.Redo,
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
                        count = index.headings.size,
                        isExpanded = expandedCategories["headings"] == true,
                        onToggleExpand = {
                            expandedCategories["headings"] = !(expandedCategories["headings"] ?: false)
                        }
                    )
                }
                if (expandedCategories["headings"] == true) {
                    if (index.headings.isEmpty()) {
                        item { EmptyCategoryRow() }
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
                        item { EmptyCategoryRow() }
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

                // 3. Text Frames
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_frames),
                        icon = Icons.Rounded.CropFree,
                        count = index.frames.size,
                        isExpanded = expandedCategories["frames"] == true,
                        onToggleExpand = {
                            expandedCategories["frames"] = !(expandedCategories["frames"] ?: false)
                        }
                    )
                }
                if (expandedCategories["frames"] == true) {
                    if (index.frames.isEmpty()) {
                        item { EmptyCategoryRow() }
                    } else {
                        items(index.frames) { frame ->
                            LeafItemRow(
                                name = frame.frameName,
                                icon = Icons.Rounded.CropFree,
                                isSelected = navState.activeItemId == frame.id,
                                isHidden = frame.visibility == VisibilityState.HIDDEN,
                                onClick = {
                                    if (frame.visibility == VisibilityState.HIDDEN) {
                                        Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                    }
                                    navEngine.goToFrame(frame.id)
                                }
                            )
                        }
                    }
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
                        item { EmptyCategoryRow() }
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

                // 5. OLE Objects
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_ole),
                        icon = Icons.Rounded.Extension,
                        count = index.oleObjects.size,
                        isExpanded = expandedCategories["ole"] == true,
                        onToggleExpand = {
                            expandedCategories["ole"] = !(expandedCategories["ole"] ?: false)
                        }
                    )
                }
                if (expandedCategories["ole"] == true) {
                    if (index.oleObjects.isEmpty()) {
                        item { EmptyCategoryRow() }
                    } else {
                        items(index.oleObjects) { ole ->
                            LeafItemRow(
                                name = ole.oleName,
                                icon = Icons.Rounded.Extension,
                                isSelected = navState.activeItemId == ole.id,
                                isHidden = ole.visibility == VisibilityState.HIDDEN,
                                onClick = {
                                    if (ole.visibility == VisibilityState.HIDDEN) {
                                        Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                    }
                                    navEngine.goToOle(ole.id)
                                }
                            )
                        }
                    }
                }

                // 6. Bookmarks
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_bookmarks),
                        icon = Icons.Rounded.Bookmark,
                        count = index.bookmarks.size,
                        isExpanded = expandedCategories["bookmarks"] == true,
                        onToggleExpand = {
                            expandedCategories["bookmarks"] = !(expandedCategories["bookmarks"] ?: false)
                        }
                    )
                }
                if (expandedCategories["bookmarks"] == true) {
                    if (index.bookmarks.isEmpty()) {
                        item { EmptyCategoryRow() }
                    } else {
                        items(index.bookmarks) { bm ->
                            LeafItemRow(
                                name = bm.name,
                                icon = Icons.Rounded.Bookmark,
                                isSelected = navState.activeItemId == bm.id,
                                onClick = { navEngine.goToBookmark(bm.id) }
                            )
                        }
                    }
                }

                // 7. Comments
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_comments),
                        icon = Icons.Rounded.Comment,
                        count = index.comments.size,
                        isExpanded = expandedCategories["comments"] == true,
                        onToggleExpand = {
                            expandedCategories["comments"] = !(expandedCategories["comments"] ?: false)
                        }
                    )
                }
                if (expandedCategories["comments"] == true) {
                    if (index.comments.isEmpty()) {
                        item { EmptyCategoryRow() }
                    } else {
                        items(index.comments) { c ->
                            LeafItemRow(
                                name = "${c.author}: ${c.content}",
                                icon = Icons.Rounded.Comment,
                                isSelected = navState.activeItemId == c.id,
                                onClick = { navEngine.goToComment(c.id) }
                            )
                        }
                    }
                }

                // 8. Sections
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_sections),
                        icon = Icons.Rounded.ViewAgenda,
                        count = index.sections.size,
                        isExpanded = expandedCategories["sections"] == true,
                        onToggleExpand = {
                            expandedCategories["sections"] = !(expandedCategories["sections"] ?: false)
                        }
                    )
                }
                if (expandedCategories["sections"] == true) {
                    if (index.sections.isEmpty()) {
                        item { EmptyCategoryRow() }
                    } else {
                        items(index.sections) { sec ->
                            LeafItemRow(
                                name = sec.sectionName,
                                icon = Icons.Rounded.ViewAgenda,
                                isSelected = navState.activeItemId == sec.id,
                                isHidden = sec.visibility == VisibilityState.HIDDEN,
                                onClick = {
                                    if (sec.visibility == VisibilityState.HIDDEN) {
                                        Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                    }
                                    navEngine.goToSection(sec.id)
                                }
                            )
                        }
                    }
                }

                // 9. Fields
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_fields),
                        icon = Icons.Rounded.TextFields,
                        count = index.fields.size,
                        isExpanded = expandedCategories["fields"] == true,
                        onToggleExpand = {
                            expandedCategories["fields"] = !(expandedCategories["fields"] ?: false)
                        }
                    )
                }
                if (expandedCategories["fields"] == true) {
                    if (index.fields.isEmpty()) {
                        item { EmptyCategoryRow() }
                    } else {
                        items(index.fields) { f ->
                            LeafItemRow(
                                name = "${f.fieldType}: ${f.value}",
                                icon = Icons.Rounded.TextFields,
                                isSelected = navState.activeItemId == f.id,
                                onClick = { navEngine.goToField(f.id) }
                            )
                        }
                    }
                }

                // 10. Footnotes
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_footnotes),
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        count = index.footnotes.size,
                        isExpanded = expandedCategories["footnotes"] == true,
                        onToggleExpand = {
                            expandedCategories["footnotes"] = !(expandedCategories["footnotes"] ?: false)
                        }
                    )
                }
                if (expandedCategories["footnotes"] == true) {
                    if (index.footnotes.isEmpty()) {
                        item { EmptyCategoryRow() }
                    } else {
                        items(index.footnotes) { fn ->
                            LeafItemRow(
                                name = "Footnote ${fn.label}",
                                icon = Icons.AutoMirrored.Rounded.Notes,
                                isSelected = navState.activeItemId == fn.id,
                                onClick = { navEngine.goToFootnote(fn.id) }
                            )
                        }
                    }
                }

                // 11. Shapes / Drawing Objects
                item {
                    CategoryHeaderRow(
                        title = stringResource(R.string.navigate_by_drawing),
                        icon = Icons.Rounded.Category,
                        count = index.shapes.size,
                        isExpanded = expandedCategories["shapes"] == true,
                        onToggleExpand = {
                            expandedCategories["shapes"] = !(expandedCategories["shapes"] ?: false)
                        }
                    )
                }
                if (expandedCategories["shapes"] == true) {
                    if (index.shapes.isEmpty()) {
                        item { EmptyCategoryRow() }
                    } else {
                        items(index.shapes) { sh ->
                            LeafItemRow(
                                name = sh.shapeName,
                                icon = Icons.Rounded.Category,
                                isSelected = navState.activeItemId == sh.id,
                                isHidden = sh.visibility == VisibilityState.HIDDEN,
                                onClick = {
                                    if (sh.visibility == VisibilityState.HIDDEN) {
                                        Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                    }
                                    navEngine.goToShape(sh.id)
                                }
                            )
                        }
                    }
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
                        item { EmptyCategoryRow() }
                    } else {
                        items((1..navState.totalPages).toList()) { p ->
                            LeafItemRow(
                                name = "Page $p",
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
                            item { EmptyCategoryRow() }
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
                            item { EmptyCategoryRow() }
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
                    NavigateBy.FRAME -> {
                        if (index.frames.isEmpty()) {
                            item { EmptyCategoryRow() }
                        } else {
                            items(index.frames) { frame ->
                                LeafItemRow(
                                    name = frame.frameName,
                                    icon = Icons.Rounded.CropFree,
                                    isSelected = navState.activeItemId == frame.id,
                                    isHidden = frame.visibility == VisibilityState.HIDDEN,
                                    startPadding = 16.dp,
                                    onClick = {
                                        if (frame.visibility == VisibilityState.HIDDEN) {
                                            Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                        }
                                        navEngine.goToFrame(frame.id)
                                    }
                                )
                            }
                        }
                    }
                    NavigateBy.IMAGE -> {
                        if (index.images.isEmpty()) {
                            item { EmptyCategoryRow() }
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
                    NavigateBy.OLE -> {
                        if (index.oleObjects.isEmpty()) {
                            item { EmptyCategoryRow() }
                        } else {
                            items(index.oleObjects) { ole ->
                                LeafItemRow(
                                    name = ole.oleName,
                                    icon = Icons.Rounded.Extension,
                                    isSelected = navState.activeItemId == ole.id,
                                    isHidden = ole.visibility == VisibilityState.HIDDEN,
                                    startPadding = 16.dp,
                                    onClick = {
                                        if (ole.visibility == VisibilityState.HIDDEN) {
                                            Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                        }
                                        navEngine.goToOle(ole.id)
                                    }
                                )
                            }
                        }
                    }
                    NavigateBy.BOOKMARK -> {
                        if (index.bookmarks.isEmpty()) {
                            item { EmptyCategoryRow() }
                        } else {
                            items(index.bookmarks) { bm ->
                                LeafItemRow(
                                    name = bm.name,
                                    icon = Icons.Rounded.Bookmark,
                                    isSelected = navState.activeItemId == bm.id,
                                    startPadding = 16.dp,
                                    onClick = { navEngine.goToBookmark(bm.id) }
                                )
                            }
                        }
                    }
                    NavigateBy.COMMENT -> {
                        if (index.comments.isEmpty()) {
                            item { EmptyCategoryRow() }
                        } else {
                            items(index.comments) { c ->
                                LeafItemRow(
                                    name = "${c.author}: ${c.content}",
                                    icon = Icons.Rounded.Comment,
                                    isSelected = navState.activeItemId == c.id,
                                    startPadding = 16.dp,
                                    onClick = { navEngine.goToComment(c.id) }
                                )
                            }
                        }
                    }
                    NavigateBy.SECTION -> {
                        if (index.sections.isEmpty()) {
                            item { EmptyCategoryRow() }
                        } else {
                            items(index.sections) { sec ->
                                LeafItemRow(
                                    name = sec.sectionName,
                                    icon = Icons.Rounded.ViewAgenda,
                                    isSelected = navState.activeItemId == sec.id,
                                    isHidden = sec.visibility == VisibilityState.HIDDEN,
                                    startPadding = 16.dp,
                                    onClick = {
                                        if (sec.visibility == VisibilityState.HIDDEN) {
                                            Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                        }
                                        navEngine.goToSection(sec.id)
                                    }
                                )
                            }
                        }
                    }
                    NavigateBy.FIELD -> {
                        if (index.fields.isEmpty()) {
                            item { EmptyCategoryRow() }
                        } else {
                            items(index.fields) { f ->
                                LeafItemRow(
                                    name = "${f.fieldType}: ${f.value}",
                                    icon = Icons.Rounded.TextFields,
                                    isSelected = navState.activeItemId == f.id,
                                    startPadding = 16.dp,
                                    onClick = { navEngine.goToField(f.id) }
                                )
                            }
                        }
                    }
                    NavigateBy.FOOTNOTE -> {
                        if (index.footnotes.isEmpty()) {
                            item { EmptyCategoryRow() }
                        } else {
                            items(index.footnotes) { fn ->
                                LeafItemRow(
                                    name = "Footnote ${fn.label}",
                                    icon = Icons.AutoMirrored.Rounded.Notes,
                                    isSelected = navState.activeItemId == fn.id,
                                    startPadding = 16.dp,
                                    onClick = { navEngine.goToFootnote(fn.id) }
                                )
                            }
                        }
                    }
                    NavigateBy.DRAWING, NavigateBy.SHAPE -> {
                        if (index.shapes.isEmpty()) {
                            item { EmptyCategoryRow() }
                        } else {
                            items(index.shapes) { sh ->
                                LeafItemRow(
                                    name = sh.shapeName,
                                    icon = Icons.Rounded.Category,
                                    isSelected = navState.activeItemId == sh.id,
                                    isHidden = sh.visibility == VisibilityState.HIDDEN,
                                    startPadding = 16.dp,
                                    onClick = {
                                        if (sh.visibility == VisibilityState.HIDDEN) {
                                            Toast.makeText(context, context.getString(R.string.object_is_hidden), Toast.LENGTH_SHORT).show()
                                        }
                                        navEngine.goToShape(sh.id)
                                    }
                                )
                            }
                        }
                    }
                    NavigateBy.PAGE -> {
                        if (navState.totalPages <= 0) {
                            item { EmptyCategoryRow() }
                        } else {
                            items((1..navState.totalPages).toList()) { p ->
                                LeafItemRow(
                                    name = "Page $p",
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
            NavigateBy.COMMENT to (R.string.navigate_by_comments to Icons.Rounded.Comment),
            NavigateBy.SECTION to (R.string.navigate_by_sections to Icons.Rounded.ViewAgenda),
            NavigateBy.FRAME to (R.string.navigate_by_frames to Icons.Rounded.CropFree),
            NavigateBy.FIELD to (R.string.navigate_by_fields to Icons.Rounded.TextFields),
            NavigateBy.FOOTNOTE to (R.string.navigate_by_footnotes to Icons.AutoMirrored.Rounded.Notes),
            NavigateBy.OLE to (R.string.navigate_by_ole to Icons.Rounded.Extension),
            NavigateBy.DRAWING to (R.string.navigate_by_drawing to Icons.Rounded.Category),
            NavigateBy.PAGE to (R.string.navigate_by_page to Icons.Rounded.Description),
            NavigateBy.REMINDER to (R.string.navigate_by_reminder to Icons.Rounded.Alarm),
            NavigateBy.INDEX to (R.string.navigate_by_indexes to Icons.Rounded.Toc),
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
                            imageVector = Icons.Rounded.Undo,
                            contentDescription = stringResource(R.string.options_done),
                            tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    IconButton(onClick = onRedo, enabled = canRedo) {
                        Icon(
                            imageVector = Icons.Rounded.Redo,
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
                contentDescription = if (isExpanded) "Collapse" else "Expand",
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
                            contentDescription = if (node.collapsed) "Expand" else "Collapse",
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
                    text = node.title,
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
        NavigateBy.COMMENT -> Icons.Rounded.Comment
        NavigateBy.SECTION -> Icons.Rounded.ViewAgenda
        NavigateBy.FRAME -> Icons.Rounded.CropFree
        NavigateBy.FIELD -> Icons.Rounded.TextFields
        NavigateBy.FOOTNOTE -> Icons.AutoMirrored.Rounded.Notes
        NavigateBy.OLE -> Icons.Rounded.Extension
        NavigateBy.DRAWING, NavigateBy.SHAPE -> Icons.Rounded.Category
        NavigateBy.PAGE -> Icons.Rounded.Description
        NavigateBy.REMINDER -> Icons.Rounded.Alarm
        NavigateBy.INDEX -> Icons.Rounded.Toc
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
