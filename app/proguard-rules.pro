# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep MediaPipe GenAI classes
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }

# Keep Room entities
-keep class com.expense.tracker.data.local.db.entities.** { *; }
-keep class com.expense.tracker.domain.model.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep LocalAIService
-keep class com.expense.tracker.data.local.ai.** { *; }

# Suppress warnings for annotation processors (not needed at runtime)
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**
-dontwarn com.google.auto.value.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.mediapipe.**
-dontwarn org.tensorflow.**

# Missing classes that are annotation processor only
-dontwarn javax.annotation.processing.AbstractProcessor
-dontwarn javax.annotation.processing.SupportedAnnotationTypes
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.**
-dontwarn javax.lang.model.type.**
-dontwarn javax.lang.model.util.**
