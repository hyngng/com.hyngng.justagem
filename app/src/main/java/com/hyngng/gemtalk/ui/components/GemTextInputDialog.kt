package com.hyngng.gemtalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hyngng.gemtalk.ui.theme.GraphiteRaised
import com.hyngng.gemtalk.ui.theme.GraphiteSoft
import com.hyngng.gemtalk.ui.theme.Jade
import com.hyngng.gemtalk.ui.theme.Mist
import com.hyngng.gemtalk.ui.theme.MistMuted

/** Text-entry sibling of GemConfirmDialog: shared 24dp surface and compact bottom-right action cluster. */
@Composable
fun GemTextInputDialog(
    title: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val textFieldInteraction = remember { MutableInteractionSource() }
    val isFocused by textFieldInteraction.collectIsFocusedAsState()
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Dialog(
        onDismissRequest = {
            keyboard?.hide()
            onDismiss()
        },
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = GraphiteRaised,
            modifier = Modifier.widthIn(max = 560.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Mist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                )
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp)
                        .background(GraphiteSoft, RoundedCornerShape(10.dp))
                        .border(
                            width = 1.dp,
                            color = if (isFocused) Jade else MistMuted.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 18.dp)
                        .focusRequester(focusRequester),
                    interactionSource = textFieldInteraction,
                    singleLine = true,
                    cursorBrush = SolidColor(Jade),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Mist),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (value.text.isNotBlank()) onConfirm() }),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .padding(end = 8.dp, bottom = 8.dp)
                        .align(androidx.compose.ui.Alignment.End),
                ) {
                    TextButton(
                        onClick = {
                            keyboard?.hide()
                            onDismiss()
                        },
                    ) { Text(dismissLabel, color = MistMuted) }
                    TextButton(
                        enabled = value.text.isNotBlank(),
                        onClick = {
                            keyboard?.hide()
                            onConfirm()
                        },
                    ) { Text(confirmLabel, color = Jade) }
                }
            }
        }
    }
}
