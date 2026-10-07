# Keep everything - don't touch any class
-keep class ** { *; }

# Don't optimize
-dontoptimize

# Don't shrink
-dontshrink

# Don't obfuscate
-dontobfuscate

# Don't preverify
-dontpreverify

# Ignore all warnings
-dontwarn **
-ignorewarnings

# Keep all attributes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod