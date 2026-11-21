package app.techdox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * Widest a screen's content may get. Phones (under ~600 dp) are unaffected; on unfolded
 * foldables, tablets and desktop windows the content stays readable and centred instead of
 * stretching edge-to-edge.
 */
val ContentMaxWidth = 640.dp

/** Centres content and caps its width at [max]. Put it after any scroll modifier. */
fun Modifier.centeredContent(max: Dp = ContentMaxWidth): Modifier =
    fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max)

/**
 * True when the window is short (under 480 dp tall): a phone in landscape, or a split-screen or
 * desktop window. Based on the window size class, not on the device.
 */
@Composable
fun isWindowHeightCompact(): Boolean =
    !currentWindowAdaptiveInfoV2().windowSizeClass
        .isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)

/**
 * A full-size [LazyColumn] whose items are centred and capped at [ContentMaxWidth]. The list itself
 * stays full width, so it scrolls from anywhere in the window.
 */
@Composable
fun CenteredLazyColumn(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    BoxWithConstraints(modifier) {
        val side = ((maxWidth - ContentMaxWidth) / 2).coerceAtLeast(padding)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = side, vertical = padding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/** Horizontal inset that centres [ContentMaxWidth] of content in [available] width. */
fun centeringInset(available: Dp, min: Dp = 0.dp): Dp =
    ((available - ContentMaxWidth) / 2).coerceAtLeast(min)
