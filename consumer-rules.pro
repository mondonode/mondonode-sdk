# ---------------------------------------------------------------------------
# MondoNode SDK — consumer ProGuard / R8 rules
#
# These rules are automatically merged into every APK that depends on the SDK
# (via the consumerProguardFiles directive in the library's build.gradle.kts).
# Plugin authors do NOT need to copy these rules into their own proguard-rules.pro.
# ---------------------------------------------------------------------------

# AIDL-generated Stub and Proxy inner classes.
# Transaction codes are baked in at compile time on both sides of each IPC call.
# Renaming or removing these classes would silently break all Binder communication.
-keep class com.systemhalted.mondonode.sdk.**$Stub       { *; }
-keep class com.systemhalted.mondonode.sdk.**$Stub$Proxy { *; }

# AIDL interfaces must remain intact so the host can cast IBinder.queryLocalInterface()
# results to the correct interface type.
-keep interface com.systemhalted.mondonode.sdk.** { *; }

# kotlinx.serialization — generated $$serializer companion objects and serializer()
# factory methods for every @Serializable SDK type. The serialization plugin
# reaches these via descriptors at runtime; R8 cannot see the usage statically.
-keepclassmembers class com.systemhalted.mondonode.sdk.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.systemhalted.mondonode.sdk.**$$serializer { *; }
