package com.centiox.verdant.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.centiox.verdant.*
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.*
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopBudgetStorage(private val path: Path) : BudgetStorage {
    override suspend fun read(): String? =
        withContext(Dispatchers.IO) { if (Files.exists(path)) Files.readString(path) else null }

    override suspend fun write(json: String) =
        withContext(Dispatchers.IO) {
            Files.createDirectories(path.parent)
            val temporary = path.resolveSibling("${path.fileName}.tmp")
            Files.writeString(temporary, json)
            try {
                Files.move(
                    temporary,
                    path,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
            Unit
        }
}

fun main() = application {
    val repository = remember {
        val dataDir =
            System.getenv("APPDATA")?.let { Path.of(it, "VerdantBudget") }
                ?: Path.of(System.getProperty("user.home"), ".local", "share", "verdant-budget")
        BudgetRepository(DesktopBudgetStorage(dataDir.resolve("budget.json")))
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = "Verdant Budget",
        state = rememberWindowState(width = 430.dp, height = 930.dp),
    ) {
        App(
            repository,
            onExport = { json ->
                SwingUtilities.invokeLater {
                    val dialog = FileDialog(null as Frame?, "Save Verdant backup", FileDialog.SAVE)
                    dialog.file = "verdant-${today()}.json"
                    dialog.isVisible = true
                    if (dialog.file != null)
                        try {
                            Files.writeString(Path.of(dialog.directory, dialog.file), json)
                        } catch (e: Exception) {
                            JOptionPane.showMessageDialog(
                                null,
                                e.message,
                                "Export failed",
                                JOptionPane.ERROR_MESSAGE,
                            )
                        }
                    dialog.dispose()
                }
            },
            onImport = { callback ->
                SwingUtilities.invokeLater {
                    val dialog =
                        FileDialog(null as Frame?, "Restore Verdant backup", FileDialog.LOAD)
                    dialog.isVisible = true
                    if (dialog.file != null)
                        try {
                            callback(Files.readString(Path.of(dialog.directory, dialog.file)))
                        } catch (e: Exception) {
                            JOptionPane.showMessageDialog(
                                null,
                                e.message,
                                "Restore failed",
                                JOptionPane.ERROR_MESSAGE,
                            )
                        }
                    else callback(null)
                    dialog.dispose()
                }
            },
        )
    }
}
