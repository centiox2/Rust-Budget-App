# Kotlin serialization generates its own consumer rules.

# AndroidJUnitRunner invokes this shared API from the separate instrumentation APK.
# Preserve its entry points while allowing the rest of the app to be optimized.
-keep class androidx.tracing.Trace { public static *; }
