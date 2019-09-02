##
#
#  Our custom Proguard rules.
#  For our own code, it's usually simplest to add the @Keep annotation instead.
#
##

## Dynamically resolved classes
-keep class android.support.v7.widget.SearchView {
    *;
}
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl

-dontobfuscate
-dontwarn com.roughike.bottombar.VerticalScrollingBehavior$ScrollDirection

## Code checking annotations and stuff that isn't needed at runtime.
-dontwarn javax.annotation.**
-dontwarn java.lang.ClassValue
-dontwarn sun.misc.Unsafe
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn org.checkerframework.**
-dontwarn afu.org.checkerframework.**

-dontnote com.roughike.bottombar.**


-dontwarn okhttp3.**
-dontwarn okio.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
