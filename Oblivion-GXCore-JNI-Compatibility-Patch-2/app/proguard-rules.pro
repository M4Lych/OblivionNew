-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,SourceFile,LineNumberTable

# JNI entry points and objects referenced by the Minecraft runtime bridge.
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}

-keep,includedescriptorclasses class com.oblivion.client.core.minecraft.MinecraftActivity { *; }
-keep,includedescriptorclasses class com.oblivion.client.util.NativeBridgeHelper { *; }
# libgxcore.so is a prebuilt LeviLauncher binary; preserve its original JNI owner name.
-keep,includedescriptorclasses class org.levimc.launcher.util.NativeBridgeHelper { *; }
-keep,includedescriptorclasses class com.oblivion.client.preloader.** { *; }
-keep,includedescriptorclasses class com.oblivion.client.core.mods.Mod { *; }
-keep,includedescriptorclasses class com.oblivion.client.core.mods.ModManager { *; }
-keep,includedescriptorclasses class com.oblivion.client.core.mods.inbuilt.ExternalModBridge { *; }
-keep,includedescriptorclasses class com.oblivion.client.core.mods.inbuilt.ExternalModBridge$* { *; }
-keep,includedescriptorclasses class com.oblivion.client.core.mods.inbuilt.nativemod.** { *; }
-keep,includedescriptorclasses class com.oblivion.client.core.content.nbt.NbtTag { *; }
-keep,includedescriptorclasses class com.oblivion.client.core.content.nbt.NbtDocument { *; }
-keep,includedescriptorclasses class com.oblivion.client.core.content.nbt.NativeNbt { *; }
-keep,includedescriptorclasses class com.oblivion.client.core.content.leveldb.WorldDatabase { *; }
-keep class com.oblivion.client.R$drawable { *; }
-keep class com.oblivion.client.** implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep fields serialized by Gson and version/profile metadata readers.
-keep class com.oblivion.client.settings.FeatureSettings {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.core.auth.MsftAccountStore$MsftAccount {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.core.versions.VersionProfileMetadata {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.util.InstanceBackupManager$BackupManifest {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.core.modcatalog.ModCatalog {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.core.modcatalog.ModCatalog$* {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.core.play.PlayAccountStore$Account {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.core.play.GameRelease {
    !static !transient <fields>;
    <init>();
}
-keep class com.oblivion.client.core.play.GameDownloads$Downloaded {
    !static !transient <fields>;
    <init>();
}

# Lombok-generated marker annotation used by Lenni0451 Commons.
-dontwarn lombok.Generated
