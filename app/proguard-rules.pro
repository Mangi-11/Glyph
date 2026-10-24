# YukiHook's libxposed runtime supplies the generated entry's consumer keep rules.

# Debug diagnostics are useful during development but must not enter Release artifacts.
-assumenosideeffects class io.github.mangi.glyph.diagnostics.Diagnostics {
    public final void debug(...);
}
