package app.techdox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.techdox.container
import app.techdox.data.Reflections
import kotlinx.coroutines.launch

private val fieldsSaver = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

/** Starting points the user can tap instead of typing. */
private val SUGGESTIONS = listOf(
    "I want to read more",
    "Be present with the people around me",
    "Go to bed on time",
    "Finish what I start",
    "Less comparing, more doing",
)

/**
 * Lets the user write their own reasons for cutting back (up to [Reflections.MAX_REASONS]).
 * Every change is saved straight away; these lines are what the pause card shows.
 *
 * @param initial the reasons saved so far, read once when the editor opens
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReasonsEditor(initial: List<String>, modifier: Modifier = Modifier) {
    val settings = LocalContext.current.container.settings
    val scope = rememberCoroutineScope()
    // Always start with one field so there is somewhere to type.
    // Saveable, so empty "Add another" fields and half-typed text survive rotation and resizing.
    val fields = rememberSaveable(saver = fieldsSaver) { initial.ifEmpty { listOf("") }.toMutableStateList() }
    fun save() { scope.launch { settings.setReasons(fields.toList()) } }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        fields.forEachIndexed { i, value ->
            OutlinedTextField(
                value = value,
                onValueChange = {
                    fields[i] = it.replace('\n', ' ').take(Reflections.MAX_REASON_LENGTH)
                    save()
                },
                placeholder = { Text(if (i == 0) "Why do you want to cut back?" else "Another reason") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                trailingIcon = {
                    if (fields.size > 1 || value.isNotEmpty()) {
                        IconButton(onClick = {
                            if (fields.size > 1) fields.removeAt(i) else fields[0] = ""
                            save()
                        }) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (fields.size < Reflections.MAX_REASONS) {
            TextButton(onClick = { fields.add("") }) { Text("Add another") }
        }

        val unused = SUGGESTIONS.filter { it !in fields }
        if (unused.isNotEmpty() && fields.count { it.isNotBlank() } < Reflections.MAX_REASONS) {
            Text(
                "Or tap one to start with",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                unused.forEach { s ->
                    SuggestionChip(
                        onClick = {
                            // Fill the first empty field, or add a new one.
                            val empty = fields.indexOfFirst { it.isBlank() }
                            if (empty >= 0) fields[empty] = s else fields.add(s)
                            save()
                        },
                        label = { Text(s) },
                    )
                }
            }
        }
    }
}
