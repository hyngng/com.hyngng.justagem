package com.hyngng.gemtalk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.hyngng.gemtalk.ui.theme.GraphiteRaised
import com.hyngng.gemtalk.ui.theme.Mist
import com.hyngng.gemtalk.ui.theme.MistMuted

/**
 * Shared confirm dialog replacing Material3 AlertDialog so the action row
 * spacing is fully controlled: the button cluster keeps an equal, small
 * inset to the right edge and the bottom edge (8dp each, was 24dp).
 */
@Composable
fun GemConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissLabel: String? = null,
    bodyStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = GraphiteRaised,
            modifier = Modifier.widthIn(max = 560.dp),
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Mist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                )
                Text(
                    text = body,
                    style = bodyStyle,
                    color = MistMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp)
                        .verticalScroll(rememberScrollState()),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 8.dp, bottom = 8.dp),
                ) {
                    if (dismissLabel != null) {
                        TextButton(onClick = onDismissRequest) {
                            Text(dismissLabel, color = MistMuted)
                        }
                    }
                    TextButton(onClick = onConfirm) {
                        Text(confirmLabel, color = confirmColor)
                    }
                }
            }
        }
    }
}
