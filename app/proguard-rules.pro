# ===== XZD AI Chat ProGuard 规则（高强度混淆 + 反逆向） =====

# --- 混淆字典（I/l/1/O/0 难分辨字符） ---
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt

# --- 彻底删除源文件名、行号、局部变量和 Kotlin @Metadata（逆向者靠 Metadata 恢复 .kt 文件名） ---
-renamesourcefileattribute ""
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable,!RuntimeVisibleAnnotations,!RuntimeVisibleParameterAnnotations,RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations,AnnotationDefault,EnclosingMethod,Signature,InnerClasses

# --- 激进混淆 ---
-repackageclasses ''
-useuniqueclassmembernames
-allowaccessmodification
-overloadaggressively
-mergeinterfacesaggressively

# --- 优化 ---
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# --- 保留必要入口 ---
-keep class com.xzd1314.aichat.MainActivity { *; }
-keep class com.xzd1314.aichat.BuildConfig { *; }
-keep class **.R$* { *; }
-keep class **.R { *; }

# --- Kotlin 协程 ---
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# --- Compose（AGP 自动处理，兜底） ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- Shizuku（反射调用，保留） ---
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# --- 数据类（JSON 序列化需要构造） ---
-keepclassmembers class com.xzd1314.aichat.data.** {
    <init>(...);
    public static *** fromJson(...);
}

# --- 移除 release 日志（全级别） ---
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
    public static *** isLoggable(...);
    public static *** getStackTraceString(...);
}

# --- 移除 System.out/err 打印 ---
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}
