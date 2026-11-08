package com.hyngng.gemtalk.ui.components

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Spanned
import android.text.TextPaint
import android.util.Size
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hyngng.gemtalk.data.ChatMessage
import com.hyngng.gemtalk.data.ChatRole
import com.hyngng.gemtalk.ui.components.GemTalkIconControl
import com.hyngng.gemtalk.ui.components.IconControlTone
import com.hyngng.gemtalk.ui.components.LocalInteractionHaptics
import com.hyngng.gemtalk.ui.theme.ErrorTint
import com.hyngng.gemtalk.ui.theme.Hairline
import com.hyngng.gemtalk.ui.theme.Jade
import com.hyngng.gemtalk.ui.theme.Mist
import com.hyngng.gemtalk.ui.theme.MistFaint
import com.hyngng.gemtalk.ui.theme.MistMuted
import com.hyngng.gemtalk.ui.i18n.LocalStrings
import com.hyngng.gemtalk.ui.theme.UserBubble
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.MarkwonTheme
import android.text.method.LinkMovementMethod
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableRowSpan
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import org.commonmark.node.ListBlock
import org.commonmark.node.Node
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    toolRecords: List<ChatMessage> = emptyList(),
    onEditMessage: ((ChatMessage) -> Unit)? = null,
) {
    if (message.role == ChatRole.TOOL) {
        ToolCallRecord(message.text)
        return
    }
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!isUser && message.thinkingEnabled && toolRecords.isEmpty()) {
                ThoughtStream(message = message, isStreaming = isStreaming)
            }

            toolRecords.forEach { record ->
                if (message.thinkingEnabled && toolRecords.isNotEmpty()) Spacer(Modifier.height(6.dp))
                ToolCallRecord(record.text)
            }
            if (!isUser && message.thinkingEnabled && toolRecords.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                ThoughtStream(message = message, isStreaming = isStreaming)
            }
            if ((message.thinkingEnabled || toolRecords.isNotEmpty()) && message.text.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
            }

            if (message.isError) {
                ErrorBubble(message.text)
            } else if (isUser) {
                if (message.attachmentUris.isNotEmpty()) {
                    PhotoGallerySection(photoUris = message.attachmentUris, alignEnd = true)
                    if (message.text.isNotBlank()) Spacer(Modifier.height(10.dp))
                }
                UserBubble(message.text)
            } else {
                AssistantText(
                    text = message.text,
                    photoUris = message.photoUris,
                    isStreaming = isStreaming,
                )
            }

            MessageFooter(
                message = message,
                isUser = isUser,
                isStreaming = isStreaming,
                onEdit = onEditMessage?.let { cb -> { cb(message) } },
            )
        }
    }
}

