package com.bugsmirror.samplehost.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bugsmirror.riskchecker.Finding
import com.bugsmirror.riskchecker.RiskReport
import com.bugsmirror.samplehost.R
import com.bugsmirror.samplehost.ScanState
import com.bugsmirror.samplehost.ui.theme.RiskColors
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

/** "Scan device" tab: the scan button, the score, and the findings coloured by severity. */
@Composable
fun ScanScreen(
    state: ScanState,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showJson by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Button(
                onClick = onScanClick,
                enabled = !state.isScanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (state.isScanning) R.string.scanning else R.string.scan_device))
            }
        }

        if (state.isScanning) {
            item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        }

        val report = state.report
        if (report == null) {
            if (!state.isScanning) {
                item {
                    Text(
                        text = stringResource(R.string.scan_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item { ScoreCard(report, state.isAutoScan) }
            if (report.findings.isEmpty()) {
                item { Text(stringResource(R.string.no_findings), style = MaterialTheme.typography.bodyLarge) }
            } else {
                items(report.findings) { finding -> FindingCard(finding) }
            }
            item { JsonSection(report, showJson, onToggle = { showJson = !showJson }) }
        }
    }
}

@Composable
private fun ScoreCard(report: RiskReport, isAutoScan: Boolean) {
    val color = RiskColors.forScore(report.score)
    val time = remember(report.scannedAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(report.scannedAt))
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.score_label), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = report.score.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    text = stringResource(R.string.score_out_of),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }
            LinearProgressIndicator(
                progress = { report.score / 100f },
                color = color,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(if (isAutoScan) R.string.scanned_at_startup else R.string.scanned_at, time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.findings_count, report.findings.size),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** One finding: a coloured bar and badge for its severity, then its title and details. */
@Composable
private fun FindingCard(finding: Finding) {
    val color = RiskColors.forSeverity(finding.severity)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.10f).compositeOver(MaterialTheme.colorScheme.surface),
        ),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(color),
            )
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(text = finding.severity, background = color)
                    Text(
                        text = finding.id,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(finding.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = finding.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Shows what RiskReport.toJson() returns, so the JSON format can be checked on the phone. */
@Composable
private fun JsonSection(report: RiskReport, expanded: Boolean, onToggle: () -> Unit) {
    Column {
        TextButton(onClick = onToggle) {
            Text(stringResource(if (expanded) R.string.hide_json else R.string.show_json))
        }
        if (expanded) {
            val json = remember(report) { JSONObject(report.toJson()).toString(2) }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SelectionContainer {
                    Text(
                        text = json,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

/** A small coloured label with white text, used for severities and the "System" tag. */
@Composable
internal fun Tag(text: String, background: Color, contentColor: Color = Color.White) {
    Surface(color = background, contentColor = contentColor, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
