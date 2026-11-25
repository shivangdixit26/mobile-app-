package com.bugsmirror.samplehost.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugsmirror.samplehost.Feedback
import com.bugsmirror.samplehost.MainViewModel
import com.bugsmirror.samplehost.R
import com.bugsmirror.samplehost.ui.theme.LocalEffectSettings
import com.bugsmirror.samplehost.ui.theme.Surfaces
import kotlinx.coroutines.launch

private const val TAB_SCAN = 0
private const val TAB_APPS = 1

/** Height of the band under the header where scrolled content fades out. */
private val EDGE_FADE_HEIGHT = 16.dp

/** The whole app: a header with two tabs, and the screen for the selected tab beneath it. */
@Composable
fun RiskCheckerApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_SCAN) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val openFailedMessage = stringResource(R.string.open_app_info_failed)

    // One scroll state per tab, kept here so each tab remembers its position and the
    // header can tell whether content is scrolled underneath it.
    val scanListState = rememberLazyListState()
    val appsListState = rememberLazyListState()
    val activeListState = if (selectedTab == TAB_SCAN) scanListState else appsListState
    val isScrolled by remember(activeListState) { derivedStateOf { activeListState.canScrollBackward } }

    // A short vibration doubles the visual feedback for the two moments that matter (playing-haptics.md).
    // Android's "Touch feedback" setting can turn these off.
    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { event ->
            val type = when (event) {
                Feedback.ScanFinished -> HapticFeedbackType.Confirm
                Feedback.SearchProblem -> HapticFeedbackType.Reject
            }
            haptics.performHapticFeedback(type)
        }
    }

    Scaffold(
        containerColor = Surfaces.page,
        topBar = {
            ScrollEdgeHeader(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                isScrolled = isScrolled,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // The screens use innerPadding as list padding, not as a margin, so their content
        // starts below the header but can scroll up underneath it.
        Crossfade(
            targetState = selectedTab,
            animationSpec = motionSpec(tween(durationMillis = 180)),
            label = "tabs",
        ) { tab ->
            when (tab) {
                TAB_SCAN -> ScanScreen(
                    state = state.scan,
                    onScanClick = viewModel::scan,
                    listState = scanListState,
                    contentPadding = innerPadding,
                )
                else -> AppSearchScreen(
                    query = state.query,
                    status = state.search,
                    onQueryChange = viewModel::onQueryChange,
                    onSearch = viewModel::search,
                    onAppClick = { app ->
                        if (!viewModel.openAppSettings(app.packageName)) {
                            scope.launch { snackbarHostState.showSnackbar(openFailedMessage) }
                        }
                    },
                    listState = appsListState,
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

/**
 * The floating "functional layer": title and tabs. It uses the scroll-edge appearance from Apple's
 * navigation bars: at rest it blends into the page; once content slides under it, a hairline edge
 * and a fade band appear so the content never collides with the tabs.
 *
 * The bar is solid on purpose. Apple's glass blurs what is underneath, but Android has no stable
 * backdrop blur, and a see-through bar without blur lets text show through the tab labels.
 * The design skill's advice for Android is the same: an opaque or lightly translucent bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScrollEdgeHeader(selectedTab: Int, onTabSelected: (Int) -> Unit, isScrolled: Boolean) {
    val highContrast = LocalEffectSettings.current.increasedContrast
    val base = Surfaces.page
    // With higher contrast the edge is always drawn, so the bar's boundary is never only implied.
    val edgeAlpha by animateFloatAsState(
        targetValue = if (isScrolled || highContrast) 1f else 0f,
        animationSpec = motionSpec(tween(durationMillis = 150)),
        label = "scrollEdge",
    )

    Column {
        Column(Modifier.background(base)) {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
            ) {
                Tab(
                    selected = selectedTab == TAB_SCAN,
                    onClick = { onTabSelected(TAB_SCAN) },
                    text = { Text(stringResource(R.string.tab_scan)) },
                )
                Tab(
                    selected = selectedTab == TAB_APPS,
                    onClick = { onTabSelected(TAB_APPS) },
                    text = { Text(stringResource(R.string.tab_apps)) },
                )
            }
            HorizontalDivider(
                modifier = Modifier.alpha(edgeAlpha),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        // Scroll edge effect: content fades out as it slides under the header. At rest no content
        // is under this band (the lists start below it), so it is invisible.
        Box(
            Modifier
                .fillMaxWidth()
                .height(EDGE_FADE_HEIGHT)
                .alpha(if (highContrast) 0f else edgeAlpha)
                .background(Brush.verticalGradient(listOf(base, base.copy(alpha = 0f)))),
        )
    }
}

/**
 * List padding for a screen: the space Scaffold reserves (header, system bars, side insets in
 * landscape) plus the screen's own [margin].
 */
@Composable
internal fun screenPadding(insets: PaddingValues, margin: Dp): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = insets.calculateStartPadding(direction) + margin,
        top = insets.calculateTopPadding() + margin,
        end = insets.calculateEndPadding(direction) + margin,
        bottom = insets.calculateBottomPadding() + margin,
    )
}