@Composable
private fun ToolCallRecord(text: String) {
    // Existing saved tool rows may still contain the former "n/12" prefix.
    val displayText = text.replaceFirst(
        Regex("^도구 호출 \\d+/\\d+"),
        LocalStrings.current.t("도구 호출", "Tool call"),
    )
    SelectionContainer {
        Text(
            text = displayText,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 17.sp),
            color = MistMuted,
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    val shape = RoundedCornerShape(22.dp, 22.dp, 7.dp, 22.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(UserBubble)
            .padding(15.dp),
    ) {
        // User text is short-lived chat prose: let the bubble hug the content
        // instead of stretching to the column width like assistant messages.
        RenderedMarkdownText(text, textColor = Mist, fillsWidth = false)
    }
}

@Composable
private fun ErrorBubble(text: String) {
    val shape = RoundedCornerShape(7.dp, 22.dp, 22.dp, 22.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(ErrorTint)
            .border(1.dp, MaterialTheme.colorScheme.errorContainer, shape)
            .padding(horizontal = 15.dp, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp).size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun AssistantText(
    text: String,
    photoUris: List<String> = emptyList(),
    isStreaming: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 1.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (text.isNotBlank()) {
            RenderedMarkdownText(text, cacheable = !isStreaming)
        }
        if (photoUris.isNotEmpty()) {
            PhotoGallerySection(photoUris = photoUris)
        }
    }
}

@Composable
private fun PhotoGallerySection(
    photoUris: List<String>,
    alignEnd: Boolean = false,
) {
    var selectedPage by remember { mutableStateOf<Int?>(null) }
    val displayPhotoUris = remember(photoUris, alignEnd) {
        if (alignEnd) photoUris.asReversed() else photoUris
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        reverseLayout = alignEnd,
        horizontalArrangement = Arrangement.spacedBy(
            10.dp,
            if (alignEnd) Alignment.End else Alignment.Start,
        ),
    ) {
        itemsIndexed(displayPhotoUris, key = { _, uri -> uri }) { displayIndex, uri ->
            PhotoThumbnailCard(
                uriString = uri,
                onClick = {
                    selectedPage = if (alignEnd) photoUris.lastIndex - displayIndex else displayIndex
                },
            )
        }
    }

    selectedPage?.let { page ->
        PhotoLightboxDialog(
            photoUris = photoUris,
            initialPage = page,
            onDismiss = { selectedPage = null },
        )
    }
}

@Composable
private fun PhotoThumbnailCard(
    uriString: String,
    onClick: () -> Unit,
) {
    val bitmap = rememberThumbnailBitmap(uriString = uriString, targetSize = 400)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(shape)
            .background(UserBubble)
            .border(1.dp, Hairline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PhotoLightboxDialog(
    photoUris: List<String>,
    initialPage: Int,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { photoUris.size }
    val s = LocalStrings.current
    val interactionHaptics = LocalInteractionHaptics.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE60A0C0E)),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val bitmap = rememberThumbnailBitmap(uriString = photoUris[page], targetSize = 1440)
                if (bitmap != null) {
                    var scale by remember(page) { mutableFloatStateOf(1f) }
                    var offset by remember(page) { mutableStateOf(Offset.Zero) }
                    // Size of the ContentScale.Fit drawing area, needed to stop
                    // the panned image from leaving the frame.
                    var drawArea by remember(page) { mutableStateOf(IntSize.Zero) }

                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage != page && (scale != 1f || offset != Offset.Zero)) {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    }

                    fun clampOffset(newScale: Float, newOffset: Offset): Offset {
                        if (drawArea == IntSize.Zero) return Offset.Zero
                        val fitFactor =
                            minOf(
                                drawArea.width / bitmap.width.toFloat(),
                                drawArea.height / bitmap.height.toFloat(),
                            )
                        val drawnW = bitmap.width * fitFactor * newScale
                        val drawnH = bitmap.height * fitFactor * newScale
                        val maxX = ((drawnW - drawArea.width) / 2f).coerceAtLeast(0f)
                        val maxY = ((drawnH - drawArea.height) / 2f).coerceAtLeast(0f)
                        return Offset(newOffset.x.coerceIn(-maxX, maxX), newOffset.y.coerceIn(-maxY, maxY))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(bitmap) {
                                detectLightboxGestures(
                                    scale = { scale },
                                    offset = { offset },
                                    center = { Offset(size.width / 2f, size.height / 2f) },
                                    onPinch = { startScale, startOffset, startCentroidOffset, zoomRatio, centroidDelta ->
                                        val newScale = (startScale * zoomRatio).coerceIn(1f, 5f)
                                        if (newScale <= 1.001f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            // Keep the content point under the initial pinch
                                            // centroid fixed while the centroid drifts.
                                            val ratio = newScale / startScale
                                            val newOffset =
                                                startOffset * ratio - startCentroidOffset * (ratio - 1f) + centroidDelta
                                            scale = newScale
                                            offset = clampOffset(newScale, newOffset)
                                        }
                                    },
                                    onPan = { panDelta ->
                                        offset = clampOffset(scale, offset + panDelta)
                                    },
                                    onEnd = {
                                        if (scale < 1.05f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        }
                                    },
                                )
                            }
                            .pointerInput(bitmap) {
                                detectTapGestures(
                                    onDoubleTap = { position ->
                                        if (scale > 1.001f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            val newScale = 2.5f
                                            val ratio = newScale / scale
                                            // Anchor the zoom at the tapped point.
                                            val anchored =
                                                position - Offset(size.width / 2f, size.height / 2f)
                                            scale = newScale
                                            offset = clampOffset(newScale, offset * ratio - anchored * (ratio - 1f))
                                        }
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                                .onSizeChanged { drawArea = it }
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                        )
                    }
                }
            }
            if (photoUris.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${photoUris.size}",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp)
                        .background(Color(0x550A0C0E), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GemTalkIconControl(
                    onClick = onDismiss,
                    tone = IconControlTone.Quiet,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = s.t("닫기", "Close"),
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberThumbnailBitmap(uriString: String, targetSize: Int): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uriString, targetSize) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uriString, targetSize) {
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Avoid OOM by leveraging OS hardware-backed thumbnail cache
                    context.contentResolver.loadThumbnail(uri, Size(targetSize, targetSize), null)
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeStream(stream, null, boundsOptions)
                        val sample = maxOf(1, maxOf(boundsOptions.outWidth, boundsOptions.outHeight) / targetSize)
                        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }
                        context.contentResolver.openInputStream(uri)?.use { streamToDecode ->
                            BitmapFactory.decodeStream(streamToDecode, null, decodeOptions)
                        }
                    }
                }
                rawBitmap?.asImageBitmap()
            }.getOrNull()?.let {
                bitmap = it
            }
        }
    }

    return bitmap
}

