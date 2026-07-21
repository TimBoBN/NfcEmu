# HostApduService implementations are instantiated by the system via reflection
# based on the class name declared in AndroidManifest.xml / apduservice.xml.
-keep class com.nfcemu.hce.NfcEmuHostApduService { *; }

# kotlinx.serialization: keep serializable model classes and their generated
# serializers (the compiler plugin generates code R8 must not strip/rename).
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class com.nfcemu.data.model.**$$serializer { *; }
-keepclassmembers class com.nfcemu.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.nfcemu.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
