# kotlinx.serialization و Compose قوانین مصرف‌کننده (consumer rules) خودشان را دارند.
# مدل‌های ذخیره‌سازی را برای اطمینان نگه می‌داریم:
-keep,includedescriptorclasses class com.flow.focus.data.**$$serializer { *; }
-keepclassmembers class com.flow.focus.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.flow.focus.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