@Composable
private fun MessageFooter(
    message: ChatMessage,
    isUser: Boolean,
    isStreaming: Boolean,
    onEdit: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboard.current
    val s = LocalStrings.current
    val showCopy = !isStreaming && message.text.isNotBlank()
    val showEdit = isUser && !isStreaming && message.text.isNotBlank() && onEdit != null
    val hasContent = (isUser && (showCopy || showEdit)) || (!isUser && (message.isError || showCopy))
    if (!hasContent) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp, start = if (isUser) 0.dp else 2.dp, end = if (isUser) 0.dp else 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isUser) {
            if (showEdit) {
                EditButton(onEdit = onEdit)
            }
            if (showEdit && showCopy) {
                Spacer(Modifier.width(2.dp))
            }
            if (showCopy) {
                CopyButton(
                    onCopy = {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("GemTalk", message.text)),
                        )
                    },
                )
            }
        } else {
            if (message.isError) {
                Text(
                    text = s.t("확인 필요", "Needs attention"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                if (showCopy) {
                    Spacer(Modifier.width(6.dp))
                }
            }
            if (showCopy) {
                CopyButton(
                    onCopy = {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("GemTalk", message.text)),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun EditButton(onEdit: () -> Unit) {
    val s = LocalStrings.current
    var edited by remember { mutableStateOf(false) }
    var feedbackId by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(feedbackId) {
        if (feedbackId == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(1_000)
        edited = false
    }

    GemTalkIconControl(
        onClick = {
            onEdit()
            edited = true
            feedbackId += 1
        },
        modifier = Modifier,
        tone = if (edited) IconControlTone.Success else IconControlTone.Quiet,
        size = 28.dp,
    ) {
        Icon(
            imageVector = if (edited) Icons.Rounded.Check else Icons.Rounded.Edit,
            contentDescription = if (edited) s.t("수정 시작", "Start editing") else s.t("수정", "Edit"),
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun CopyButton(onCopy: suspend () -> Unit) {
    val s = LocalStrings.current
    var copied by remember { mutableStateOf(false) }
    var feedbackId by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(feedbackId) {
        if (feedbackId == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(1_000)
        copied = false
    }

    GemTalkIconControl(
        onClick = {
            scope.launch {
                runCatching { onCopy() }.onSuccess {
                    copied = true
                    feedbackId += 1
                }
            }
        },
        modifier = Modifier,
        tone = if (copied) IconControlTone.Success else IconControlTone.Quiet,
        size = 28.dp,
    ) {
        Icon(
            imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
            contentDescription = if (copied) s.t("복사 완료", "Copied") else s.t("복사", "Copy"),
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
@OptIn(FlowPreview::class)
private fun ThoughtStream(message: ChatMessage, isStreaming: Boolean) {
    val interactionHaptics = LocalInteractionHaptics.current
    val s = LocalStrings.current
    val hasThought = message.thoughtText.isNotBlank()
    var readerOpen by remember(message.id) { mutableStateOf(false) }
    var readerText by remember(message.id) { mutableStateOf(message.thoughtText) }
    val previewScroll = rememberScrollState()
    val panelShape = RoundedCornerShape(12.dp)
    val thoughtComplete = message.thinkingFinished || message.text.isNotBlank()
    val status = when {
        message.thinkingUnavailable -> s.t("생각 미지원", "Thinking unavailable")
        isStreaming && !message.thinkingFinished -> s.t("생각 중", "Thinking")
        thoughtComplete -> s.t("생각 완료", "Thought complete")
        isStreaming -> s.t("생각 중", "Thinking")
        hasThought -> s.t("생각 중단", "Thinking stopped")
        else -> s.t("생각 원문 없음", "No thought text")
    }

    LaunchedEffect(message.id, readerOpen, message.thoughtText) {
        if (readerOpen) readerText = message.thoughtText
    }

    val isThinkingStreamActive = isStreaming && !message.thinkingFinished && !message.thinkingUnavailable
    // Include the first thought chunk in the keys. The effect starts before the
    // model returns a thought; without this key it exits once and never scrolls.
    LaunchedEffect(message.id, hasThought, isThinkingStreamActive, isStreaming, readerOpen) {
        if (!hasThought || readerOpen) return@LaunchedEffect
        if (isThinkingStreamActive) {
            snapshotFlow { previewScroll.maxValue }
                .distinctUntilChanged()
                .debounce(70)
                .collectLatest { target ->
                    val distance = (target - previewScroll.value).coerceAtLeast(0)
                    if (distance > 0) {
                        previewScroll.animateScrollTo(
                            target,
                            animationSpec = tween(
                                durationMillis = (260 + distance / 2).coerceIn(260, 520),
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }
                }
        } else if (isStreaming && message.text.isNotBlank()) {
            // The Markdown view may complete its native layout just after the
            // first answer token arrives; keep settling only when that extent grows.
            snapshotFlow { previewScroll.maxValue }
                .distinctUntilChanged()
                .debounce(70)
                .collectLatest { target ->
                    previewScroll.animateScrollTo(
                        target,
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                    )
                }
        } else if (!isStreaming && message.text.isNotBlank()) {
            repeat(2) { androidx.compose.runtime.withFrameNanos { } }
            previewScroll.scrollTo(previewScroll.maxValue)
        }
    }

    Box(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 1.dp).clip(panelShape)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isThinkingStreamActive && !message.thinkingUnavailable) {
                        ThinkingDots(modifier = Modifier.width(22.dp))
                    } else if (!message.thinkingUnavailable && thoughtComplete) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Jade.copy(alpha = 0.82f),
                            modifier = Modifier.size(13.dp),
                        )
                    } else {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(MistMuted))
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            letterSpacing = 0.15.sp,
                        ),
                        color = MistMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 94.dp)
                        .verticalAlphaMask(
                            top = AlphaMaskEdge(extentFraction = 0.10f),
                            bottom = AlphaMaskEdge(extentFraction = 0.10f),
                        )
                        // Keep the mask in the fixed-height viewport. If it is
                        // placed inside verticalScroll it follows the unbounded
                        // text content and its fade edges move while scrolling.
                        .verticalScroll(previewScroll, enabled = false),
                ) {
                    Column(Modifier.padding(bottom = 4.dp)) {
                        when {
                            hasThought -> RenderedMarkdownText(
                                message.thoughtText,
                                thought = true,
                                cacheable = !isStreaming,
                            )
                            message.thinkingUnavailable -> Text(
                                s.t("일반 응답으로 계속 진행합니다.", "Continuing with a normal response."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MistMuted,
                            )
                            isStreaming -> Text(
                                s.t("모델의 생각이 여기에 이어집니다.", "The model's thoughts appear here."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MistMuted,
                            )
                            else -> Text(
                                s.t("이 응답에는 표시할 생각 원문이 없습니다.", "This response has no thought text to show."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MistMuted,
                            )
                        }
                    }
                }
            }
            // This transparent hit surface sits above the Markdown renderer so
            // its text-selection and scroll gestures cannot steal a panel tap.
            if (hasThought) {
                Surface(
                    onClick = {
                        interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                        readerText = message.thoughtText
                        readerOpen = true
                    },
                    modifier = Modifier.matchParentSize(),
                    shape = panelShape,
                    color = Color.Transparent,
                    contentColor = Color.Transparent,
                ) { }
            }
    }

    if (readerOpen) {
        Dialog(
            onDismissRequest = { readerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).heightIn(max = 760.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 760.dp)
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(s.t("생각 전문", "Full thoughts"), style = MaterialTheme.typography.titleLarge, color = Mist)
                        }
                        IconButton(onClick = {
                            interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                            readerOpen = false
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = s.t("생각 전문 닫기", "Close full thoughts"), tint = MistMuted)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Hairline)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 620.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 14.dp),
                    ) {
                        RenderedMarkdownText(
                            readerText,
                            thought = true,
                        )
                    }
                }
            }
        }
    }
}

private class CompactListBlockHandler : MarkwonVisitor.BlockHandler {
    override fun blockStart(visitor: MarkwonVisitor, node: Node) {
        visitor.ensureNewLine()
    }

    override fun blockEnd(visitor: MarkwonVisitor, node: Node) {
        if (visitor.hasNext(node)) {
            visitor.ensureNewLine()
            // 다음 블록이 하위 목록(ListBlock)인 경우 제목과 목록 사이의 거슬리는 강제 빈 줄(forceNewLine)을 건너뛴다.
            if (node.next !is ListBlock) {
                visitor.forceNewLine()
            }
        }
    }
}

private data class MarkdownSpannedKey(
    val text: String,
    val color: Int,
    val width: Int,
    val thought: Boolean,
)

private val markdownSpannedCache = object : LinkedHashMap<MarkdownSpannedKey, Spanned>(32, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<MarkdownSpannedKey, Spanned>?): Boolean {
        return size > 64
    }
}

// Markwon TablePlugin은 첫 onMeasure() 시점에 행 높이를 계산하지 못하고 draw() 시점에
// 실제 Canvas 폭으로 StaticLayout을 생성한 뒤 view.post { setText() }를 통해 다음 프레임에 비동기 재측정을 트리거한다.
// 이 2단계 지연 팽창으로 인해 LazyColumn 재활용 시 높이가 급격히 널뛰어(100dp -> 500dp) 스크롤이 반발하고 관성을 잃는다.
// targetWidth 폭을 가진 1px 더미 캔버스를 통해 TableRowSpan들의 draw()를 사전에 1회 호출해두면,
// 첫 onMeasure()부터 이미 계산된 실제 높이와 폭이 즉각 반환되어 비동기 재측정과 높이 요동을 원천 차단할 수 있다.
private fun prelayoutTableSpans(
    spanned: Spanned,
    targetWidth: Int,
    textPaint: TextPaint,
) {
    if (targetWidth <= 0 || spanned.isEmpty()) return
    val tableSpans = spanned.getSpans(0, spanned.length, TableRowSpan::class.java)
    if (tableSpans.isNullOrEmpty()) return

    runCatching {
        val dummyBitmap = Bitmap.createBitmap(targetWidth, 1, Bitmap.Config.ALPHA_8)
        val dummyCanvas = Canvas(dummyBitmap)
        for (span in tableSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            span.draw(dummyCanvas, spanned, start, end, 0f, 0, 0, 0, textPaint)
        }
        dummyBitmap.recycle()
    }
}

@Composable
@OptIn(FlowPreview::class)
private fun RenderedMarkdownText(
    text: String,
    thought: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    fillsWidth: Boolean = true,
    cacheable: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bodyColor = if (thought) {
        if (isDark) Color(0xFFADB4BB) else Color(0xFF70777F)
    } else {
        textColor
    }
    val mathSpanCache = remember(context, bodyColor, density) {
        RaTeXSpanCache(
            context = context.applicationContext,
            fontSizeDp = 15.5f * density.fontScale,
            color = bodyColor.toArgb(),
        )
    }
    val markwon = remember(context, thought, isDark, colorScheme, textColor, density, mathSpanCache) {
        Markwon.builder(context)
            .usePlugin(
                MarkwonInlineParserPlugin.create { factoryBuilder ->
                    factoryBuilder.addInlineProcessor(MathInlineProcessor())
                },
            )
            .usePlugin(RaTeXMarkwonPlugin(mathSpanCache))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(
                object : AbstractMarkwonPlugin() {
                    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                        builder.blockHandler(CompactListBlockHandler())
                    }

                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder
                            .linkColor(colorScheme.primary.toArgb())
                            .listItemColor(bodyColor.toArgb())
                            .blockMargin(with(density) { 16.dp.roundToPx() })
                            .codeTextColor((if (thought) bodyColor else colorScheme.primary).toArgb())
                            .codeBackgroundColor(colorScheme.surfaceVariant.toArgb())
                            .codeBlockTextColor(bodyColor.toArgb())
                            .codeBlockBackgroundColor(colorScheme.surfaceVariant.toArgb())
                            .codeTypeface(Typeface.MONOSPACE)
                            .codeBlockTypeface(Typeface.MONOSPACE)
                            .headingTextSizeMultipliers(floatArrayOf(1.6f, 1.35f, 1.16f, 1f, 0.92f, 0.84f))
                    }
                },
            )
            .build()
    }
    BoxWithConstraints(modifier = if (fillsWidth) Modifier.fillMaxWidth() else Modifier) {
        val availableWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else context.resources.displayMetrics.widthPixels
        val spanned = remember(text, bodyColor, availableWidth, thought, markwon, cacheable) {
            // Streaming partials change on every token, so they bypass the
            // global LRU entirely; only settled text earns a cache entry.
            val cached = if (!cacheable) null else synchronized(markdownSpannedCache) {
                markdownSpannedCache[MarkdownSpannedKey(text, bodyColor.toArgb(), availableWidth, thought)]
            }
            cached ?: run {
                val parsed = markwon.toMarkdown(text)
                if (availableWidth > 0) {
                    val textPaint = TextPaint().apply {
                        textSize = with(density) { 15.5.sp.toPx() }
                        color = bodyColor.toArgb()
                    }
                    prelayoutTableSpans(parsed, availableWidth, textPaint)
                }
                if (cacheable) {
                    synchronized(markdownSpannedCache) {
                        markdownSpannedCache[MarkdownSpannedKey(text, bodyColor.toArgb(), availableWidth, thought)] = parsed
                    }
                }
                parsed
            }
        }

        AndroidView(
            factory = { viewContext ->
                TextView(viewContext).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f)
                    setTextColor(bodyColor.toArgb())
                    setLineSpacing(with(density) { 5.5.sp.toPx() }, 1f)
                    typeface = Typeface.DEFAULT
                    includeFontPadding = false
                    setTextIsSelectable(false)
                    background = null
                    setPadding(0, 0, 0, 0)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    // LinkMovementMethod so theme'd linkColor is tappable.
                    movementMethod = LinkMovementMethod.getInstance()
                }
            },
            modifier = if (fillsWidth) Modifier.fillMaxWidth() else Modifier,
            update = { view ->
                view.setTextColor(bodyColor.toArgb())
                markwon.setParsedMarkdown(view, spanned)
            },
        )
    }
}
