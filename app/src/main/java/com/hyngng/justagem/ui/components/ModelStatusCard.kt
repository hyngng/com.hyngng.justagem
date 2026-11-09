package com.hyngng.justagem.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyngng.justagem.ui.ModelPhase
import com.hyngng.justagem.ui.ModelUi
import com.hyngng.justagem.ui.i18n.LocalStrings
import com.hyngng.justagem.ui.theme.GraphiteRaised
import com.hyngng.justagem.ui.theme.GraphiteSoft
import com.hyngng.justagem.ui.theme.Jade
import com.hyngng.justagem.ui.theme.JadeContent
import com.hyngng.justagem.ui.theme.Mist
import com.hyngng.justagem.ui.theme.MistFaint
import com.hyngng.justagem.ui.theme.MistMuted

@Composable
fun ModelStatusCard(
    model: ModelUi,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    val interactionHaptics = LocalInteractionHaptics.current
    val context = LocalContext.current
    val s = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GraphiteRaised.copy(alpha = 0.92f))
            .animateContentSize()
            .padding(18.dp),
    ) {
        when (model.phase) {
            // Keep this empty because the parent reserves the loading slot with a shimmer placeholder.
            ModelPhase.CHECKING -> {
            }

            ModelPhase.DOWNLOADABLE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Jade.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            tint = Jade,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = s.t("${model.label} 준비", "Set up ${model.label}"),
                            style = MaterialTheme.typography.titleMedium,
                            color = Mist,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = s.t(
                        "와이파이를 권장합니다",
                        "Wi-Fi is recommended",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MistMuted,
                )
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                            onDownload()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Jade,
                            contentColor = JadeContent,
                        ),
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(s.t("모델 내려받기", "Download model"), style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = {
                        interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                        onRetry()
                    }) {
                        Text(s.t("다시 확인", "Check again"), color = MistMuted, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            ModelPhase.DOWNLOADING -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = s.t("${model.label} 내려받는 중", "Downloading ${model.label}"),
                        style = MaterialTheme.typography.titleMedium,
                        color = Mist,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatDownload(model.downloadedBytes, model.totalBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Jade,
                    )
                }
                Spacer(Modifier.height(12.dp))
                val fraction = if (model.totalBytes > 0) {
                    (model.downloadedBytes.toFloat() / model.totalBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    null
                }
                if (fraction == null) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Jade,
                        trackColor = GraphiteSoft,
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Jade,
                        trackColor = GraphiteSoft,
                    )
                }
            }

            ModelPhase.READY -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Jade),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = model.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = Mist,
                    )
                    if (model.fallback) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = s.t("E4B 대신 경량 모델", "Lightweight model instead of E4B"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MistFaint,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    model.baseModel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MistFaint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            ModelPhase.UNAVAILABLE, ModelPhase.ERROR -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A2327)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFFB4AB),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (model.phase == ModelPhase.ERROR) {
                            s.t("모델을 준비하지 못했어요", "The model could not be prepared")
                        } else {
                            s.t("이 기기에서는 모델을 쓸 수 없어요", "The model is unavailable on this device")
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Mist,
                    )
                }

                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = s.t("함께 보기", "See also"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MistMuted,
                    )
                    ModelRequirementLink(
                        context = context,
                        label = s.t("Prompt API 요구 사항·기기 지원 안내", "Prompt API requirements and device support"),
                        url = "https://developers.google.com/ml-kit/genai/prompt/android",
                    )
                    ModelRequirementLink(
                        context = context,
                        label = s.t("지원 기기 목록", "Supported device list"),
                        url = "https://developers.google.com/ml-kit/genai",
                    )
                    ModelRequirementLink(
                        context = context,
                        label = s.t("AICore 개발자 프리뷰 등록 (Gemini Nano v4 미리 받기)", "Enroll in the AICore Developer Preview (get Gemini Nano v4 early)"),
                        url = "https://developers.google.com/ml-kit/genai/aicore-dev-preview",
                    )
                }

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        interactionHaptics?.perform(HapticFeedbackType.TextHandleMove)
                        onRetry()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GraphiteSoft,
                        contentColor = Mist,
                    ),
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(s.t("상태 다시 확인", "Check status again"), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

fun formatDownload(downloaded: Long, total: Long): String {
    fun mb(value: Long) = "%.1f MB".format(value / 1_048_576f)
    return if (total > 0) "${mb(downloaded)} / ${mb(total)}" else mb(downloaded)
}

/** Opens a requirements/help page from the model status card; styled in Jade like a text link. */
@Composable
private fun ModelRequirementLink(
    context: Context,
    label: String,
    url: String,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = Jade,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        },
    )
}
