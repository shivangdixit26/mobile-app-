package com.bugsmirror.samplehost.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugsmirror.samplehost.MainViewModel
import com.bugsmirror.samplehost.R
import kotlinx.coroutines.launch

private const val TAB_SCAN = 0
private const val TAB_APPS = 1

/** The whole app: a top bar and two tabs, "Scan device" and "Apps by permission". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskCheckerApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_SCAN) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openFailedMessage = stringResource(R.string.open_app_info_failed)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == TAB_SCAN,
                    onClick = { selectedTab = TAB_SCAN },
                    text = { Text(stringResource(R.string.tab_scan)) },
                )
                Tab(
                    selected = selectedTab == TAB_APPS,
                    onClick = { selectedTab = TAB_APPS },
                    text = { Text(stringResource(R.string.tab_apps)) },
                )
            }
            when (selectedTab) {
                TAB_SCAN -> ScanScreen(
                    state = state.scan,
                    onScanClick = viewModel::scan,
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
                )
            }
        }
    }
}
