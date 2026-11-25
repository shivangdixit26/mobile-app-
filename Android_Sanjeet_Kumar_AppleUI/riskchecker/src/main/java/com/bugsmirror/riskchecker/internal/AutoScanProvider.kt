package com.bugsmirror.riskchecker.internal

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.bugsmirror.riskchecker.RiskCheckerAutoScan

/**
 * Bonus: starts [RiskCheckerAutoScan] when the host app starts.
 *
 * Android creates every ContentProvider listed in the (merged) manifest before
 * Application.onCreate(), so declaring this one in the library's manifest runs our code
 * at start-up without the host app calling anything. It stores no data, so the
 * ContentProvider methods below do nothing.
 *
 * Public only because Android must be able to create it. Not part of the library's API.
 */
class AutoScanProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false
        try {
            // scan() returns at once; the checks run on a background thread.
            RiskCheckerAutoScan.start(context)
        } catch (t: Throwable) {
            Log.w("RiskChecker", "Start-up scan could not start", t)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
