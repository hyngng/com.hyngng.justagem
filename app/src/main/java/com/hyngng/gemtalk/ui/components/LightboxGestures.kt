package com.hyngng.gemtalk.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.hypot

/**
 * Gesture driving for the photo lightbox, written on top of awaitEachGesture
 * instead of detectTransformGestures / transformable because both of those
 * consume pointer events broadly and starve the surrounding HorizontalPager
 * (that was the failure of the two previous attempts, see
 * docs/history/2026-11-07-lightbox-swipe-and-topbar-button-alignment.md and
 * docs/history/2026-11-07-voice-overlay-padding-isolation-and-transformable.md).
 *
 * The consumption policy is the whole point of this detector:
 * - one finger at 1x scale: nothing is consumed, so HorizontalPager swipes.
 * - two fingers: pinch zoom, changing the page is impossible while pinching.
 * - one finger while zoomed: pan, bounded so the bitmap cannot leave the view.
 *
 * Pinch anchoring: the gesture-start scale/offset/centroid/distance are
 * snapshotted once, then each event computes
 *   newScale = startScale * (distance / startDistance)
 *   newOffset = r * startOffset + centroidDelta - (r - 1) * (startCentroid - center)
 * (r = newScale / startScale), which keeps the content point under the
 * initial pinch centroid fixed without jitter — the reason a naive
 * `scale *= zoom` version shakes.
 */
internal suspend fun PointerInputScope.detectLightboxGestures(
  scale: () -> Float,
  offset: () -> Offset,
  center: () -> Offset,
  onPinch: (startScale: Float, startOffset: Offset, startCentroidOffset: Offset, zoomRatio: Float, centroidDelta: Offset) -> Unit,
  onPan: (panDelta: Offset) -> Unit,
  onEnd: () -> Unit,
) = awaitEachGesture {
  awaitFirstDown(requireUnconsumed = false)

  var pinchStartScale = 0f
  var pinchStartOffset = Offset.Zero
  var pinchStartCentroidOffset = Offset.Zero
  var pinchStartDistance = 0f
  var pinching = false
  var panning = false
  var lastPanPosition = Offset.Zero

  while (true) {
    val event = awaitPointerEvent()
    val pressed = event.changes.filter { it.pressed }
    if (pressed.isEmpty()) break

    if (pressed.size >= 2) {
      val first = pressed[0].position
      val second = pressed[1].position
      val centroid = (first + second) / 2f
      val distance = hypot(second.x - first.x, second.y - first.y)

      if (!pinching) {
        pinching = true
        panning = false
        pinchStartScale = scale()
        pinchStartOffset = offset()
        pinchStartCentroidOffset = centroid - center()
        pinchStartDistance = distance.coerceAtLeast(1f)
      } else {
        val zoomRatio = distance / pinchStartDistance
        onPinch(pinchStartScale, pinchStartOffset, pinchStartCentroidOffset, zoomRatio, centroid - center() - pinchStartCentroidOffset)
      }
      event.changes.forEach { if (it.positionChanged()) it.consume() }
    } else {
      if (scale() > 1.001f) {
        if (!panning) {
          panning = true
          pinching = false
          lastPanPosition = pressed[0].position
        } else {
          val delta = pressed[0].position - lastPanPosition
          lastPanPosition = pressed[0].position
          onPan(delta)
        }
        event.changes.forEach { if (it.positionChanged()) it.consume() }
      } else {
        pinching = false
        panning = false
      }
    }
  }
  onEnd()
}
