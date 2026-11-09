package com.hyngng.justagem.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class AlphaMaskEdge(
    val extent: Dp? = null,
    val extentFraction: Float? = null,
    // 화면 끝이 아닌 하단 독(입력창) 등 내부 UI 컴포넌트와의 경계에서 페이드가 끝나도록 오프셋을 지정한다.
    val inset: Dp = 0.dp,
    val insetPx: Float? = null,
    val maxFraction: Float = 0.5f,
    val curve: List<Pair<Float, Float>> = listOf(0f to 0f, 1f to 1f),
)

internal val SoftAlphaMaskCurve = listOf(
    0f to 0f,
    0.3f to 0.2f,
    0.7f to 0.7f,
    1f to 1f,
)

// 채팅 뷰포트 하단은 75% 불투명도(거의 불투명하게 가려짐, 잔여 메시지 투과율 25% = 0.25f)를 타겟으로 하여
// 입력창 도크와의 경계에서 1.0 -> 0.25로 완만하게 감쇠하며,
// 도크 뒤쪽(인셋)에서도 75% 불투명도(alpha = 0.25f)를 유지한다.
internal val ChatBottomAlphaMaskCurve = listOf(
    0f to 0.25f,
    0.3f to 0.35f,
    0.7f to 0.70f,
    1f to 1f,
)

internal fun Modifier.verticalAlphaMask(
    top: AlphaMaskEdge? = null,
    bottom: AlphaMaskEdge? = null,
): Modifier {
    if (top == null && bottom == null) return this

    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val height = size.height
            if (height <= 0f) return@drawWithContent

            val stops = mutableListOf<Pair<Float, Float>>()
            if (top == null) stops += 0f to 1f

            top?.let { edge ->
                val topInset = edge.insetPx ?: edge.inset.toPx()
                val topBoundary = topInset.coerceIn(0f, height)
                val availableHeight = (height - topBoundary).coerceAtLeast(0f)
                val requestedExtent = edge.extent?.toPx() ?: (edge.extentFraction ?: 0f) * availableHeight
                val extent = minOf(requestedExtent, availableHeight * edge.maxFraction.coerceIn(0f, 0.5f))

                val endAlpha = edge.curve.firstOrNull()?.second ?: 0f
                if (topBoundary > 0f) {
                    stops += 0f to endAlpha
                    stops += (topBoundary / height) to endAlpha
                }
                val extentRatio = if (height > 0f) extent / height else 0f
                val boundaryRatio = if (height > 0f) topBoundary / height else 0f
                edge.curve.forEach { (fraction, alpha) ->
                    val pos = boundaryRatio + fraction.coerceIn(0f, 1f) * extentRatio
                    stops += pos to alpha.coerceIn(0f, 1f)
                }
            }

            bottom?.let { edge ->
                val bottomInset = edge.insetPx ?: edge.inset.toPx()
                val bottomBoundary = (height - bottomInset).coerceIn(0f, height)
                val availableHeight = bottomBoundary.coerceAtLeast(0f)
                val requestedExtent = edge.extent?.toPx() ?: (edge.extentFraction ?: 0f) * availableHeight
                val extent = minOf(requestedExtent, availableHeight * edge.maxFraction.coerceIn(0f, 0.5f))

                val extentRatio = if (height > 0f) extent / height else 0f
                val boundaryRatio = if (height > 0f) bottomBoundary / height else 0f

                edge.curve.forEach { (fraction, alpha) ->
                    val pos = boundaryRatio - fraction.coerceIn(0f, 1f) * extentRatio
                    stops += pos to alpha.coerceIn(0f, 1f)
                }
                // 하단 인셋 영역(예: 입력창 뒤쪽)은 메시지 본문이 비치지 않도록 투명도를 유지한다.
                if (bottomBoundary < height) {
                    val endAlpha = edge.curve.firstOrNull()?.second ?: 0f
                    stops += boundaryRatio to endAlpha
                    stops += 1f to endAlpha
                }
            }
            if (bottom == null) stops += 1f to 1f

            val colorStops = stops
                .sortedBy { it.first }
                .distinctBy { it.first }
                .map { (position, alpha) -> position to Color.White.copy(alpha = alpha) }
                .toTypedArray()

            drawRect(
                brush = Brush.verticalGradient(
                    *colorStops,
                    startY = 0f,
                    endY = height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
}
