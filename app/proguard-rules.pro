# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#    http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Custom Rules for Firestore Deserialization ---

# Keep the no-argument constructor and fields for data models used by Firestore.
# This is crucial for Firestore's toObject() method to work correctly,
# especially in release builds where ProGuard/R8 might strip them.

-keepclassmembers class com.aquaa.edusoul.models.Student {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.User {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.ClassSession {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.Subject {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.Batch {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.Exam {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.Homework {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.Announcement {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.FeeStructure {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.LearningResource {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.Message {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.RecurringClassSession {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.Result {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.StudentBatchLink {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.SyllabusProgress {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.TeacherSubjectBatchLink {
    <init>();
    <fields>;
}

-keepclassmembers class com.aquaa.edusoul.models.FeePayment {
    <init>();
    <fields>;
}

# --- End Custom Rules ---