# Privacy and local data

Verdant Budget is an offline personal budget tracker. It does not require an account and contains no advertising, analytics, bank connections, or application servers. The Android application does not request internet permission.

The app stores the name, currency, transactions, and budgets you enter. On Android, the ledger is encrypted with AES-256-GCM, using a non-exportable key managed by Android Keystore. Writes use AtomicFile so an interrupted save can recover the previous ledger. The desktop JVM host stores JSON in the operating system user's application-data directory; desktop storage is not encrypted by this implementation.

Backup files are readable JSON. You choose the destination or document provider. A provider you choose may have its own synchronization and privacy behavior. Keep financial backups somewhere private. Restore validates the file and asks before replacing the current ledger.

Reset clears the app's active ledger and preferences. Exported backup files are outside the app's private storage and are not deleted by reset or uninstall. Uninstalling removes the app's private Android data and encryption key. Restore an exported backup after reinstalling if needed.

The app does not collect diagnostics or send financial records to GitHub. Automated tests use generated sample data. Source and release artifacts are hosted on GitHub; manually submitted support issues are separate from data stored inside the app.
