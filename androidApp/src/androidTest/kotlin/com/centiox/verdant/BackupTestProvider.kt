package com.centiox.verdant

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File

/** Only packaged in the instrumentation APK, never in the distributed application. */
class BackupTestProvider : ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: Uri) = "application/json"

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        require(uri.lastPathSegment == "backup.json")
        val file = File(context!!.cacheDir, "test-backup.json")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor =
        MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply {
            addRow(
                arrayOf<Any>("backup.json", File(context!!.cacheDir, "test-backup.json").length())
            )
        }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) =
        if (File(context!!.cacheDir, "test-backup.json").delete()) 1 else 0
}
