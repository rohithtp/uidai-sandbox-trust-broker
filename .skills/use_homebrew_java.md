# Skill: Use Homebrew OpenJDK

## Context
On this system, the default `java` command in `/usr/bin/java` is a macOS shim that may not point to a valid runtime. A functional OpenJDK 25 installation is available via Homebrew.

## Instructions
Whenever you need to run Java, Maven, or any JVM-based tool, you MUST ensure that `JAVA_HOME` is set to the Homebrew OpenJDK path.

### Environment Variable
- **JAVA_HOME**: `/opt/homebrew/opt/openjdk@25`

### Usage Example
When running Maven commands, prefix them with the export:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@25 && ./mvnw <command>
```

### Verification
You can verify the setup by running:
```bash
$JAVA_HOME/bin/java -version
```
