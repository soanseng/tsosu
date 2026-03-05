# Tsosu ProGuard rules

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.tsosu.**$$serializer { *; }
-keepclassmembers class app.tsosu.** {
    *** Companion;
}
-keepclasseswithmembers class app.tsosu.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Google API Client
-keep class com.google.api.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**
-dontwarn com.google.api.client.http.apache.v2.**
-dontwarn com.google.common.base.**
-dontwarn com.google.common.collect.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.apache.http.**
-dontwarn android.net.http.**

# Google Calendar API models
-keep class com.google.api.services.calendar.model.** { *; }

# Hilt
-dontwarn dagger.hilt.android.internal.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# kotlinx-datetime
-keep class kotlinx.datetime.** { *; }

# Credential Manager
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**
