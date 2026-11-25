# R8 rules that are applied to any app that uses this library and turns on minify.
#
# The library uses no reflection, so R8 may shrink and rename its code freely.
# The only class that must keep its name is the one Android creates from the
# manifest by its class name, without any code referring to it.
-keep class com.bugsmirror.riskchecker.internal.AutoScanProvider {
    public <init>();
}
