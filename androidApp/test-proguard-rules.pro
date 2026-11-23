# Error Prone annotations in Espresso reference the JDK compiler model.
# That compile-time-only type is absent from Android and never used at runtime.
-dontwarn javax.lang.model.element.Modifier
