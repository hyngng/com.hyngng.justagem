package com.hyngng.gemtalk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyngng.gemtalk.data.ChatSession
import com.hyngng.gemtalk.ui.components.GemOrb
import com.hyngng.gemtalk.ui.components.GemTalkDesign
import com.hyngng.gemtalk.ui.components.GemTalkIconControl
import com.hyngng.gemtalk.ui.components.GemTextInputDialog
import com.hyngng.gemtalk.ui.components.AlphaMaskEdge
import com.hyngng.gemtalk.ui.components.verticalAlphaMask
import com.hyngng.gemtalk.ui.components.IconControlTone
import com.hyngng.gemtalk.ui.components.LocalInteractionHaptics
import com.hyngng.gemtalk.ui.components.OrbState
import com.hyngng.gemtalk.ui.i18n.LocalStrings
import com.hyngng.gemtalk.ui.theme.Graphite
import com.hyngng.gemtalk.ui.theme.GraphiteRaised
import com.hyngng.gemtalk.ui.theme.GraphiteSoft
import com.hyngng.gemtalk.ui.theme.Hairline
import com.hyngng.gemtalk.ui.theme.Ice
import com.hyngng.gemtalk.ui.theme.Jade
import com.hyngng.gemtalk.ui.theme.Mist
import com.hyngng.gemtalk.ui.theme.MistFaint
import com.hyngng.gemtalk.ui.theme.MistMuted

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SessionDrawer(
    visible: Boolean,
    sessions: List<ChatSession>,
    activeId: String,
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (ChatSession) -> Unit,
    onRename: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    val s = LocalStrings.current
    var actionTarget by remember { mutableStateOf<ChatSession?>(null) }
    var renameTarget by remember { mutableStateOf<ChatSession?>(null) }
    var renamedTitle by remember { mutableStateOf(TextFieldValue("")) }
    // Drawer search covers titles and non-tool message bodies; matching is
    // case-insensitive and exact-substring (no tokenization quirks).
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val trimmedQuery = searchQuery.text.trim()
    val visibleSessions =
        if (trimmedQuery.isEmpty()) {
            sessions
        } else {
            val needle = trimmedQuery.lowercase()
            sessions.filter { session ->
                session.title.lowercase().contains(needle) ||
                    session.messages.any { message ->
                        !message.isError && message.text.lowercase().contains(needle)
                    }
            }
        }
    LaunchedEffect(sessions, actionTarget?.id, renameTarget?.id) {
        if (actionTarget?.let { target -> sessions.none { it.id == target.id } } == true) actionTarget = null
        if (renameTarget?.let { target -> sessions.none { it.id == target.id } } == true) renameTarget = null
    }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                            onClose()
                        },
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                // Same out-heavy cubic easing as the settings page push.
                tween(190, easing = GemTalkDesign.slideEase),
            ) { -it } + fadeIn(tween(90)),
            exit = slideOutHorizontally(tween(200)) { -it } + fadeOut(tween(160)),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            BoxWithConstraints {
                val panelWidth = minOf(maxWidth * 0.88f, 340.dp)
                Surface(
                    modifier = Modifier
                        .width(panelWidth)
                        .fillMaxHeight(),
                    color = Graphite,
                    shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                ) {
                    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    val contentPadding = GemTalkDesign.drawerHorizontalPadding
                    Column(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = contentPadding),
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Justagem",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                color = Mist,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                                onClose()
                            }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = s.t("닫기", "Close"),
                                    tint = MistMuted,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(GemTalkDesign.drawerHeaderToActionSpacing))

                        // Category Label — while searching it doubles as the
                        // match count so the scope of results stays legible.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (trimmedQuery.isEmpty()) {
                                    s.t("대화 내역", "Chats")
                                } else {
                                    s.t("일치하는 대화 ${visibleSessions.size}개", "${visibleSessions.size} matching chats")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Mist,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(Modifier.height(GemTalkDesign.drawerLabelToListSpacing + 10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .background(GraphiteSoft, CircleShape)
                                .padding(start = 14.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = s.t("대화 검색", "Search chats"),
                                tint = MistMuted,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Mist),
                                cursorBrush = SolidColor(Jade),
                                decorationBox = { inner ->
                                    Box(Modifier.fillMaxWidth()) {
                                        if (searchQuery.text.isEmpty()) {
                                            Text(
                                                text = s.t("대화 검색", "Search chats"),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MistFaint,
                                            )
                                        }
                                        inner()
                                    }
                                },
                            )
                            if (searchQuery.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                                        searchQuery = TextFieldValue("")
                                    },
                                    modifier = Modifier.size(30.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = s.t("검색어 지우기", "Clear search"),
                                        tint = MistMuted,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(GemTalkDesign.compactSpacing))

                        val listState = rememberLazyListState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalAlphaMask(
                                    top = if (listState.canScrollBackward) {
                                        AlphaMaskEdge(extent = 14.dp, maxFraction = 0.12f)
                                    } else {
                                        null
                                    },
                                    bottom = if (listState.canScrollForward) {
                                        AlphaMaskEdge(extent = 14.dp, maxFraction = 0.12f)
                                    } else {
                                        null
                                    },
                                ),
                        ) {
                            if (visibleSessions.isEmpty() && trimmedQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = s.t("일치하는 대화가 없어요", "No matching chats"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MistMuted,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    userScrollEnabled = renameTarget == null && actionTarget == null,
                                    contentPadding = PaddingValues(top = GemTalkDesign.compactSpacing, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(visibleSessions, key = { it.id }) { session ->
                                        SessionRow(
                                            session = session,
                                            active = session.id == activeId,
                                            onClick = { onSelect(session.id) },
                                            onLongPress = { actionTarget = session },
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(GemTalkDesign.drawerListToSettingsSpacing))

                        // Circular settings button, right-aligned to match
                        // GemTalkIconControl design language (same as mic/thinking buttons)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = GemTalkDesign.drawerSettingsEndInset),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            GemTalkIconControl(
                                onClick = onOpenSettings,
                                tone = IconControlTone.Neutral,
                            ) {
                                Icon(
                                    Icons.Rounded.Settings,
                                    contentDescription = s.t("설정", "Settings"),
                                    modifier = Modifier.size(GemTalkDesign.compactIconSize),
                                )
                            }
                        }
                        Spacer(
                            Modifier.height(
                                (GemTalkDesign.drawerSettingsBottomSpacing - navigationBottom).coerceAtLeast(0.dp),
                            ),
                        )
                    }
                }
            }
        }

        actionTarget?.let { session ->
            ModalBottomSheet(
                onDismissRequest = { actionTarget = null },
                sheetState = rememberModalBottomSheetState(),
                containerColor = GraphiteRaised,
                contentColor = Mist,
                dragHandle = { BottomSheetDefaults.DragHandle(color = Jade) },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SessionActionRow(
                        title = s.t("이름 변경", "Rename"),
                        icon = Icons.Rounded.Edit,
                        onClick = {
                            interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                            renamedTitle = TextFieldValue(
                                text = session.title,
                                selection = TextRange(session.title.length),
                            )
                            actionTarget = null
                            renameTarget = session
                        },
                    )
                    SessionActionRow(
                        title = s.t("삭제", "Delete"),
                        icon = Icons.Rounded.DeleteOutline,
                        destructive = true,
                        onClick = {
                            interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                            actionTarget = null
                            onDelete(session)
                        },
                    )
                }
            }
        }

        if (renameTarget != null) {
            GemTextInputDialog(
                title = s.t("이름 변경", "Rename chat"),
                value = renamedTitle,
                onValueChange = { renamedTitle = it },
                confirmLabel = s.t("저장", "Save"),
                dismissLabel = s.t("취소", "Cancel"),
                onDismiss = { renameTarget = null },
                onConfirm = {
                    renameTarget?.let { target ->
                        onRename(target.id, renamedTitle.text.trim())
                        renameTarget = null
                    }
                },
            )
        }
    }
}

@Composable
private fun SessionActionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GraphiteSoft,
        contentColor = if (destructive) MaterialTheme.colorScheme.error else Mist,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SessionRow(
    session: ChatSession,
    active: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val interactionHaptics = LocalInteractionHaptics.current
    val s = LocalStrings.current
    val rowShape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier
            .heightIn(min = 56.dp)
            .clip(rowShape)
            .combinedClickable(
                onClick = { interactionHaptics?.perform(HapticFeedbackType.TextHandleMove); onClick() },
                onLongClick = {
                    interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                    onLongPress()
                },
            ),
        color = if (active) GraphiteRaised else Color.Transparent,
        shape = rowShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = session.title.ifBlank { s.t("새 대화", "New chat") },
                style = MaterialTheme.typography.bodyMedium,
                color = Mist.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

