-keep,includedescriptorclasses class com.oblivion.client.controls.** { *; }

# The controls repository persists these models by field name.
-keep class com.oblivion.client.controls.CustomControls {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.controls.ControlData {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.controls.ControlDrawerData {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.controls.ControlJoystickData {
    !static !transient <fields>;
    <init>();
}
-keep enum com.oblivion.client.controls.ControlDrawerData$Orientation { *; }
