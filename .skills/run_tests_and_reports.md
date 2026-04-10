# Skill: Run JUnit Tests and Generate Surefire Reports

## Context
This project is a multi-module Maven project. Testing and reporting require a specific Java version and certain Maven goals to ensure both machine-readable (XML) and human-readable (HTML) reports are correctly generated.

## Prerequisites
- **Java Version**: Ensure `JAVA_HOME` is set to the Homebrew OpenJDK 25 path.
- **Reference**: See [.skills/use_homebrew_java.md](file:///Users/rohithtp/mine/home/workspaces/uidai/uidai-sandbox-trust-broker/.skills/use_homebrew_java.md) for environment details.

## Instructions

### 1. Run Tests (XML/TXT Reports)
To execute all JUnit tests and generate the standard Surefire XML/TXT reports (required for CI/CD integrations):
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home && export PATH=$JAVA_HOME/bin:$PATH && ./mvnw clean test
```
*   **Output Location**: `[module-name]/target/surefire-reports/`
*   **Files**: `TEST-*.xml`, `*.txt`

### 2. Generate HTML reports
To generate the human-readable HTML Surefire reports for a visual summary of test results:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home && export PATH=$JAVA_HOME/bin:$PATH && ./mvnw surefire-report:report
```
*   **Output Location**: `[module-name]/target/reports/surefire.html`
*   **Note**: This goal will use the existing test results or run tests if they haven't been run yet.

### 3. Full Cycle (Clean, Test, Report)
To perform a complete clean build, run all tests, and generate both types of reports in a single command:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home && export PATH=$JAVA_HOME/bin:$PATH && ./mvnw clean test surefire-report:report
```

## Verification
After execution, verify that the reports exist:
- **XML**: `ls interoperability-gateway-service/target/surefire-reports/`
- **HTML**: `ls interoperability-gateway-service/target/reports/surefire.html`
