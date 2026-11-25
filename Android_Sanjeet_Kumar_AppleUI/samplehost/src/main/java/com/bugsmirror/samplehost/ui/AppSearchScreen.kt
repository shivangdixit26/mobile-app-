package com.bugsmirror.samplehost.ui

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.bugsmirror.riskchecker.AppInfo
import com.bugsmirror.riskchecker.MatchedPermission
import com.bugsmirror.riskchecker.PermissionResult
import com.bugsmirror.riskchecker.RiskChecker
import com.bugsmirror.samplehost.R
import com.bugsmirror.samplehost.SearchStatus
import com.bugsmirror.samplehost.ui.theme.LocalEffectSettings
import com.bugsmirror.samplehost.ui.theme.Surfaces

/** Icons are drawn at 40dp; 128px keeps them sharp on high-density screens. */
private const val ICON_SIZE_PX = 128
private const val PLACEHOLDER_ROWS = 3
private val SCREEN_MARGIN = 16.dp
private val PRIMARY_BUTTON_HEIGHT = 52.dp

/** "Apps by permission" tab: the search box, messages, and the matching apps. */
@Composable
fun AppSearchScreen(
    query: String,
    status: SearchStatus,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val search = {
        keyboard?.hide()
        onSearch()
    }
    val reduceMotion = LocalEffectSettings.current.reduceMotion

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = screenPadding(contentPadding, SCREEN_MARGIN),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_label)) },
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                supportingText = {
                    Text(stringResource(R.string.search_supported, RiskChecker.SUPPORTED_LABELS.joinToString(", ")))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Surfaces.card,
                    unfocusedContainerColor = Surfaces.card,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { SearchButton(enabled = status != SearchStatus.Loading, onClick = search) }

        when (status) {
            SearchStatus.Idle -> Unit
            SearchStatus.EmptyQuery -> item { MessageCard(stringResource(R.string.search_empty)) }
            SearchStatus.Loading -> loadingPlaceholders()
            is SearchStatus.Done -> searchResults(status.result, onAppClick, reduceMotion)
        }
    }
}

@Composable
private fun SearchButton(enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .height(PRIMARY_BUTTON_HEIGHT)
            .scaleBy(scale),
    ) {
        Text(stringResource(R.string.search), style = MaterialTheme.typography.titleMedium)
    }
}

private fun LazyListScope.searchResults(
    result: PermissionResult,
    onAppClick: (AppInfo) -> Unit,
    reduceMotion: Boolean,
) {
    if (result.unknownLabels.isNotEmpty()) {
        item {
            MessageCard(
                text = stringResource(R.string.not_recognised, result.unknownLabels.joinToString(", ")),
                isError = true,
            )
        }
    }
    item {
        val text = if (result.apps.isEmpty()) {
            stringResource(R.string.no_apps_found)
        } else {
            stringResource(R.string.apps_found, result.apps.size)
        }
        Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    }
    items(result.apps, key = { it.packageName }) { app ->
        AppRow(
            app = app,
            onClick = { onAppClick(app) },
            modifier = if (reduceMotion) Modifier else Modifier.animateItem(),
        )
    }
}

/**
 * "Show something as soon as possible" (loading.md): rows shaped like the results pulse while
 * the apps are read, with the text kept for screen readers. With reduce motion they stay still.
 */
private fun LazyListScope.loadingPlaceholders() {
    item {
        Text(
            text = stringResource(R.string.searching),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    items(PLACEHOLDER_ROWS) { PlaceholderRow() }
}

@Composable
private fun PlaceholderRow() {
    val pulse by rememberInfiniteTransition(label = "placeholder").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "pulse",
    )
    val alpha = if (LocalEffectSettings.current.reduceMotion) 0.7f else pulse
    val block = MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        colors = CardDefaults.cardColors(containerColor = Surfaces.card),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(block, CircleShape),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaceholderLine(width = 140.dp, height = 16.dp)
                PlaceholderLine(width = 200.dp, height = 12.dp)
                PlaceholderLine(width = 110.dp, height = 12.dp)
            }
        }
    }
}

@Composable
private fun PlaceholderLine(width: Dp, height: Dp) {
    Box(
        Modifier
            .width(width)
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
    )
}

/** One app: icon, name, package, "System" tag, matched names, and granted/requested state. */
@Composable
private fun AppRow(app: AppInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scaleBy(scale),
        colors = CardDefaults.cardColors(containerColor = Surfaces.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClickLabel = stringResource(R.string.open_app_info),
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(app.icon)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (app.isSystemApp) {
                        Box(Modifier.padding(start = 8.dp)) {
                            Tag(
                                text = stringResource(R.string.system_tag),
                                background = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.matched, app.matchedLabels.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (app.matchedPermissions.isNotEmpty()) {
                    Text(
                        text = permissionStates(app.matchedPermissions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** "CAMERA: granted, READ_SMS: requested only" */
@Composable
private fun permissionStates(permissions: List<MatchedPermission>): String =
    // map, not joinToString's lambda: map is inline, so stringResource may be called inside it.
    permissions.map { permission ->
        val shortName = permission.name.substringAfterLast('.')
        stringResource(
            if (permission.isGranted) R.string.permission_granted else R.string.permission_requested,
            shortName,
        )
    }.joinToString(", ")

@Composable
private fun AppIcon(icon: Drawable?) {
    // Turn the Drawable into a bitmap once per icon, not on every recomposition.
    val bitmap = remember(icon) {
        icon?.let { runCatching { it.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap() }.getOrNull() }
    }
    val iconModifier = Modifier.size(40.dp)
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = iconModifier)
    } else {
        Box(iconModifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
    }
}

@Composable
private fun MessageCard(text: String, isError: Boolean = false) {
    val colors = if (isError) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        CardDefaults.cardColors(containerColor = Surfaces.card)
    }
    Card(Modifier.fillMaxWidth(), colors = colors) {
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
    }
}
