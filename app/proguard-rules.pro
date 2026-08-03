# ReteKey IME — R8 rules.
#
# AGP auto-generates keep rules for components declared in the manifest (the IME service and the
# activities), so R8 keeps them and their reachable graph. These explicit keeps are defensive: the
# input method is instantiated by the framework by name, and its public callback surface must stay.

-keep class com.retekey.ReteKeyImeService { *; }
-keep class com.retekey.SettingsActivity { *; }
-keep class com.retekey.PreviewActivity { *; }

# Keep framework-invoked View and Activity constructors in general.
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
