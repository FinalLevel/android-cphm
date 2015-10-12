# DONT USE THIS OPTION:
# -allowaccessmodification
# Access rights important for CPHM Library

# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/tera/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep field names, because they use as column names in sqlite tables
-keepclassmembers public class * extends com.finallevel.cphm.BaseColumns {
	public !static !final <fields>;
}

# Clear loggining
# TODO: uncomment before release
#-assumenosideeffects class android.util.Log {
#    public static boolean isLoggable(java.lang.String, int);
#    public static int v(...);
#    public static int d(...);
#    public static int i(...);
#    public static int w(...);
#    public static int e(...);
#    public static int wtf(...);
#}
