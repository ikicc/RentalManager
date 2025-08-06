# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 保留调试信息，便于崩溃分析
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 应用基本配置
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-verbose

# 保留Android核心组件
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends androidx.fragment.app.Fragment

# 保留native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留View的getter/setter方法
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留Parcelable实现类
-keepclassmembers class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator CREATOR;
  public static final ** CREATOR;
}

# 保留Serializable实现类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留资源ID引用
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保留实体类和数据模型
-keep class com.morgen.rentalmanager.myapplication.** { *; }

# Room数据库相关配置
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson相关配置
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Jetpack Compose相关配置
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin相关配置
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-dontwarn kotlin.**

# Accompanist库相关配置
-keep class com.google.accompanist.** { *; }

# Coil图片加载库相关配置
-keep class coil.** { *; }
-dontwarn coil.**