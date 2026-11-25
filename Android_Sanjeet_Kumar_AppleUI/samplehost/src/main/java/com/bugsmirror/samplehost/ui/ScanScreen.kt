package com.bugsmirror.samplehost.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bugsmirror.riskchecker.Finding
import com.bugsmirror.riskchecker.RiskReport
import com.bugsmirror.samplehost.R
import com.bugsmirror.samplehost.ScanState
import com.bugsmirror.samplehost.ui.theme.LocalEffectSettings
import com.bugsmirror.samplehost.ui.theme.RiskColors
import com.bugsmirror.samplehost.ui.theme.Surfaces
import org.json.JSONObject
import java.text.DateFormat
import java.util.Date

private val SCREEN_MARGIN = 16.dp
private val PRIMARY_BUTTON_HEIGHT = 52.dp
private val SCORE_RING_SIZE = 128.dp

/** "Scan device" tab: the scan button, the score, and the findings coloured by severity. */
@Composable
fun ScanScreen(
    state: ScanState,
    onScanClick: () -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var showJson by rememberSaveable { mutableStateOf(false) }
    val reduceMotion = LocalEffectSettings.current.reduceMotion

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = screenPadding(contentPadding, SCREEN_MARGIN),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScanButton(isScanning = state.isScanning, onClick = onScanClick) }

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
                // Stable keys let a rescan animate only what changed: a fixed finding fades out
                // and the rest slide up, instead of the whole list being replaced.
                items(report.findings.withStableKeys(), key = { it.first }) { (_, finding) ->
                    FindingCard(finding, if (reduceMotion) Modifier else Modifier.animateItem())
                }
            }
            item { JsonSection(report, showJson, onToggle = { showJson = !showJson }) }
        }
    }
}

/**
 * Unique, stable key for each finding: its id plus how many times that id came before.
 * A report has at most one finding per check, but CHECK_FAILED can appear several times.
 */
private fun List<Finding>.withStableKeys(): List<Pair<String, Finding>> {
    val seen = mutableMapOf<String, Int>()
    return map { finding ->
        val count = seen.getOrDefault(finding.id, 0)
        seen[finding.id] = count + 1
        "${finding.id}#$count" to finding
    }
}

/** The main action: full width, 52dp tall (easy to hit), shrinks slightly while pressed. */
@Composable
private fun ScanButton(isScanning: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    Button(
        onClick = onClick,
        enabled = !isScanning,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .height(PRIMARY_BUTTON_HEIGHT)
            .scaleBy(scale),
    ) {
        Text(
            text = stringResource(if (isScanning) R.string.scanning else R.string.scan_device),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * The screen's signature element: a ring that fills to the score. When a rescan changes the score,
 * the ring and the number move to the new value, so the change is easy to see. On first display
 * they simply appear, so switching tabs does not replay the animation.
 */
@Composable
private fun ScoreCard(report: RiskReport, isAutoScan: Boolean) {
    // Two shades: the tag carries white text on the base colour; the ring and number sit on the card.
    val tagColor = RiskColors.forScore(report.score)
    val color = RiskColors.forScoreOnSurface(report.score)
    val shownScore by animateIntAsState(
        targetValue = report.score,
        animationSpec = motionSpec(tween(durationMillis = 700, easing = FastOutSlowInEasing)),
        label = "score",
    )
    val ringProgress by animateFloatAsState(
        targetValue = report.score / 100f,
        animationSpec = motionSpec(tween(durationMillis = 700, easing = FastOutSlowInEasing)),
        label = "ring",
    )
    val riskText = stringResource(riskLabel(report.score))
    val time = remember(report.scannedAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(report.scannedAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surfaces.card),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Screen readers hear one sentence, not "65", "of 100" and a progress bar separately.
            val description = stringResource(R.string.score_a11y, report.score, riskText)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(SCORE_RING_SIZE)
                    .clearAndSetSemantics { contentDescription = description },
            ) {
                CircularProgressIndicator(
                    progress = { ringProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    strokeWidth = 12.dp,
                    trackColor = color.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = shownScore.toString(),
                        // Tabular figures: every digit is the same width, so the number doesn't wobble while it counts.
                        style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                    Text(
                        text = stringResource(R.string.score_of_100),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.score_label), style = MaterialTheme.typography.titleMedium)
                // The risk is also said in words, so colour is never the only signal.
                Tag(text = riskText, background = tagColor)
                Text(
                    text = stringResource(R.string.findings_count, report.findings.size),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(if (isAutoScan) R.string.scanned_at_startup else R.string.scanned_at, time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@StringRes
private fun riskLabel(score: Int): Int = when {
    score >= 80 -> R.string.risk_low
    score >= 50 -> R.string.risk_some
    else -> R.string.risk_high
}

/** One finding: a coloured bar and tag for its severity, then its title and details. */
@Composable
private fun FindingCard(finding: Finding, modifier: Modifier = Modifier) {
    val color = RiskColors.forSeverity(finding.severity)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f).compositeOver(Surfaces.card),
        ),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(color),
            )
            Column(Modifier.padding(14.dp)) {
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
                Spacer(Modifier.height(6.dp))
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
    val reduceMotion = LocalEffectSettings.current.reduceMotion
    // The panel grows and shrinks smoothly instead of jumping.
    Column(if (reduceMotion) Modifier else Modifier.animateContentSize()) {
        TextButton(onClick = onToggle) {
            Text(stringResource(if (expanded) R.string.hide_json else R.string.show_json))
        }
        if (expanded) {
            val json = remember(report) { JSONObject(report.toJson()).toString(2) }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
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

/** A small coloured label with white text, used for severities, the risk level and the "System" tag. */
@Composable
internal fun Tag(text: String, background: Color, contentColor: Color = Color.White) {
    Surface(color = background, contentColor = contentColor, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
