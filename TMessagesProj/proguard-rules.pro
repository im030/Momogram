-keep public class com.google.android.gms.* { public *; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keep @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep *; }

-keep class org.webrtc.* { *; }
-keep class org.webrtc.audio.* { *; }
-keep class org.webrtc.voiceengine.* { *; }
# -keep class org.telegram.messenger.* { *; }
-keep class org.telegram.messenger.AnimatedFileDrawableStream { *; }
-keep class org.telegram.messenger.camera.* { *; }
-keep class org.telegram.messenger.secretmedia.* { *; }
-keep class org.telegram.messenger.support.* { *; }
-keep class org.telegram.messenger.time.* { *; }
-keep class org.telegram.messenger.video.* { *; }
-keep class org.telegram.messenger.voip.* { *; }
-keep class org.telegram.SQLite.** { *; }
-keep class org.telegram.ui.** { *; }
-keep class org.telegram.tgnet.ConnectionsManager { *; }
-keep class org.telegram.tgnet.NativeByteBuffer { *; }
-keep class org.telegram.tgnet.RequestTimeDelegate { *; }
-keep class org.telegram.tgnet.RequestDelegate { *; }

# 030: removed at 10.9.1
#-keep class org.telegram.tgnet.QuickAckDelegate { *; }
#-keep class org.telegram.tgnet.WriteToSocketDelegate { *; }

-keep class com.v2ray.ang.dto.** { *; }
-keep class org.openintents.openpgp.** { *; }

# -keep class io.github.trojan_gfw.** { *; }

-dontwarn com.coremedia.**
-dontwarn org.telegram.**
-dontwarn com.google.android.exoplayer2.ext.**
-dontwarn com.google.android.exoplayer2.util.**
-dontwarn com.google.android.exoplayer2.metadata.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.common.cache.**
-dontwarn com.google.common.primitives.**
-dontwarn com.googlecode.mp4parser.**

-keep class com.google.android.datatransport.** { *; }
-keep class com.google.android.exoplayer2.ext.** { *; }
-keep class com.google.android.exoplayer2.metadata.flac.PictureFrame { *; }
-keep class com.google.android.exoplayer2.decoder.SimpleDecoderOutputBuffer { *; }
-keep class org.telegram.ui.Stories.recorder.FfmpegAudioWaveformLoader { *; }
-keep class androidx.mediarouter.app.MediaRouteButton { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class org.dizitart.no2.**  { *; }
-keep class org.slf4j.** { *; }
-keep class org.h2.** { *; }
-keep class org.objenesis.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keepattributes *Annotation*

-keepnames class * implements java.io.Serializable
#-keep class * implements org.dizitart.no2.mapper.Mappable { *; }

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class moe.hx030.** { *; }

#-keep class java.beans.Transient { *; }
#-keep class java.lang.management.** { *; }
#-keep class sun.net.spi.nameservice.** { *; }

-dontwarn java.beans.Transient
-dontwarn java.lang.management.GarbageCollectorMXBean
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.OperatingSystemMXBean
-dontwarn lombok.Generated
-dontwarn sun.net.spi.nameservice.NameServiceDescriptor
# dnsjava registers this service on Java 18+; java.net.spi.* does not exist on Android
-dontwarn org.xbill.DNS.spi.DnsjavaInetAddressResolverProvider

## https://developers.google.com/ml-kit/known-issues#android_issues
#-keep class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni { *; }

# Huawei Services
-keep class com.huawei.hianalytics.**{ *; }
-keep class com.huawei.updatesdk.**{ *; }
-keep class com.huawei.hms.**{ *; }

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**

-keep class io.nano.tex.** {*;}

# JLatexMath: macro/atom classes are loaded reflectively by Class.forName
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**

# Use -keep to explicitly keep any other classes shrinking would remove
#-dontoptimize
#-dontobfuscate

# MapLibre
-dontwarn org.maplibre.**

-keepnames class androidx.recyclerview.widget.RecyclerView
-keepclassmembers class androidx.recyclerview.widget.RecyclerView {
    public void suppressLayout(boolean);
    public boolean isLayoutSuppressed();
}

-keepattributes SourceFile,LineNumberTable
-keep class org.telegram.tgnet.** { *; }
