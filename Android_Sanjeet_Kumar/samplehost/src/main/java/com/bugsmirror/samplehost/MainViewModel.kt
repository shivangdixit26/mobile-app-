package com.bugsmirror.samplehost

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bugsmirror.riskchecker.RiskChecker
import com.bugsmirror.riskchecker.RiskCheckerAutoScan
import com.bugsmirror.riskchecker.RiskReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Talks to the riskchecker library and keeps the screen's state. */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Given the Application, so the library never holds on to an Activity.
    private val riskChecker = RiskChecker(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /** Shows the library's start-up scan, unless the user has already started a scan. */
    private val autoScanListener: (RiskReport) -> Unit = { report ->
        _uiState.update { state ->
            if (state.scan.report == null && !state.scan.isScanning) {
                state.copy(scan = ScanState(report = report, isAutoScan = true))
            } else {
                state
            }
        }
    }

    init {
        RiskCheckerAutoScan.observe(autoScanListener)
    }

    fun scan() {
        if (_uiState.value.scan.isScanning) return
        _uiState.update { it.copy(scan = it.scan.copy(isScanning = true)) }
        // The library runs the checks on its own thread and calls back on the main thread.
        riskChecker.scan { report ->
            _uiState.update { it.copy(scan = ScanState(report = report)) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search() {
        searchJob?.cancel()
        // "Camera, sms, Camra" -> ["Camera", " sms", " Camra"]. The library trims and ignores case.
        val labels = _uiState.value.query.split(',').filter { it.isNotBlank() }
        if (labels.isEmpty()) {
            _uiState.update { it.copy(search = SearchStatus.EmptyQuery) }
            return
        }
        _uiState.update { it.copy(search = SearchStatus.Loading) }
        searchJob = viewModelScope.launch {
            // Reading every installed app is slow, so it must not run on the main thread.
            val result = withContext(Dispatchers.IO) { riskChecker.getAppsByPermissions(labels) }
            _uiState.update { it.copy(search = SearchStatus.Done(result)) }
        }
    }

    /** Returns false if the App info screen could not be opened. */
    fun openAppSettings(packageName: String): Boolean = riskChecker.openAppSettings(packageName)

    override fun onCleared() {
        RiskCheckerAutoScan.removeObserver(autoScanListener)
    }
}
