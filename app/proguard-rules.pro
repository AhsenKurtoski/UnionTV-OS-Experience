# The UI is built programmatically, so no custom views need XML inflation rules.
# Keep the launcher entry point and anything the system instantiates by name.
-keep class com.ahsenkurtoski.uniontvos.MainActivity { *; }

# Keep line numbers so crash reports from the device stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
