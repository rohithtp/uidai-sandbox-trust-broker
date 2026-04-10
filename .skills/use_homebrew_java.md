# Skill: Use Homebrew OpenJDK

## Context
On this system, the default `java` command in `/usr/bin/java` is a macOS shim that may not point to a valid runtime. A functional OpenJDK 25 (or latest) installation is available via Homebrew at `/opt/homebrew/opt/openjdk`.

## Instructions
Whenever you need to run Java, Maven (using `./mvnw`), or any JVM-based tool, you MUST ensure that `JAVA_HOME` is set to the Homebrew OpenJDK path.

### Environment variables
For **Silicon Mac (M1/M2/M3)**:
- **JAVA_HOME**: `/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home`

For **Intel Mac**:
- **JAVA_HOME**: `/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home`

### Usage Example
When running Maven commands, prefix them with the environment setup:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home && export PATH=$JAVA_HOME/bin:$PATH && ./mvnw <command>
```

### Verification
You can verify the setup by running:
```bash
$JAVA_HOME/bin/java -version
```

### Troubleshooting
If `JAVA_HOME` is not recognized, verify the path exists:
```bash
ls -l /opt/homebrew/opt/openjdk/bin/java
```
