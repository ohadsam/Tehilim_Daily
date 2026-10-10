# Room entities are also (de)serialised into backup files; keep their shape.
-keep class com.ohadsam.tehilim.data.db.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature
# Glance widget receivers / callbacks are instantiated by the framework.
-keep class com.ohadsam.tehilim.widget.** { *; }
-keep class com.ohadsam.tehilim.reminders.*Receiver { *; }
