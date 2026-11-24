# Privacy and local data

Verdant Budget is an offline personal budget tracker. It does not require an account and contains no advertising, analytics, bank connections, or application servers. The Android application does not request internet permission.

The app stores the profile, accounts, transactions and fees, budgets, categories, scheduled payments, goals, templates, drafts, and preferences you enter. On Android, the ledger is encrypted with AES-256-GCM, using a non-exportable key managed by Android Keystore. Writes use AtomicFile so an interrupted save can recover the previous ledger. The desktop JVM host stores JSON in the operating system user's application-data directory; desktop storage is not encrypted by this implementation.

Ordinary JSON backups and CSV exports are readable files. Protected backups use a user-chosen password (at least 12 characters), PBKDF2-HMAC-SHA256 with 600,000 iterations, a fresh random salt, and AES-256-GCM authenticated encryption. The password is not stored and cannot be recovered. Android stages exports in a separate Keystore-encrypted private file so a document-picker process restart does not expose plaintext or silently fall back to an unprotected export. You choose the destination or document provider. A provider you choose may have its own synchronization and privacy behavior. Keep financial backups somewhere private. Restore validates the file and asks before replacing the current ledger.

Reset clears the app's active ledger and preferences. Exported backup files are outside the app's private storage and are not deleted by reset or uninstall. Uninstalling removes the app's private Android data and encryption key. Restore an exported backup after reinstalling if needed.

The app does not collect diagnostics or send financial records to GitHub. Automated tests use generated sample data. Source and release artifacts are hosted on GitHub; manually submitted support issues are separate from data stored inside the app.

App lock uses Android biometric or device screen-lock authentication, re-locks when the app leaves the foreground, and blocks screenshots while enabled. A restored backup does not enable another device's lock; enable it locally after restoring. Amount privacy hides summary/list amounts, while entry editors still show the values needed for editing.

Optional local payment, budget, and backup reminders use Android notifications and WorkManager. Android controls permission and delivery timing. Notifications hide financial details when app lock or balance privacy is enabled. The home-screen widget contains only quick-entry actions and no account balances. The app does not read SMS or execute M-Pesa payments.
