# Drools IntelliJ Plugin (Starter)

Package: `com.gravity.drools`  
Target: IntelliJ build line **252.\*** (your current IC-252.26830.84)

## What you get
- Minimal plugin that recognizes `.drl` files
- Basic syntax highlighter scaffold (currently plain text)
- `sinceBuild=252` to avoid the 253+ requirement
- Uses your **local IDE** via `IDEA_HOME` to guarantee compatibility

## How to use

1. **Set IDEA_HOME** to your IntelliJ 252 installation folder.
   - Windows (PowerShell):
     ```powershell
     $env:IDEA_HOME="C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.2"
     ```
   - Linux/macOS (bash/zsh):
     ```bash
     export IDEA_HOME="/Applications/IntelliJ IDEA CE.app/Contents"   # adjust path
     ```

2. Generate Gradle wrapper and run the IDE sandbox:
   ```bash
   ./gradlew wrapper --gradle-version 8.10.2 --distribution-type all
   ./gradlew runIde
   ```

3. Build the plugin zip:
   ```bash
   ./gradlew buildPlugin
   # find zip in build/distributions/
   ```

## Next steps (to reach parity with JetBrains' Drools)
- Add a real lexer (JFlex) and parser (Grammar-Kit/PSI)
- Register `lang.parserDefinition` in `plugin.xml`
- Add annotators, inspections, completion, and tests
- Port features from: https://github.com/JetBrains/intellij-plugins/tree/master/drools

Tip: Keep Kotlin/Java toolchain at **17** to match the platform.
