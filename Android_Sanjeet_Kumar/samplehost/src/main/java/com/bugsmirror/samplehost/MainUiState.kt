package com.bugsmirror.samplehost

import com.bugsmirror.riskchecker.PermissionResult
import com.bugsmirror.riskchecker.RiskReport

/** Everything the screen shows. Held by [MainViewModel], so it survives screen rotation. */
data class MainUiState(
    val scan: ScanState = ScanState(),
    val query: String = "",
    val search: SearchStatus = SearchStatus.Idle,
)

data class ScanState(
    val isScanning: Boolean = false,
    val report: RiskReport? = null,
    /** True when [report] came from the library's automatic start-up scan. */
    val isAutoScan: Boolean = false,
)

sealed interface SearchStatus {
    /** Nothing searched yet. */
    data object Idle : SearchStatus

    /** Search was pressed with an empty text box. */
    data object EmptyQuery : SearchStatus

    data object Loading : SearchStatus

    data class Done(val result: PermissionResult) : SearchStatus
}
