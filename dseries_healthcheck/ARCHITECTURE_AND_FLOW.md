# dSeries Health Check - Architecture and Flow

**Version:** 1.0.0  
**Date:** February 11, 2026  
**Document Type:** Technical Architecture

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Execution Flow](#execution-flow)
4. [Component Details](#component-details)
5. [Configuration System](#configuration-system)
6. [Health Check Process](#health-check-process)
7. [Reporting System](#reporting-system)
8. [Integration Points](#integration-points)

---

## 🎯 Overview

The dSeries Health Check Tool is a comprehensive system monitoring solution that performs automated health checks on ESP dSeries Workload Automation installations. It validates system resources, configuration settings, and service availability.

### Key Characteristics
- **Language:** Java 8+ (with Python alternative)
- **Execution Model:** Command-line driven
- **Configuration:** Properties-based
- **Output:** Console + File reports
- **Exit Codes:** 0 (success), 1 (failure), 2 (error)

---

## 🏗️ Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         USER INTERACTION LAYER                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    │               │               │
                    ▼               ▼               ▼
        ┌──────────────────┐ ┌──────────┐ ┌─────────────────┐
        │  Batch Script    │ │PowerShell│ │  Direct Java    │
        │ run_healthcheck  │ │  Script  │ │   Execution     │
        │   _simple.bat    │ │Run-Health│ │                 │
        │                  │ │Check.ps1 │ │                 │
        └──────────────────┘ └──────────┘ └─────────────────┘
                    │               │               │
                    └───────────────┼───────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          MAIN APPLICATION                               │
│                   DSeriesHealthCheckSimple.java                         │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                    INITIALIZATION PHASE                          │  │
│  │  1. Parse command-line arguments                                 │  │
│  │  2. Validate installation directory                              │  │
│  │  3. Load configuration from properties files                     │  │
│  │  4. Initialize counters and data structures                      │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                    HEALTH CHECK EXECUTION                        │  │
│  │                                                                   │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │  │
│  │  │  SYS-001    │  │  SYS-002    │  │  SYS-003    │            │  │
│  │  │     CPU     │→ │   Memory    │→ │    Disk     │            │  │
│  │  │ Utilization │  │    Usage    │  │    Space    │            │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘            │  │
│  │         │                 │                 │                    │  │
│  │         └─────────────────┼─────────────────┘                    │  │
│  │                           ▼                                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │  │
│  │  │  SRV-001    │  │  DB-001     │  │  SRV-003    │            │  │
│  │  │  JVM Heap   │→ │  Database   │→ │   Server    │            │  │
│  │  │    Size     │  │Connectivity │  │    Port     │            │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘            │  │
│  │         │                 │                 │                    │  │
│  │         └─────────────────┼─────────────────┘                    │  │
│  │                           ▼                                       │  │
│  │                  ┌─────────────┐                                 │  │
│  │                  │  SRV-005    │                                 │  │
│  │                  │Installation │                                 │  │
│  │                  │  Directory  │                                 │  │
│  │                  └─────────────┘                                 │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                    RESULT AGGREGATION                            │  │
│  │  • Calculate overall health score                                │  │
│  │  • Categorize results (PASS/WARN/FAIL)                          │  │
│  │  • Determine system status                                       │  │
│  │  • Generate recommendations                                      │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                    │                                    │
│                                    ▼                                    │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │                    REPORTING PHASE                               │  │
│  │  • Console output (real-time)                                    │  │
│  │  • Summary display                                               │  │
│  │  • Exit code determination                                       │  │
│  └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
        ┌──────────────────┐ ┌──────────┐ ┌─────────────────┐
        │   Console        │ │  Exit    │ │  Optional       │
        │   Output         │ │  Code    │ │  Reports        │
        │   (stdout)       │ │  (0/1/2) │ │  (MD/TXT)       │
        └──────────────────┘ └──────────┘ └─────────────────┘
```

---

## 🔄 Execution Flow

### Flow Diagram

```
START
  │
  ▼
┌─────────────────────────────────────┐
│ User Executes Health Check          │
│ (Batch/PowerShell/Java)             │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ Parse Command-Line Arguments        │
│ • Get installation directory path   │
│ • Validate arguments                │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ Validate Installation Directory     │
│ • Check if directory exists         │
│ • Verify read permissions           │
└─────────────────────────────────────┘
  │
  ├─── [Directory NOT Found] ──────────────────┐
  │                                             │
  │                                             ▼
  │                                    ┌─────────────────┐
  │                                    │ Display Error   │
  │                                    │ Exit Code: 2    │
  │                                    └─────────────────┘
  │                                             │
  │                                             ▼
  │                                           END
  │
  ▼
┌─────────────────────────────────────┐
│ Display Banner                      │
│ • Tool version                      │
│ • Current date/time                 │
│ • Hostname                          │
│ • Installation directory            │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ Initialize Health Check System      │
│ • Set counters to 0                 │
│ • Load thresholds                   │
│ • Initialize result storage         │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ CHECK 1: CPU Utilization (SYS-001) │
│ • Get system CPU load               │
│ • Compare with thresholds           │
│ • Record result                     │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ CHECK 2: Memory Usage (SYS-002)    │
│ • Get JVM memory stats              │
│ • Calculate percentage              │
│ • Compare with thresholds           │
│ • Record result                     │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ CHECK 3: Disk Space (SYS-003)      │
│ • Get disk usage for C: drive       │
│ • Calculate percentage              │
│ • Compare with thresholds           │
│ • Record result                     │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ CHECK 4: JVM Heap Size (SRV-001)   │
│ • Read windows.service.properties   │
│ • Parse -Xmx parameter              │
│ • Compare with minimum (4096 MB)    │
│ • Record result (CRITICAL CHECK)    │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ CHECK 5: Database (DB-001)         │
│ • Read db.properties                │
│ • Parse JDBC URL, username          │
│ • Display configuration             │
│ • Skip actual connection test       │
│ • Record result                     │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ CHECK 6: Server Port (SRV-003)     │
│ • Attempt socket connection         │
│ • Test port 7599 accessibility      │
│ • Record result                     │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ CHECK 7: Installation Dir (SRV-005)│
│ • Verify key directories exist      │
│   - bin/, conf/, lib/, logs/, jre/  │
│ • Record result                     │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ Calculate Overall Health Score      │
│ • Score = (Pass×100 + Warn×60) / Total│
│ • Determine status:                 │
│   - 90-100: EXCELLENT               │
│   - 75-89:  GOOD                    │
│   - 60-74:  FAIR                    │
│   - 40-59:  POOR                    │
│   - 0-39:   CRITICAL                │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ Display Summary                     │
│ • Overall health score              │
│ • Status indicator                  │
│ • Total checks                      │
│ • Pass/Warn/Fail counts             │
│ • Critical issues warning           │
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ Determine Exit Code                 │
│ • Score ≥ 60: Exit 0 (SUCCESS)     │
│ • Score < 60: Exit 1 (FAILURE)     │
└─────────────────────────────────────┘
  │
  ▼
END
```

---

## 🧩 Component Details

### 1. Entry Points

#### A. Batch File Wrapper (`run_healthcheck_simple.bat`)
```
Purpose: Windows command-line wrapper
Flow:
  1. Validate installation directory argument
  2. Check if directory exists
  3. Verify Java class is compiled
  4. Auto-compile if needed
  5. Execute Java class with directory argument
  6. Display results
  7. Return exit code
```

#### B. PowerShell Script (`Run-HealthCheck.ps1`)
```
Purpose: Advanced Windows automation
Flow:
  1. Parse parameters (InstallDir, OutputDir, GenerateReports)
  2. Validate installation directory
  3. Check Java availability
  4. Compile if needed
  5. Execute health check
  6. Display formatted results
  7. Optionally generate additional reports
  8. Return exit code
```

#### C. Direct Java Execution
```
Purpose: Cross-platform execution
Flow:
  1. Java runtime invokes main() method
  2. Passes installation directory as args[0]
  3. Executes health check
  4. Returns exit code to OS
```

---

### 2. Main Application (`DSeriesHealthCheckSimple.java`)

#### Class Structure
```java
public class DSeriesHealthCheckSimple {
    // Configuration variables
    private static String dbHost = "G36R0T3";
    private static int dbPort = 5432;
    private static String dbName = "dseries";
    private static String dbUser = "postgres";
    private static String installDir = "C:/CA/ESPdSeriesWAServer_R12_4";
    
    // Counters
    private static int totalChecks = 0;
    private static int passedChecks = 0;
    private static int warningChecks = 0;
    private static int failedChecks = 0;
    
    // Thresholds
    private static int cpuWarning = 70;
    private static int cpuCritical = 85;
    private static int memWarning = 80;
    private static int memCritical = 90;
    private static int diskWarning = 75;
    private static int diskCritical = 85;
    private static int jvmHeapMinMB = 4096;
}
```

---

## ⚙️ Configuration System

### Configuration Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                  CONFIGURATION SOURCES                       │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────┐
│ Command-Line │  │  dSeries Config  │  │   Default    │
│  Arguments   │  │     Files        │  │   Values     │
│              │  │                  │  │              │
│ • InstallDir │  │ • db.properties  │  │ • Hardcoded  │
│              │  │ • windows.service│  │ • In Java    │
│              │  │   .properties    │  │   code       │
└──────────────┘  └──────────────────┘  └──────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ▼
                ┌─────────────────────┐
                │  Configuration      │
                │  Merge & Priority   │
                │                     │
                │  Priority Order:    │
                │  1. Command-line    │
                │  2. Config files    │
                │  3. Defaults        │
                └─────────────────────┘
                            │
                            ▼
                ┌─────────────────────┐
                │   Active Runtime    │
                │   Configuration     │
                └─────────────────────┘
```

### Configuration Files

#### 1. `db.properties` (Database Configuration)
**Location:** `{installDir}/conf/db.properties`

**Purpose:** Contains database connection parameters

**Format:**
```properties
# Database Connection Settings
jdbc.URL=jdbc:postgresql://G36R0T3:5432/dseries
rdbms.userid=postgres
rdbms.password=<encrypted>
```

**How It's Read:**
```java
File dbPropsFile = new File(installDir + "/conf/db.properties");
if (dbPropsFile.exists()) {
    BufferedReader reader = new BufferedReader(new FileReader(dbPropsFile));
    String line;
    while ((line = reader.readLine()) != null) {
        if (line.startsWith("jdbc.URL=")) {
            String url = line.substring("jdbc.URL=".length());
            // Parse: jdbc:postgresql://G36R0T3:5432/dseries
            // Extract: host, port, database name
        } else if (line.startsWith("rdbms.userid=")) {
            dbUser = line.substring("rdbms.userid=".length());
        }
    }
    reader.close();
}
```

**Data Extracted:**
- Database host (e.g., G36R0T3)
- Database port (e.g., 5432)
- Database name (e.g., dseries)
- Database username (e.g., postgres)

---

#### 2. `windows.service.properties` (JVM Configuration)
**Location:** `{installDir}/conf/windows.service.properties`

**Purpose:** Contains JVM heap size and other Java settings

**Format:**
```properties
# JVM Properties
jvmproperty_1=-Djava.awt.headless=true
jvmproperty_2=-Xms1024M
jvmproperty_3=-Xmx1024M
jvmproperty_4=-XX:+UseG1GC
```

**How It's Read:**
```java
File propsFile = new File(installDir + "/conf/windows.service.properties");
int heapSizeMB = 0;

if (propsFile.exists()) {
    BufferedReader reader = new BufferedReader(new FileReader(propsFile));
    String line;
    while ((line = reader.readLine()) != null) {
        if (line.contains("-Xmx")) {
            // Extract heap size from -Xmx1024M or -Xmx4g
            String[] parts = line.split("=");
            if (parts.length > 1) {
                String xmx = parts[1].trim();
                if (xmx.contains("-Xmx")) {
                    xmx = xmx.substring(xmx.indexOf("-Xmx") + 4);
                    if (xmx.toLowerCase().endsWith("g")) {
                        heapSizeMB = Integer.parseInt(xmx.substring(0, xmx.length() - 1)) * 1024;
                    } else if (xmx.toLowerCase().endsWith("m")) {
                        heapSizeMB = Integer.parseInt(xmx.substring(0, xmx.length() - 1));
                    }
                }
            }
        }
    }
    reader.close();
}
```

**Data Extracted:**
- JVM maximum heap size (-Xmx)
- JVM initial heap size (-Xms)
- Garbage collector settings

---

#### 3. `healthcheck.properties` (Optional - Future Enhancement)
**Location:** `{installDir}/config/healthcheck.properties`

**Purpose:** Customize health check thresholds

**Format:**
```properties
# System Resource Thresholds
threshold.cpu.warning=70
threshold.cpu.critical=85
threshold.memory.warning=80
threshold.memory.critical=90
threshold.disk.warning=75
threshold.disk.critical=85

# JVM Settings
threshold.jvm.heap.min.mb=4096
threshold.jvm.heap.recommended.mb=4096

# Server Settings
server.host=localhost
server.port=7599

# Database Settings
db.host=localhost
db.port=5432
db.name=dseries
db.user=postgres
```

**Note:** Currently uses hardcoded defaults. This file can be implemented for future customization.

---

## 🔍 Health Check Process

### Detailed Check Flow

#### CHECK 1: CPU Utilization (SYS-001)

```
┌─────────────────────────────────────┐
│ Get System CPU Load                 │
│ Using: OperatingSystemMXBean        │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│ Calculate CPU Percentage            │
│ cpuLoad = osBean.getSystemCpuLoad() │
│ cpuPercent = cpuLoad * 100          │
└─────────────────────────────────────┘
                │
                ▼
        ┌───────┴───────┐
        │               │
        ▼               ▼
┌──────────────┐  ┌──────────────┐
│ CPU >= 85%?  │  │ CPU >= 70%?  │
│   (Critical) │  │  (Warning)   │
└──────────────┘  └──────────────┘
        │               │
        ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   FAIL       │  │   WARNING    │  │    PASS      │
│ failedChecks++│  │warningChecks++│  │passedChecks++│
└──────────────┘  └──────────────┘  └──────────────┘
        │               │               │
        └───────────────┼───────────────┘
                        ▼
            ┌─────────────────────┐
            │ Display Result      │
            │ totalChecks++       │
            └─────────────────────┘
```

**Code:**
```java
private static void checkCpuUtilization() {
    System.out.println("[SYS-001] Checking CPU Utilization...");
    totalChecks++;
    
    try {
        com.sun.management.OperatingSystemMXBean osBean = 
            (com.sun.management.OperatingSystemMXBean) 
            java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        
        double cpuLoad = osBean.getSystemCpuLoad() * 100;
        
        if (cpuLoad >= cpuCritical) {
            System.out.println("  [FAIL] CRITICAL: CPU usage is " + cpuLoad + "%");
            failedChecks++;
        } else if (cpuLoad >= cpuWarning) {
            System.out.println("  [WARN] WARNING: CPU usage is " + cpuLoad + "%");
            warningChecks++;
        } else {
            System.out.println("  [PASS] CPU usage is " + cpuLoad + "% (healthy)");
            passedChecks++;
        }
    } catch (Exception e) {
        System.out.println("  [SKIP] Error checking CPU: " + e.getMessage());
    }
}
```

---

#### CHECK 4: JVM Heap Size (SRV-001) - CRITICAL

```
┌─────────────────────────────────────┐
│ Locate Configuration File           │
│ {installDir}/conf/                  │
│ windows.service.properties          │
└─────────────────────────────────────┘
                │
                ▼
        ┌───────┴───────┐
        │               │
        ▼               ▼
┌──────────────┐  ┌──────────────┐
│ File Exists? │  │ File Missing │
│     YES      │  │     NO       │
└──────────────┘  └──────────────┘
        │               │
        ▼               ▼
┌──────────────┐  ┌──────────────┐
│ Read File    │  │   SKIP       │
│ Line by Line │  │   CHECK      │
└──────────────┘  └──────────────┘
        │
        ▼
┌─────────────────────────────────────┐
│ Search for -Xmx Parameter           │
│ Pattern: jvmproperty_*=-Xmx*        │
└─────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────┐
│ Parse Heap Size                     │
│ • -Xmx4096M  → 4096 MB             │
│ • -Xmx4g     → 4096 MB             │
│ • -Xmx1024M  → 1024 MB             │
└─────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────┐
│ Compare with Minimum (4096 MB)      │
└─────────────────────────────────────┘
        │
        ▼
        ┌───────┴───────┐
        │               │
        ▼               ▼
┌──────────────┐  ┌──────────────┐
│ Heap < 4096? │  │ Heap >= 4096?│
│     YES      │  │     NO       │
└──────────────┘  └──────────────┘
        │               │
        ▼               ▼
┌──────────────┐  ┌──────────────┐
│   FAIL       │  │    PASS      │
│ CRITICAL!    │  │   Healthy    │
│ Display Fix  │  │              │
│ Instructions │  │              │
└──────────────┘  └──────────────┘
```

**This is the MOST CRITICAL check for dSeries!**

---

## 📊 Scoring Algorithm

### Score Calculation

```
┌─────────────────────────────────────┐
│ Input: Check Results                │
│ • passedChecks                      │
│ • warningChecks                     │
│ • failedChecks                      │
│ • totalChecks                       │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│ Apply Weights                       │
│ • PASS    = 100 points              │
│ • WARNING = 60 points               │
│ • FAIL    = 0 points                │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│ Calculate Weighted Score            │
│                                     │
│ score = (passedChecks × 100 +      │
│          warningChecks × 60 +       │
│          failedChecks × 0)          │
│         ÷ totalChecks               │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│ Determine Status                    │
│ • 90-100: EXCELLENT ✅             │
│ • 75-89:  GOOD      🟢             │
│ • 60-74:  FAIR      🟡             │
│ • 40-59:  POOR      🟠             │
│ • 0-39:   CRITICAL  🔴             │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│ Determine Exit Code                 │
│ • score >= 60: Exit 0 (SUCCESS)    │
│ • score < 60:  Exit 1 (FAILURE)    │
└─────────────────────────────────────┘
```

### Example Calculation

**Scenario:**
- Total Checks: 7
- Passed: 3
- Warnings: 1
- Failed: 3

**Calculation:**
```
Score = (3 × 100 + 1 × 60 + 3 × 0) ÷ 7
      = (300 + 60 + 0) ÷ 7
      = 360 ÷ 7
      = 51.4
      ≈ 51/100
```

**Result:** POOR (40-59 range)  
**Exit Code:** 1 (FAILURE)

---

## 📝 Reporting System

### Report Generation Flow

```
┌─────────────────────────────────────┐
│ Health Checks Complete              │
│ All results collected               │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│ Generate Console Output             │
│ • Real-time during execution        │
│ • Color-coded results               │
│ • [PASS], [WARN], [FAIL] indicators│
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│ Display Summary Section             │
│ ═══════════════════════════════════ │
│   HEALTH CHECK SUMMARY              │
│ ═══════════════════════════════════ │
│                                     │
│   Overall Health Score: XX/100      │
│   Status: [STATUS]                  │
│                                     │
│   Total Checks: X                   │
│   ✅ Passed: X                      │
│   ⚠️  Warnings: X                   │
│   ❌ Failed: X                      │
│ ═══════════════════════════════════ │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│ Optional: Generate File Reports     │
│ (If wrapper scripts used)           │
│                                     │
│ • HEALTH_CHECK_RESULTS_*.md         │
│ • ACTION_PLAN_*.md                  │
│ • EXECUTIVE_SUMMARY_*.txt           │
└─────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────┐
│ Return Exit Code                    │
│ • 0: Success (score >= 60)         │
│ • 1: Failure (score < 60)          │
│ • 2: Error (invalid input)         │
└─────────────────────────────────────┘
```

---

## 🔗 Integration Points

### System Integration Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    EXTERNAL SYSTEMS                          │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────┐
│   Operating  │  │  dSeries Config  │  │   dSeries    │
│    System    │  │     Files        │  │   Server     │
│              │  │                  │  │              │
│ • CPU stats  │  │ • db.properties  │  │ • Port 7599  │
│ • Memory     │  │ • windows.service│  │ • Service    │
│ • Disk space │  │   .properties    │  │   status     │
└──────────────┘  └──────────────────┘  └──────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ▼
                ┌─────────────────────┐
                │  Health Check Tool  │
                │                     │
                │ • Reads OS metrics  │
                │ • Parses config     │
                │ • Tests connectivity│
                └─────────────────────┘
                            │
                            ▼
                ┌─────────────────────┐
                │  Output Channels    │
                └─────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐  ┌──────────────────┐  ┌──────────────┐
│   Console    │  │   Exit Code      │  │   Reports    │
│   (stdout)   │  │   (OS process)   │  │   (files)    │
└──────────────┘  └──────────────────┘  └──────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            ▼
                ┌─────────────────────┐
                │  Automation Tools   │
                │                     │
                │ • Task Scheduler    │
                │ • Monitoring        │
                │ • Alerting          │
                └─────────────────────┘
```

---

## 🎯 Complete End-to-End Example

### Scenario: Running Health Check

```
USER ACTION:
  C:\> run_healthcheck_simple.bat "C:\CA\ESPdSeriesWAServer_R12_4"

STEP 1: Batch File Execution
  ├─ Validate argument provided ✓
  ├─ Check directory exists ✓
  ├─ Check if .class file exists ✓
  └─ Execute: java DSeriesHealthCheckSimple "C:\CA\ESPdSeriesWAServer_R12_4"

STEP 2: Java Application Starts
  ├─ main() method invoked
  ├─ args[0] = "C:\CA\ESPdSeriesWAServer_R12_4"
  ├─ installDir = args[0]
  ├─ Validate directory exists ✓
  └─ Display banner

STEP 3: Health Checks Execute
  ├─ [SYS-001] CPU Utilization
  │   ├─ Get CPU load: 45.2%
  │   ├─ Compare: 45.2% < 70% (warning)
  │   ├─ Result: PASS ✓
  │   └─ passedChecks = 1
  │
  ├─ [SYS-002] Memory Usage
  │   ├─ Get memory: 65.3%
  │   ├─ Compare: 65.3% < 80% (warning)
  │   ├─ Result: PASS ✓
  │   └─ passedChecks = 2
  │
  ├─ [SYS-003] Disk Space
  │   ├─ Get disk usage: 98.2%
  │   ├─ Compare: 98.2% > 85% (critical)
  │   ├─ Result: FAIL ✗
  │   └─ failedChecks = 1
  │
  ├─ [SRV-001] JVM Heap Size
  │   ├─ Read: C:\CA\ESPdSeriesWAServer_R12_4\conf\windows.service.properties
  │   ├─ Find: jvmproperty_3=-Xmx1024M
  │   ├─ Parse: 1024 MB
  │   ├─ Compare: 1024 < 4096 (minimum)
  │   ├─ Result: FAIL ✗ (CRITICAL!)
  │   └─ failedChecks = 2
  │
  ├─ [DB-001] Database Connectivity
  │   ├─ Read: C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties
  │   ├─ Parse: jdbc:postgresql://G36R0T3:5432/dseries
  │   ├─ Extract: host=G36R0T3, port=5432, db=dseries
  │   ├─ JDBC driver not in classpath
  │   └─ Result: SKIP (informational)
  │
  ├─ [SRV-003] Server Port
  │   ├─ Test: localhost:7599
  │   ├─ Socket connection attempt
  │   ├─ Connection refused
  │   ├─ Result: FAIL ✗
  │   └─ failedChecks = 3
  │
  └─ [SRV-005] Installation Directory
      ├─ Check: bin/ ✓
      ├─ Check: conf/ ✓
      ├─ Check: lib/ ✓
      ├─ Check: logs/ ✓
      ├─ Check: jre/ ✓
      ├─ Result: PASS ✓
      └─ passedChecks = 3

STEP 4: Calculate Score
  ├─ totalChecks = 7
  ├─ passedChecks = 3
  ├─ warningChecks = 0
  ├─ failedChecks = 3
  ├─ score = (3×100 + 0×60 + 3×0) / 7
  ├─ score = 300 / 7
  ├─ score = 42.8 ≈ 42/100
  └─ status = POOR (40-59 range)

STEP 5: Display Summary
  ═══════════════════════════════════
    HEALTH CHECK SUMMARY
  ═══════════════════════════════════
  
    Overall Health Score: 42/100
    Status: POOR
  
    Total Checks: 7
    ✅ Passed: 3
    ⚠️  Warnings: 0
    ❌ Failed: 3
  
    WARNING: 3 CRITICAL ISSUE(S) DETECTED!
  ═══════════════════════════════════

STEP 6: Exit
  ├─ score = 42 < 60
  ├─ Exit code = 1 (FAILURE)
  └─ System.exit(1)

RESULT:
  ├─ Console output displayed
  ├─ Exit code returned to OS: 1
  └─ Batch file displays completion message
```

---

## 📚 Summary

### Key Takeaways

1. **Modular Design**: Each health check is independent
2. **Configuration-Driven**: Reads from dSeries config files
3. **Flexible Execution**: Multiple entry points (Batch/PS/Java)
4. **Clear Scoring**: Weighted algorithm with defined thresholds
5. **Actionable Output**: Clear PASS/WARN/FAIL indicators
6. **Automation-Ready**: Exit codes for scripting

### Critical Configuration Files

| File | Purpose | Key Data |
|------|---------|----------|
| `db.properties` | Database config | JDBC URL, username |
| `windows.service.properties` | JVM settings | Heap size (-Xmx) |
| `healthcheck.properties` | Thresholds (optional) | Custom limits |

### Health Check Priority

1. **CRITICAL**: JVM Heap Size (SRV-001) - Must be ≥ 4096 MB
2. **CRITICAL**: Disk Space (SYS-003) - Must be < 85%
3. **HIGH**: Server Port (SRV-003) - Must be accessible
4. **MEDIUM**: CPU, Memory, Database, Installation

---

**Document Version:** 1.0.0  
**Last Updated:** February 11, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
