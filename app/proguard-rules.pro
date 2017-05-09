-keepclassmembers class uk.ac.warwick.my.app.bridge.MyWarwickJavaScriptInterface {
   public *;
}

-keep class android.support.v7.widget.SearchView {
    *;
}

-dontwarn javax.annotation.**
-dontwarn java.lang.ClassValue
-dontwarn sun.misc.Unsafe
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn com.roughike.bottombar.VerticalScrollingBehavior$ScrollDirection
