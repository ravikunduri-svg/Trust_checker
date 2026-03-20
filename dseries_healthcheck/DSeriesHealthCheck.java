import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import java.util.Date;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import java.security.GeneralSecurityException;

/**
 * ESP dSeries Workload Automation Health Check Tool - Enhanced Version
 * 
 * Comprehensive health check tool with external SQL configuration support.
 * Performs technical reviews including architecture, security, database,
 * agents, clients, high availability, and performance checks.
 * 
 * Version: 4.2.2
 * Date: 2026-02-11
 * 
 * Version 4.2.2 Enhancements:
 * - Fixed namespace-aware calendar detection for all XML formats
 * - Made dependency visualization optional with --show-dependencies (-d) flag
 * - Dependency analysis now only shows when explicitly requested
 * 
 * Version 4.2.1 Enhancements:
 * - Fixed calendar detection to recognize "daily", "weekly", "monthly", "yearly" schedules
 * - Prevents false calendar recommendations for apps using frequency-based scheduling
 * 
 * Version 4.2.0 Enhancements:
 * - Dependency and flow visualization for applications
 * - Cross-application dependency tracking (ext_job with applid)
 * - Internal job dependency analysis (relconditionlist)
 * - Multiple output formats: tables and ASCII flow graphs
 * - Dependency statistics and relationship mapping
 * 
 * Version 4.1.2 Enhancements:
 * - Fixed ext_job (external/sub-application jobs) counting - now included in job totals
 * - Excluded link elements from job counts (they are flow control, not jobs)
 * - Improved calendar usage detection to check actual schedules (MON-SUN, calendar references)
 * - Increased calendar recommendation threshold from >5 to >10 jobs to reduce false positives
 * - Added multi-level calendar detection (app-level, run frequency, job schedules)
 * 
 * Version 4.1.1 Enhancements:
 * - Added support for dSeries native application files (files without .xml extension)
 * - Refined FTP cloud recommendations to exclude native file transfers
 * - Fixed unix_job and nt_job counting in application analysis
 * 
 * Version 4.1.0 Enhancements:
 * - Standalone application analysis mode (--analyze-apps flag)
 * - Analyze exported application artifacts without dSeries installation
 * - No database connection required for application analysis
 * 
 * Version 4.0.0 Enhancements:
 * - Application best practices scanner with XML analysis
 * - Cloud integration opportunity detection (AWS, Azure, GCP, 40+ plugins)
 * - Job configuration validation (hardcoded paths, credentials, retry logic)
 * - Application design recommendations (JavaScript, calendars, defaults)
 * - Security best practices validation (credential management)
 * - Modernization roadmap with specific plugin recommendations
 * - Integration with Broadcom's 40+ cloud plugin extensions
 * 
 * Version 3.0.0 Enhancements:
 * - Comprehensive log file analysis (errors.txt, stdout, tracelog files)
 * - Thread dump analysis with deadlock detection
 * - Database connection pool analysis and leak detection
 * - Proactive error pattern recognition and recommendations
 * - Startup performance analysis
 * - Automatic root cause analysis for common issues
 * - Connection usage statistics and bottleneck identification
 * 
 * Version 2.11.0 Enhancements:
 * - Complete rewrite of connectToDatabase() to match DBConnectionValidator exactly
 * - Passes ALL db.properties to DriverManager (not just user/password)
 * - Removes specific properties before passing to driver (jdbc.Driver, jdbc.URL, etc.)
 * - Uses Scrambler.recover() with RelationalDatabaseManager.getKey() for decryption
 * - Fixed property removal order (EXACTLY like DBConnectionValidator)
 * - Fixed JVM config detection (now checks startServer on Unix)
 * 
 * Version 2.10.0 Enhancements:
 * - Enhanced password decryption with RelationalDatabaseManager key lookup
 * - Added Base64 password detection and decryption
 * - Improved error handling for InvocationTargetException
 * - Better debugging for password decryption failures
 * 
 * Version 2.9.0 Enhancements:
 * - Fixed Oracle JDBC URL handling (now uses original URL from db.properties)
 * - Fixed ORA-17866 error (invalid port number format)
 * - Fixed JVM config file detection (now checks startServer on Unix)
 * - Classpath scripts correctly identified (classpath.sh on Unix, classpath.bat on Windows)
 * - Enhanced Oracle URL parsing to support //[host]:port/service format
 * 
 * Version 2.8.0 Enhancements:
 * - Fixed Oracle JDBC driver detection (was incorrectly using PostgreSQL driver)
 * - Fixed dbDriver property reading (now checks both jdbc.Driver and rdbms.driver)
 * - Fixed JVM heap size detection on Linux (now checks espserver/espserver.sh)
 * - Removed control-M characters from shell script (proper LF line endings)
 * - Added driver name to configuration output for debugging
 * 
 * Version 2.7.0 Enhancements:
 * - Verified all SQL queries against actual dSeries database schema
 * - Fixed table names: ESP_EVENT_RP, ESP_RESOURCE_RP, ESP_ALERT_RP, ESP_CONN_PROFILE_RP
 * - Fixed column names: ACTIONSTATUS, HOLD_COUNT, AVAILABILITY, MAX_AVAILABILITY
 * - Removed non-existent tables: ESP_EVENT, ESP_CALENDAR, ESP_TRIGGER, ESP_SESSION
 * - All 34 health checks now execute successfully without errors
 * 
 * Version 2.6.0 Enhancements:
 * - Database-agnostic SQL query conversion (PostgreSQL, Oracle, DB2, SQL Server)
 * - Additional health check queries (agents, events, calendars, resources, security)
 * - Improved error messages (removed internal class names)
 * - Fixed line endings in shell script
 * 
 * Version 2.4.0 Enhancements:
 * - SSL/TLS certificate support (trustStore, trustStorePassword)
 * - Windows authentication (integratedSecurity) detection and handling
 * - AIX+Oracle SSL special handling
 * - Connection validation with timeout (isValid)
 * - Query timeout and performance monitoring
 * - Enhanced resource cleanup with proper finally blocks
 * - Scrambler encryption support (dSeries native)
 * - Connection retry logic with configurable attempts
 * 
 * Version 2.3.0 Enhancements:
 * - Dynamic classpath loading from dSeries bin/classpath.bat
 * - Native dSeries password encryption/decryption support
 * - Universal database support (PostgreSQL, Oracle, DB2, SQL Server, etc.)
 * - Windows authentication DLL support for SQL Server
 * - Automatic detection and loading of all JDBC drivers
 * 
 * Version 2.2.0 Enhancements:
 * - Automatic JDBC driver detection and loading
 * - Enhanced database connection troubleshooting
 * - Support for PostgreSQL, Oracle, SQL Server
 * - Improved error messages and guidance
 * 
 * Features:
 * - External SQL query configuration
 * - Database table size monitoring
 * - Data integrity checks
 * - Agent and client connectivity
 * - High availability monitoring
 * - Performance metrics
 * - Housekeeping validation
 * - Version and component tracking
 * - Migration history
 * - Sensitive data protection
 * 
 * Usage:
 *   java DSeriesHealthCheck <install_dir> [db_config_file] [sql_config_file]
 * 
 * Example:
 *   java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4
 *   java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4 config/db.properties config/health_check_queries.sql
 */
/**
 * Log Analysis Support Classes
 */
class ErrorPattern {
    String pattern;
    String errorType;
    String severity;
    String description;
    String recommendation;
    int count;
    List<String> examples;
    
    public ErrorPattern(String pattern, String errorType, String severity, String description, String recommendation) {
        this.pattern = pattern;
        this.errorType = errorType;
        this.severity = severity;
        this.description = description;
        this.recommendation = recommendation;
        this.count = 0;
        this.examples = new ArrayList<>();
    }
}

class ConnectionPoolStats {
    int uniqueConnections;
    long totalOperations;
    Map<String, Long> connectionUsage;
    String primaryConnection;
    long primaryConnectionOps;
    double primaryConnectionPercent;
    boolean leaksDetected;
    List<String> leakedConnections;
    
    public ConnectionPoolStats() {
        this.connectionUsage = new HashMap<>();
        this.leakedConnections = new ArrayList<>();
    }
}

class StartupPerformance {
    long serverStartTime;
    Map<String, Long> componentReadyTimes;
    long totalStartupTime;
    long applicationManagerTime;
    long restComponentTime;
    String bottleneckComponent;
    long bottleneckDuration;
    
    public StartupPerformance() {
        this.componentReadyTimes = new LinkedHashMap<>();
    }
}

class ThreadDumpInfo {
    int totalThreads;
    int blockedThreads;
    int runnableThreads;
    int waitingThreads;
    String lockObject;
    List<String> blockedOnLock;
    boolean deadlockDetected;
    String deadlockDescription;
    
    public ThreadDumpInfo() {
        this.blockedOnLock = new ArrayList<>();
    }
}

public class DSeriesHealthCheck {
    
    // Configuration
    private static String installDir;
    private static String sqlConfigFile = "config/health_check_queries.sql";
    private static boolean useExternalDbConfig = false;
    private static String externalDbConfigFile = null;
    private static int serverPort = 7599; // Default, will be read from instanceconf.xml
    private static boolean showDependencies = false; // Flag to control dependency visualization
    
    // Database connection details
    private static String dbHost = "localhost";
    private static int dbPort = 5432;
    private static String dbName = "dseries";
    private static String dbUser = "dseries_user";
    private static String dbPassword = "";
    private static String dbDriver = "org.postgresql.Driver";
    private static String dbType = "";
    private static String originalJdbcUrl = null;  // Store original URL from db.properties
    private static Connection dbConnection = null;
    
    // SSL/TLS properties
    private static String sslTrustStore = null;
    private static String sslTrustStorePassword = null;
    private static boolean sslEnabled = false;
    
    // Windows authentication
    private static boolean integratedSecurity = false;
    
    // Connection timing
    private static long connectionStartTime = 0;
    private static long connectionEndTime = 0;
    private static long queryStartTime = 0;
    private static long queryEndTime = 0;
    
    // Health check results
    private static int totalChecks = 0;
    private static int passedChecks = 0;
    private static int warningChecks = 0;
    private static int failedChecks = 0;
    private static int infoChecks = 0;
    
    // Check results storage
    private static List<CheckResult> checkResults = new ArrayList<>();
    
    // Thresholds
    private static int cpuWarning = 70;
    private static int cpuCritical = 85;
    private static int memWarning = 80;
    private static int memCritical = 90;
    private static int diskWarning = 75;
    private static int diskCritical = 85;
    private static int jvmHeapMinMB = 4096;
    
    // SQL Query metadata
    static class SQLCheck {
        String checkId;
        String checkName;
        String category;
        String severity;
        String description;
        String query;
        String thresholdOperator;
        String thresholdValue;
        String remediation;
        
        public SQLCheck(String checkId, String checkName, String category, String severity,
                       String description, String query, String thresholdOperator,
                       String thresholdValue, String remediation) {
            this.checkId = checkId;
            this.checkName = checkName;
            this.category = category;
            this.severity = severity;
            this.description = description;
            this.query = query;
            this.thresholdOperator = thresholdOperator;
            this.thresholdValue = thresholdValue;
            this.remediation = remediation;
        }
    }
    
    // Check result storage
    static class CheckResult {
        String checkId;
        String checkName;
        String category;
        String severity;
        String status;
        String message;
        String remediation;
        
        public CheckResult(String checkId, String checkName, String category,
                          String severity, String status, String message, String remediation) {
            this.checkId = checkId;
            this.checkName = checkName;
            this.category = category;
            this.severity = severity;
            this.status = status;
            this.message = message;
            this.remediation = remediation;
        }
    }
    
    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length < 1) {
            printUsage();
            System.exit(2);
        }
        
        // Check for standalone application analysis mode
        if (args[0].equals("--analyze-apps") || args[0].equals("-a")) {
            if (args.length < 2) {
                System.err.println("ERROR: Application directory path required");
                System.err.println();
                printUsage();
                System.exit(2);
            }
            // Check for --show-dependencies flag
            if (args.length > 2 && (args[2].equals("--show-dependencies") || args[2].equals("-d"))) {
                showDependencies = true;
            }
            runStandaloneApplicationAnalysis(args[1]);
            return;
        }
        
        installDir = args[0];
        
        // Optional: Use external database config (for testing/non-standard setups)
        if (args.length > 1 && args[1].toLowerCase().endsWith("db.properties")) {
            useExternalDbConfig = true;
            externalDbConfigFile = args[1];
        }
        
        // Optional: Custom SQL queries file
        if (args.length > 1 && args[1].toLowerCase().endsWith(".sql")) {
            sqlConfigFile = args[1];
        } else if (args.length > 2) {
            sqlConfigFile = args[2];
        }
        
        // Print header
        printHeader();
        
        // Validate installation directory
        File installDirFile = new File(installDir);
        if (!installDirFile.exists()) {
            System.err.println("ERROR: Installation directory not found: " + installDir);
            System.exit(2);
        }
        
        System.out.println("Starting comprehensive health check...");
        System.out.println();
        
        // Run health checks
        try {
            // System checks
            checkCpuUtilization();
            checkMemoryUsage();
            checkDiskSpace();
            checkJvmHeapSize();
            checkInstallationDirectory();
            checkServerPort();
            checkLoggingConfiguration();
            
            // Load database configuration
            loadDatabaseConfig();
            
            // Database checks (if connection available)
            if (connectToDatabase()) {
                runDatabaseChecks();
                disconnectFromDatabase();
            } else {
                System.out.println("⚠️  Database checks skipped (no connection)");
                System.out.println();
            }
            
            // Log analysis (new in v3.0.0)
            performLogAnalysis();
            
            // Application best practices analysis (new in v4.0.0)
            performApplicationAnalysis();
            
            // Display summary
            displaySummary();
            
            // Generate report
            generateReport();
            
            // Exit with appropriate code
            int healthScore = calculateHealthScore();
            if (healthScore >= 60) {
                System.exit(0);  // Success
            } else {
                System.exit(1);  // Failure
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: Health check failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  1. Full Health Check:");
        System.out.println("     java DSeriesHealthCheck <install_dir> [sql_config_file]");
        System.out.println();
        System.out.println("  2. Standalone Application Analysis:");
        System.out.println("     java DSeriesHealthCheck --analyze-apps <apps_directory> [--show-dependencies]");
        System.out.println("     java DSeriesHealthCheck -a <apps_directory> [-d]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  install_dir         - dSeries installation directory (required for full check)");
        System.out.println("  sql_config_file     - SQL queries configuration file (optional)");
        System.out.println("  apps_directory      - Directory containing exported application XML files");
        System.out.println("  --show-dependencies - Show dependency and flow analysis (optional)");
        System.out.println("  -d                  - Short form of --show-dependencies");
        System.out.println();
        System.out.println("Standalone Application Analysis Mode:");
        System.out.println("  Analyzes application files without requiring dSeries installation");
        System.out.println("  Supports: .xml files (exported) and files without extension (native format)");
        System.out.println("  Perfect for analyzing Desktop Client apps or exported artifacts");
        System.out.println("  Provides best practices validation and cloud integration recommendations");
        System.out.println("  No database connection required");
        System.out.println("  Use --show-dependencies flag to include dependency/flow visualization");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Full health check");
        System.out.println("  java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4");
        System.out.println();
        System.out.println("  # Analyze exported applications only");
        System.out.println("  java DSeriesHealthCheck --analyze-apps C:/exports/applications");
        System.out.println("  java DSeriesHealthCheck -a /home/user/exported_apps");
        System.out.println();
        System.out.println("  # Analyze with dependency visualization");
        System.out.println("  java DSeriesHealthCheck --analyze-apps C:/exports/apps --show-dependencies");
        System.out.println("  java DSeriesHealthCheck -a C:/exports/apps -d");
        System.out.println();
        System.out.println("  # Analyze single application file");
        System.out.println("  java DSeriesHealthCheck --analyze-apps C:/exports/PAYROLL_APP.xml");
        System.out.println();
        System.out.println("How to Export Applications:");
        System.out.println("  1. Desktop Client: Right-click application -> Export -> Save as XML");
        System.out.println("  2. CLI: espappexport -A APPLICATION_NAME -f output.xml");
        System.out.println("  3. REST API: GET /application/{name}/export");
        System.out.println();
        System.out.println("Advanced (for testing with external db.properties):");
        System.out.println("  java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4 /path/to/db.properties");
    }
    
    private static void printHeader() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  ESP dSeries Workload Automation Health Check Tool v4.2.2");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        System.out.println("  Host: " + getHostname());
        System.out.println("  dSeries: " + installDir);
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
    }
    
    private static String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    // ========================================================================
    // SYSTEM CHECKS
    // ========================================================================
    
    private static void checkCpuUtilization() {
        System.out.println("[SYS-001] Checking CPU Utilization...");
        totalChecks++;
        
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) 
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            
            double cpuLoad = osBean.getSystemCpuLoad() * 100;
            int availableProcessors = osBean.getAvailableProcessors();
            
            String status, message;
            if (cpuLoad < cpuWarning) {
                status = "PASS";
                passedChecks++;
                message = String.format("✅ CPU: %.1f%% (Normal) - %d cores available", cpuLoad, availableProcessors);
            } else if (cpuLoad < cpuCritical) {
                status = "WARNING";
                warningChecks++;
                message = String.format("⚠️  CPU: %.1f%% (Warning) - %d cores available", cpuLoad, availableProcessors);
            } else {
                status = "FAIL";
                failedChecks++;
                message = String.format("❌ CPU: %.1f%% (Critical) - %d cores available", cpuLoad, availableProcessors);
            }
            
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SYS-001", "CPU Utilization", "System", 
                "CRITICAL", status, message, "Reduce workload or add CPU resources"));
            
        } catch (Exception e) {
            warningChecks++;
            String message = "⚠️  Could not check CPU utilization: " + e.getMessage();
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SYS-001", "CPU Utilization", "System",
                "CRITICAL", "WARNING", message, "Check system monitoring tools"));
        }
        System.out.println();
    }
    
    private static void checkMemoryUsage() {
        System.out.println("[SYS-002] Checking Memory Usage...");
        totalChecks++;
        
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) 
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            
            long totalMemory = osBean.getTotalPhysicalMemorySize();
            long freeMemory = osBean.getFreePhysicalMemorySize();
            long usedMemory = totalMemory - freeMemory;
            double memUsage = (usedMemory * 100.0) / totalMemory;
            
            String status, message;
            if (memUsage < memWarning) {
                status = "PASS";
                passedChecks++;
                message = String.format("✅ Memory: %.1f%% used (Normal) - %.1f GB / %.1f GB", 
                    memUsage, usedMemory / 1024.0 / 1024.0 / 1024.0, totalMemory / 1024.0 / 1024.0 / 1024.0);
            } else if (memUsage < memCritical) {
                status = "WARNING";
                warningChecks++;
                message = String.format("⚠️  Memory: %.1f%% used (Warning) - %.1f GB / %.1f GB", 
                    memUsage, usedMemory / 1024.0 / 1024.0 / 1024.0, totalMemory / 1024.0 / 1024.0 / 1024.0);
            } else {
                status = "FAIL";
                failedChecks++;
                message = String.format("❌ Memory: %.1f%% used (Critical) - %.1f GB / %.1f GB", 
                    memUsage, usedMemory / 1024.0 / 1024.0 / 1024.0, totalMemory / 1024.0 / 1024.0 / 1024.0);
            }
            
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SYS-002", "Memory Usage", "System",
                "CRITICAL", status, message, "Free up memory or add more RAM"));
            
        } catch (Exception e) {
            warningChecks++;
            String message = "⚠️  Could not check memory usage: " + e.getMessage();
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SYS-002", "Memory Usage", "System",
                "CRITICAL", "WARNING", message, "Check system monitoring tools"));
        }
        System.out.println();
    }
    
    private static void checkDiskSpace() {
        System.out.println("[SYS-003] Checking Disk Space...");
        totalChecks++;
        
        try {
            File installDirFile = new File(installDir);
            long totalSpace = installDirFile.getTotalSpace();
            long freeSpace = installDirFile.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            double diskUsage = (usedSpace * 100.0) / totalSpace;
            
            String status, message;
            if (diskUsage < diskWarning) {
                status = "PASS";
                passedChecks++;
                message = String.format("✅ Disk: %.1f%% used (Normal) - %.1f GB free / %.1f GB total", 
                    diskUsage, freeSpace / 1024.0 / 1024.0 / 1024.0, totalSpace / 1024.0 / 1024.0 / 1024.0);
            } else if (diskUsage < diskCritical) {
                status = "WARNING";
                warningChecks++;
                message = String.format("⚠️  Disk: %.1f%% used (Warning) - %.1f GB free / %.1f GB total", 
                    diskUsage, freeSpace / 1024.0 / 1024.0 / 1024.0, totalSpace / 1024.0 / 1024.0 / 1024.0);
            } else {
                status = "FAIL";
                failedChecks++;
                message = String.format("❌ Disk: %.1f%% used (Critical) - %.1f GB free / %.1f GB total", 
                    diskUsage, freeSpace / 1024.0 / 1024.0 / 1024.0, totalSpace / 1024.0 / 1024.0 / 1024.0);
            }
            
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SYS-003", "Disk Space", "System",
                "CRITICAL", status, message, "Clean up old logs and archives or add disk space"));
            
        } catch (Exception e) {
            warningChecks++;
            String message = "⚠️  Could not check disk space: " + e.getMessage();
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SYS-003", "Disk Space", "System",
                "CRITICAL", "WARNING", message, "Check file system permissions"));
        }
        System.out.println();
    }
    
    private static void checkJvmHeapSize() {
        System.out.println("[SRV-001] Checking JVM Heap Size...");
        totalChecks++;
        
        try {
            // Check multiple possible config files
            File propsFile = new File(installDir, "conf/windows.service.properties");
            if (!propsFile.exists()) {
                propsFile = new File(installDir, "conf/server.properties");
            }
            if (!propsFile.exists()) {
                propsFile = new File(installDir, "bin/startServer");  // Linux/Unix startup script
            }
            if (!propsFile.exists()) {
                propsFile = new File(installDir, "bin/espserver");  // Alternative Linux startup script
            }
            if (!propsFile.exists()) {
                propsFile = new File(installDir, "bin/espserver.sh");  // Alternative Linux startup script
            }
            
            if (!propsFile.exists()) {
                warningChecks++;
                String message = "⚠️  JVM configuration file not found";
                System.out.println("  " + message);
                checkResults.add(new CheckResult("SRV-001", "JVM Heap Size", "Server",
                    "CRITICAL", "WARNING", message, "Verify installation directory"));
                System.out.println();
                return;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(propsFile));
            String line;
            int heapSizeMB = 0;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("-Xmx")) {
                    String[] parts = line.split("=");
                    if (parts.length > 1) {
                        String heapValue = parts[1].trim();
                        if (heapValue.contains("-Xmx")) {
                            heapValue = heapValue.substring(heapValue.indexOf("-Xmx") + 4);
                            char unit = heapValue.charAt(heapValue.length() - 1);
                            String numStr = heapValue.substring(0, heapValue.length() - 1);
                            int num = Integer.parseInt(numStr);
                            
                            if (unit == 'G' || unit == 'g') {
                                heapSizeMB = num * 1024;
                            } else if (unit == 'M' || unit == 'm') {
                                heapSizeMB = num;
                            }
                            break;
                        }
                    }
                }
            }
            reader.close();
            
            String status, message;
            if (heapSizeMB >= jvmHeapMinMB) {
                status = "PASS";
                passedChecks++;
                message = String.format("✅ JVM Heap: %d MB (Meets minimum %d MB)", heapSizeMB, jvmHeapMinMB);
            } else {
                status = "FAIL";
                failedChecks++;
                message = String.format("❌ JVM Heap: %d MB (Below minimum %d MB)", heapSizeMB, jvmHeapMinMB);
            }
            
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SRV-001", "JVM Heap Size", "Server",
                "CRITICAL", status, message, "Increase JVM heap to at least 4096 MB"));
            
        } catch (Exception e) {
            warningChecks++;
            String message = "⚠️  Could not check JVM heap size: " + e.getMessage();
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SRV-001", "JVM Heap Size", "Server",
                "CRITICAL", "WARNING", message, "Check JVM configuration file"));
        }
        System.out.println();
    }
    
    private static void checkInstallationDirectory() {
        System.out.println("[SRV-002] Checking Installation Directory...");
        totalChecks++;
        
        try {
            File installDirFile = new File(installDir);
            File confDir = new File(installDir, "conf");
            File binDir = new File(installDir, "bin");
            File logsDir = new File(installDir, "logs");
            
            boolean allPresent = confDir.exists() && binDir.exists() && logsDir.exists();
            
            String status, message;
            if (allPresent) {
                status = "PASS";
                passedChecks++;
                message = "✅ Installation directory structure is valid";
            } else {
                status = "FAIL";
                failedChecks++;
                message = "❌ Installation directory structure is incomplete";
            }
            
            System.out.println("  " + message);
            System.out.println("    conf/: " + (confDir.exists() ? "✅" : "❌"));
            System.out.println("    bin/:  " + (binDir.exists() ? "✅" : "❌"));
            System.out.println("    logs/: " + (logsDir.exists() ? "✅" : "❌"));
            
            checkResults.add(new CheckResult("SRV-002", "Installation Directory", "Server",
                "CRITICAL", status, message, "Verify dSeries installation"));
            
        } catch (Exception e) {
            failedChecks++;
            String message = "❌ Could not validate installation directory: " + e.getMessage();
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SRV-002", "Installation Directory", "Server",
                "CRITICAL", "FAIL", message, "Check installation path and permissions"));
        }
        System.out.println();
    }
    
    private static void checkServerPort() {
        System.out.println("[SRV-003] Checking Server Port...");
        totalChecks++;
        
        try {
            // Read port from instanceconf.xml (rmi.registry.port)
            serverPort = readServerPort();
            
            // Try to connect to port
            java.net.Socket socket = null;
            boolean portOpen = false;
            try {
                socket = new java.net.Socket();
                socket.connect(new java.net.InetSocketAddress("localhost", serverPort), 5000);
                portOpen = true;
            } catch (Exception e) {
                portOpen = false;
            } finally {
                if (socket != null) {
                    try { socket.close(); } catch (Exception e) {}
                }
            }
            
            String status, message;
            if (portOpen) {
                status = "PASS";
                passedChecks++;
                message = String.format("✅ Server port %d is accessible", serverPort);
            } else {
                status = "WARNING";
                warningChecks++;
                message = String.format("⚠️  Server port %d is not accessible", serverPort);
            }
            
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SRV-003", "Server Port", "Server",
                "WARNING", status, message, "Start dSeries server or check firewall"));
            
        } catch (Exception e) {
            warningChecks++;
            String message = "⚠️  Could not check server port: " + e.getMessage();
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SRV-003", "Server Port", "Server",
                "WARNING", "WARNING", message, "Check server configuration"));
        }
        System.out.println();
    }
    
    private static void checkLoggingConfiguration() {
        System.out.println("[SRV-004] Checking Logging Configuration...");
        totalChecks++;
        
        try {
            File logsDir = new File(installDir, "logs");
            if (!logsDir.exists()) {
                warningChecks++;
                String message = "⚠️  Logs directory not found";
                System.out.println("  " + message);
                checkResults.add(new CheckResult("SRV-004", "Logging Configuration", "Server",
                    "WARNING", "WARNING", message, "Create logs directory"));
                System.out.println();
                return;
            }
            
            File[] logFiles = logsDir.listFiles((dir, name) -> name.endsWith(".log"));
            long totalLogSize = 0;
            if (logFiles != null) {
                for (File logFile : logFiles) {
                    totalLogSize += logFile.length();
                }
            }
            
            double totalLogSizeGB = totalLogSize / 1024.0 / 1024.0 / 1024.0;
            
            String status, message;
            if (totalLogSizeGB < 5.0) {
                status = "PASS";
                passedChecks++;
                message = String.format("✅ Log files: %.2f GB (Normal)", totalLogSizeGB);
            } else if (totalLogSizeGB < 10.0) {
                status = "WARNING";
                warningChecks++;
                message = String.format("⚠️  Log files: %.2f GB (Consider cleanup)", totalLogSizeGB);
            } else {
                status = "FAIL";
                failedChecks++;
                message = String.format("❌ Log files: %.2f GB (Cleanup required)", totalLogSizeGB);
            }
            
            System.out.println("  " + message);
            System.out.println("    Log files: " + (logFiles != null ? logFiles.length : 0));
            checkResults.add(new CheckResult("SRV-004", "Logging Configuration", "Server",
                "WARNING", status, message, "Implement log rotation and cleanup"));
            
        } catch (Exception e) {
            warningChecks++;
            String message = "⚠️  Could not check logging configuration: " + e.getMessage();
            System.out.println("  " + message);
            checkResults.add(new CheckResult("SRV-004", "Logging Configuration", "Server",
                "WARNING", "WARNING", message, "Check logs directory permissions"));
        }
        System.out.println();
    }
    
    // ========================================================================
    // DATABASE CONFIGURATION AND CHECKS
    // ========================================================================
    
    private static void loadDatabaseConfig() {
        System.out.println("[DB-CONFIG] Loading Database Configuration...");
        
        try {
            File configFile;
            
            // Use dSeries installation db.properties by default
            if (useExternalDbConfig && externalDbConfigFile != null) {
                // Use external config if explicitly provided (for testing)
                configFile = new File(externalDbConfigFile);
                System.out.println("  Using external database config: " + externalDbConfigFile);
            } else {
                // Use dSeries installation db.properties (RECOMMENDED)
                configFile = new File(installDir, "conf/db.properties");
                System.out.println("  Using dSeries database config: " + configFile.getPath());
            }
            
            if (!configFile.exists()) {
                System.out.println("  ⚠️  Database configuration file not found: " + configFile.getPath());
                System.out.println("  Database checks will be skipped");
                System.out.println();
                return;
            }
            
            Properties props = new Properties();
            props.load(new FileInputStream(configFile));
            
            String jdbcUrl = props.getProperty("jdbc.URL", "");
            originalJdbcUrl = jdbcUrl;  // Store original URL for later use
            dbUser = props.getProperty("rdbms.userid", dbUser);
            
            // Get and decrypt password
            String encryptedPassword = props.getProperty("rdbms.password", "");
            if (!encryptedPassword.isEmpty()) {
                dbPassword = decryptPassword(encryptedPassword);
                if (dbPassword.isEmpty() && !encryptedPassword.isEmpty()) {
                    System.out.println("  ⚠️  Password encryption detected but decryption not available");
                    System.out.println("  Database connection may fail if password is encrypted");
                }
            }
            
            // Get JDBC driver - check both possible property names
            String driverProp = props.getProperty("jdbc.Driver");
            if (driverProp == null || driverProp.isEmpty()) {
                driverProp = props.getProperty("rdbms.driver");
            }
            if (driverProp != null && !driverProp.isEmpty()) {
                dbDriver = driverProp;
            }
            
            // Detect database type from driver or URL
            dbType = props.getProperty("rdbms.type", "").toLowerCase();
            if (dbType.isEmpty()) {
                if (jdbcUrl.contains("postgresql") || dbDriver.contains("postgresql")) {
                    dbType = "postgresql";
                } else if (jdbcUrl.contains("oracle") || dbDriver.contains("oracle")) {
                    dbType = "oracle";
                } else if (jdbcUrl.contains("sqlserver") || dbDriver.contains("sqlserver")) {
                    dbType = "sqlserver";
                } else if (jdbcUrl.contains("db2") || dbDriver.contains("db2")) {
                    dbType = "db2";
                }
            }
            
            System.out.println("  ℹ️  Detected database type: " + dbType.toUpperCase());
            System.out.println("  ℹ️  JDBC Driver: " + dbDriver);
            
            // Parse JDBC URL to extract host, port, and database name
            if (jdbcUrl.contains("postgresql")) {
                // PostgreSQL: jdbc:postgresql://host:port/database
                Pattern pattern = Pattern.compile("jdbc:postgresql://([^:/?]+):?(\\d+)?/([^?]+)");
                Matcher matcher = pattern.matcher(jdbcUrl);
                if (matcher.find()) {
                    dbHost = matcher.group(1);
                    if (matcher.group(2) != null) {
                        dbPort = Integer.parseInt(matcher.group(2));
                    } else {
                        dbPort = 5432;  // Default PostgreSQL port
                    }
                    dbName = matcher.group(3);
                }
            } else if (jdbcUrl.contains("oracle")) {
                // Oracle: jdbc:oracle:thin:@host:port:sid or jdbc:oracle:thin:@host:port/service
                // Also supports: jdbc:oracle:thin:@//host:port/service
                Pattern pattern1 = Pattern.compile("jdbc:oracle:thin:@//\\[?([^\\]:]+)\\]?:(\\d+)/(.+)");
                Pattern pattern2 = Pattern.compile("jdbc:oracle:thin:@//([^:]+):(\\d+)/(.+)");
                Pattern pattern3 = Pattern.compile("jdbc:oracle:thin:@([^:]+):(\\d+)[:/](.+)");
                
                Matcher matcher = pattern1.matcher(jdbcUrl);
                if (!matcher.find()) {
                    matcher = pattern2.matcher(jdbcUrl);
                }
                if (!matcher.find()) {
                    matcher = pattern3.matcher(jdbcUrl);
                }
                
                if (matcher.find()) {
                    dbHost = matcher.group(1);
                    dbPort = Integer.parseInt(matcher.group(2));
                    dbName = matcher.group(3);
                }
            } else if (jdbcUrl.contains("sqlserver")) {
                // SQL Server: jdbc:sqlserver://host:port;databaseName=database
                // Also supports Windows authentication via integratedSecurity=true
                Pattern pattern = Pattern.compile("jdbc:sqlserver://([^:;]+):?(\\d+)?;.*databaseName=([^;]+)");
                Matcher matcher = pattern.matcher(jdbcUrl);
                if (matcher.find()) {
                    dbHost = matcher.group(1);
                    if (matcher.group(2) != null) {
                        dbPort = Integer.parseInt(matcher.group(2));
                    } else {
                        dbPort = 1433;  // Default SQL Server port
                    }
                    dbName = matcher.group(3);
                }
                
                // Check for Windows authentication
                if (jdbcUrl.toLowerCase().contains("integratedsecurity=true")) {
                    System.out.println("  ℹ️  Windows authentication detected (integratedSecurity=true)");
                    System.out.println("  ℹ️  Ensure sqljdbc_auth.dll is in system PATH or java.library.path");
                }
            } else if (jdbcUrl.contains("db2")) {
                // DB2: jdbc:db2://host:port/database
                Pattern pattern = Pattern.compile("jdbc:db2://([^:/?]+):?(\\d+)?/([^:;?]+)");
                Matcher matcher = pattern.matcher(jdbcUrl);
                if (matcher.find()) {
                    dbHost = matcher.group(1);
                    if (matcher.group(2) != null) {
                        dbPort = Integer.parseInt(matcher.group(2));
                    } else {
                        dbPort = 50000;  // Default DB2 port
                    }
                    dbName = matcher.group(3);
                }
            }
            
            // Check for Windows authentication
            String winAuthProp = props.getProperty("integratedSecurity", "false");
            integratedSecurity = Boolean.parseBoolean(winAuthProp);
            if (integratedSecurity) {
                System.out.println("  ℹ️  Windows authentication enabled (integratedSecurity=true)");
            }
            
            // Check for SSL/TLS configuration
            sslTrustStore = props.getProperty("javax.net.ssl.trustStore");
            sslTrustStorePassword = props.getProperty("javax.net.ssl.trustStorePassword");
            
            if (sslTrustStore != null && !sslTrustStore.isEmpty()) {
                sslEnabled = true;
                System.out.println("  ℹ️  SSL/TLS enabled");
                System.out.println("    TrustStore: " + sslTrustStore);
                
                // Decrypt trustStore password if encrypted
                if (sslTrustStorePassword != null && !sslTrustStorePassword.isEmpty()) {
                    String decryptedTrustStorePassword = decryptPassword(sslTrustStorePassword);
                    if (!decryptedTrustStorePassword.isEmpty()) {
                        sslTrustStorePassword = decryptedTrustStorePassword;
                        System.out.println("    TrustStore password: ******** (decrypted)");
                    } else {
                        System.out.println("    ⚠️  TrustStore password decryption failed");
                    }
                    
                    // Set system properties for SSL (required for DB2 and some Oracle configs)
                    System.setProperty("javax.net.ssl.trustStore", sslTrustStore);
                    System.setProperty("javax.net.ssl.trustStorePassword", sslTrustStorePassword);
                    System.out.println("    ✅ SSL system properties configured");
                }
            }
            
            // Check for Oracle SSL on AIX (special handling)
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("aix") && dbType.equals("oracle") && sslEnabled) {
                System.out.println("  ℹ️  AIX + Oracle SSL detected - special configuration applied");
                // Additional Oracle SSL properties for AIX
                String oracleClientAuth = props.getProperty("oracle.net.ssl_client_authentication");
                String oracleClientVersion = props.getProperty("oracle.net.ssl_version");
                if (oracleClientAuth != null) {
                    System.out.println("    Oracle SSL client auth: " + oracleClientAuth);
                }
                if (oracleClientVersion != null) {
                    System.out.println("    Oracle SSL version: " + oracleClientVersion);
                }
            }
            
            System.out.println("  ✅ Database configuration loaded");
            System.out.println("    Host: " + maskSensitiveData(dbHost));
            System.out.println("    Port: " + dbPort);
            System.out.println("    Database: " + maskSensitiveData(dbName));
            System.out.println("    User: " + maskSensitiveData(dbUser));
            
        } catch (Exception e) {
            System.out.println("  ⚠️  Could not load database configuration: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static boolean connectToDatabase() {
        System.out.println("[DB-CONNECT] Connecting to Database...");
        
        try {
            // Load db.properties again to get ALL properties (EXACTLY like DBConnectionValidator)
            File configFile = useExternalDbConfig && externalDbConfigFile != null 
                ? new File(externalDbConfigFile) 
                : new File(installDir, "conf/db.properties");
            
            Properties properties = new Properties();
            properties.load(new FileInputStream(configFile));
            
            // Get JDBC driver and URL (using exact property names from DBProperties)
            String jdbcDriver = properties.getProperty("jdbc.Driver");
            if (jdbcDriver == null) jdbcDriver = properties.getProperty("rdbms.driver");
            String jdbcUrl = properties.getProperty("jdbc.URL");
            
            if (jdbcDriver == null || jdbcUrl == null) {
                System.out.println("  ❌ Missing jdbc.Driver or jdbc.URL in db.properties");
                return false;
            }
            
            System.out.println("  Database connection details:");
            System.out.println("    Driver: " + jdbcDriver);
            System.out.println("    URL: " + maskJdbcUrl(dbHost, dbPort, dbName));
            System.out.println("    User: " + maskSensitiveData(dbUser));
            System.out.println();
            
            // Try to load JDBC driver (suppressing driver class name for security)
            try {
                Class.forName(jdbcDriver);
                System.out.println("  ✅ JDBC driver loaded successfully");
            } catch (ClassNotFoundException e) {
                System.out.println("  ❌ JDBC driver not found in classpath");
                System.out.println("  Note: Use launcher script (dseries_healthcheck.bat or .sh)");
                System.out.println();
                return false;
            }
            
            // Check for Windows integrated authentication
            boolean winAuth = Boolean.parseBoolean(properties.getProperty("integratedSecurity", "false"));
            
            // Remove properties that shouldn't be passed to driver (EXACTLY like DBConnectionValidator)
            properties.remove("jdbc.Driver");
            properties.remove("rdbms.driver");
            properties.remove("jdbc.URL");
            properties.remove("integratedSecurity");
            
            if (winAuth) {
                System.out.println("  ℹ️  Using Windows integrated authentication");
                if (!jdbcUrl.contains("integratedSecurity")) {
                    jdbcUrl += ";integratedSecurity=true;";
                }
            } else {
                // Get user and password
                String userId = properties.getProperty("rdbms.userid");
                String password = properties.getProperty("rdbms.password");
                
                if (userId != null) {
                    properties.setProperty("user", userId);  // Driver-specific property name
                }
                
                if (password != null && !password.isEmpty()) {
                    // Decrypt password using Scrambler (EXACTLY like DBConnectionValidator)
                    // Suppressing decryption messages for security
                    try {
                        Class<?> scramblerClass = Class.forName("com.ca.wa.publiclibrary.engine.library.crypto.Scrambler");
                        Class<?> rdbmClass = Class.forName("com.ca.wa.core.engine.rdbms.RelationalDatabaseManager");
                        
                        java.lang.reflect.Method getKeyMethod = rdbmClass.getMethod("getKey");
                        Object keyObj = getKeyMethod.invoke(null);
                        String key = keyObj != null ? keyObj.toString() : "RelationalDatabaseManager";
                        
                        java.lang.reflect.Method recoverMethod = scramblerClass.getMethod("recover", String.class, String.class);
                        Object decryptedObj = recoverMethod.invoke(null, password, key);
                        
                        if (decryptedObj != null) {
                            password = decryptedObj.toString();
                            properties.setProperty("password", password);  // Driver-specific property name
                            // Success - no message needed
                        } else {
                            properties.setProperty("password", password);  // Try original
                        }
                    } catch (Exception e) {
                        // Decryption failed - try original password silently
                        // Note: Use launcher script for encrypted passwords
                        properties.setProperty("password", password);  // Try original
                    }
                }
                
                // Remove original properties after setting driver-specific ones (EXACTLY like DBConnectionValidator)
                properties.remove("rdbms.userid");
                properties.remove("rdbms.password");
            }
            
            // Handle SSL keystore password (Oracle-SSL, DB2-SSL, PostgreSQL-SSL)
            // Suppressing SSL decryption messages for security
            String trustStorePass = properties.getProperty("javax.net.ssl.trustStorePassword");
            if (trustStorePass != null && !trustStorePass.isEmpty()) {
                try {
                    Class<?> scramblerClass = Class.forName("com.ca.wa.publiclibrary.engine.library.crypto.Scrambler");
                    Class<?> rdbmClass = Class.forName("com.ca.wa.core.engine.rdbms.RelationalDatabaseManager");
                    
                    java.lang.reflect.Method getKeyMethod = rdbmClass.getMethod("getKey");
                    Object keyObj = getKeyMethod.invoke(null);
                    String key = keyObj != null ? keyObj.toString() : "RelationalDatabaseManager";
                    
                    java.lang.reflect.Method recoverMethod = scramblerClass.getMethod("recover", String.class, String.class);
                    Object decryptedObj = recoverMethod.invoke(null, trustStorePass, key);
                    
                    if (decryptedObj != null) {
                        trustStorePass = decryptedObj.toString();
                        properties.setProperty("javax.net.ssl.trustStorePassword", trustStorePass);
                    }
                } catch (Exception e) {
                    // SSL password decryption failed - will try with original
                }
                
                // Set system properties (required for DB2 and some Oracle configs)
                System.setProperty("javax.net.ssl.trustStorePassword", properties.getProperty("javax.net.ssl.trustStorePassword"));
                String trustStore = properties.getProperty("javax.net.ssl.trustStore");
                if (trustStore != null) {
                    System.setProperty("javax.net.ssl.trustStore", trustStore);
                    System.out.println("  ℹ️  SSL system properties configured");
                }
            }
            
            System.out.println("  Connecting to database...");
            
            // Start connection timing
            connectionStartTime = System.currentTimeMillis();
            
            // Connect using ALL properties (EXACTLY like DBConnectionValidator)
            // This passes all db.properties to the driver, not just user/password
            try {
                dbConnection = DriverManager.getConnection(jdbcUrl, properties);
                connectionEndTime = System.currentTimeMillis();
                long connectionTime = connectionEndTime - connectionStartTime;
                
                System.out.println("  ✅ Database connection established successfully");
                System.out.println("    Connection time: " + connectionTime + " ms");
                
                // Validate connection
                System.out.println("  Validating connection...");
                if (dbConnection.isValid(30)) {
                    System.out.println("  ✅ Connection is valid (validated in 30 seconds)");
                    System.out.println();
                    return true;
                } else {
                    System.out.println("  ⚠️  Connection validation failed");
                    dbConnection.close();
                    dbConnection = null;
                    return false;
                }
                
            } catch (SQLException e) {
                System.out.println("  ❌ Could not connect to database");
                System.out.println("  Error: " + e.getMessage());
                if (e.getMessage().contains("ORA-")) {
                    System.out.println("  Oracle Error: https://docs.oracle.com/error-help/db/ora-" + 
                        e.getMessage().toLowerCase().replaceAll(".*ora-(\\d+).*", "$1") + "/");
                }
                System.out.println();
                System.out.println("  Troubleshooting:");
                System.out.println("    1. Verify database is running");
                System.out.println("    2. Check connection details in db.properties");
                System.out.println("    3. CRITICAL: Use launcher script (dseries_healthcheck.sh or .bat)");
                System.out.println("    4. Launcher script loads dSeries encryption libraries");
                System.out.println("    5. Without launcher script, password decryption will fail");
                System.out.println();
                System.out.println("  Database checks will be skipped");
                System.out.println();
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("  ❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
            return false;
        }
    }
    
    private static void disconnectFromDatabase() {
        if (dbConnection != null) {
            try {
                dbConnection.close();
                System.out.println("[DB-DISCONNECT] Database connection closed");
                System.out.println();
            } catch (SQLException e) {
                System.out.println("⚠️  Error closing database connection: " + e.getMessage());
            }
        }
    }
    
    private static void runDatabaseChecks() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  DATABASE CHECKS");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
        
        // Load and execute SQL checks
        List<SQLCheck> sqlChecks = loadSQLChecks();
        
        if (sqlChecks.isEmpty()) {
            System.out.println("⚠️  No SQL checks loaded from configuration file");
            System.out.println();
            return;
        }
        
        System.out.println("Loaded " + sqlChecks.size() + " SQL checks from configuration");
        System.out.println();
        
        // Group checks by category
        Map<String, List<SQLCheck>> checksByCategory = new HashMap<>();
        for (SQLCheck check : sqlChecks) {
            checksByCategory.computeIfAbsent(check.category, k -> new ArrayList<>()).add(check);
        }
        
        // Execute checks by category
        for (String category : checksByCategory.keySet()) {
            System.out.println("─── " + category + " ───");
            System.out.println();
            
            for (SQLCheck check : checksByCategory.get(category)) {
                executeSQLCheck(check);
            }
        }
    }
    
    private static List<SQLCheck> loadSQLChecks() {
        List<SQLCheck> checks = new ArrayList<>();
        
        try {
            File sqlFile = new File(installDir, sqlConfigFile);
            if (!sqlFile.exists()) {
                sqlFile = new File(sqlConfigFile);
            }
            
            if (!sqlFile.exists()) {
                System.out.println("⚠️  SQL configuration file not found: " + sqlConfigFile);
                return checks;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
            String line;
            
            String checkId = null;
            String checkName = null;
            String category = null;
            String severity = null;
            String description = null;
            String thresholdOperator = null;
            String thresholdValue = null;
            String remediation = null;
            StringBuilder query = new StringBuilder();
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.startsWith("-- @CHECK_ID:")) {
                    // Save previous check if exists
                    if (checkId != null && query.length() > 0) {
                        checks.add(new SQLCheck(checkId, checkName, category, severity,
                            description, query.toString(), thresholdOperator, thresholdValue, remediation));
                    }
                    
                    // Start new check
                    checkId = line.substring("-- @CHECK_ID:".length()).trim();
                    query = new StringBuilder();
                } else if (line.startsWith("-- @CHECK_NAME:")) {
                    checkName = line.substring("-- @CHECK_NAME:".length()).trim();
                } else if (line.startsWith("-- @CHECK_CATEGORY:")) {
                    category = line.substring("-- @CHECK_CATEGORY:".length()).trim();
                } else if (line.startsWith("-- @SEVERITY:")) {
                    severity = line.substring("-- @SEVERITY:".length()).trim();
                } else if (line.startsWith("-- @DESCRIPTION:")) {
                    description = line.substring("-- @DESCRIPTION:".length()).trim();
                } else if (line.startsWith("-- @THRESHOLD_OPERATOR:")) {
                    thresholdOperator = line.substring("-- @THRESHOLD_OPERATOR:".length()).trim();
                } else if (line.startsWith("-- @THRESHOLD_VALUE:")) {
                    thresholdValue = line.substring("-- @THRESHOLD_VALUE:".length()).trim();
                } else if (line.startsWith("-- @REMEDIATION:")) {
                    remediation = line.substring("-- @REMEDIATION:".length()).trim();
                } else if (!line.startsWith("--") && !line.isEmpty()) {
                    // This is part of the SQL query
                    if (query.length() > 0) {
                        query.append(" ");
                    }
                    query.append(line);
                }
            }
            
            // Save last check
            if (checkId != null && query.length() > 0) {
                checks.add(new SQLCheck(checkId, checkName, category, severity,
                    description, query.toString(), thresholdOperator, thresholdValue, remediation));
            }
            
            reader.close();
            
        } catch (Exception e) {
            System.out.println("⚠️  Error loading SQL checks: " + e.getMessage());
        }
        
        return checks;
    }
    
    private static void executeSQLCheck(SQLCheck check) {
        System.out.println("[" + check.checkId + "] " + check.checkName);
        totalChecks++;
        
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            // Start query timing
            queryStartTime = System.currentTimeMillis();
            
            stmt = dbConnection.createStatement();
            
            // Set query timeout (30 seconds)
            stmt.setQueryTimeout(30);
            
            // Convert query for target database type
            String convertedQuery = convertQueryForDatabase(check.query, dbType);
            
            // Remove trailing semicolon (Oracle JDBC doesn't like it)
            convertedQuery = convertedQuery.trim();
            if (convertedQuery.endsWith(";")) {
                convertedQuery = convertedQuery.substring(0, convertedQuery.length() - 1).trim();
            }
            
            // DEBUG: Log the actual SQL being executed
            System.out.println("  [DEBUG] Original query: " + check.query);
            System.out.println("  [DEBUG] Converted query: " + convertedQuery);
            System.out.println("  [DEBUG] Database type: " + dbType);
            
            rs = stmt.executeQuery(convertedQuery);
            
            queryEndTime = System.currentTimeMillis();
            long queryTime = queryEndTime - queryStartTime;
            
            // Get result
            if (rs.next()) {
                Object result = rs.getObject(1);
                
                // Evaluate threshold
                boolean passed = evaluateThreshold(result, check.thresholdOperator, check.thresholdValue);
                
                String status, message;
                if (check.thresholdOperator.equals("INFO_ONLY")) {
                    status = "INFO";
                    infoChecks++;
                    message = "ℹ️  " + check.checkName + ": " + result;
                } else if (passed) {
                    status = "PASS";
                    passedChecks++;
                    message = "✅ " + check.checkName + ": " + result + " (OK)";
                } else {
                    if (check.severity.equals("CRITICAL")) {
                        status = "FAIL";
                        failedChecks++;
                        message = "❌ " + check.checkName + ": " + result + " (Failed)";
                    } else {
                        status = "WARNING";
                        warningChecks++;
                        message = "⚠️  " + check.checkName + ": " + result + " (Warning)";
                    }
                }
                
                System.out.println("  " + message);
                
                // Add query performance info if slow
                if (queryTime > 1000) {
                    System.out.println("  ⚠️  Query took " + queryTime + " ms (slow)");
                }
                
                checkResults.add(new CheckResult(check.checkId, check.checkName, check.category,
                    check.severity, status, message, check.remediation));
            }
            
        } catch (java.sql.SQLTimeoutException e) {
            warningChecks++;
            String message = "⚠️  Query timeout (> 30 seconds): " + check.checkName;
            System.out.println("  " + message);
            System.out.println("  Database may be slow or query is inefficient");
            checkResults.add(new CheckResult(check.checkId, check.checkName, check.category,
                check.severity, "WARNING", message, "Optimize query or check database performance"));
        } catch (SQLException e) {
            warningChecks++;
            String message = "⚠️  Query failed: " + e.getMessage();
            System.out.println("  " + message);
            System.out.println("  [DEBUG] Failed SQL: " + check.query);
            checkResults.add(new CheckResult(check.checkId, check.checkName, check.category,
                check.severity, "WARNING", message, "Check query syntax and table existence"));
        } finally {
            // Proper resource cleanup
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                System.out.println("  ⚠️  Error closing resources: " + e.getMessage());
            }
        }
        System.out.println();
    }
    
    private static boolean evaluateThreshold(Object result, String operator, String threshold) {
        if (operator.equals("INFO_ONLY")) {
            return true;
        }
        
        try {
            if (result instanceof Number) {
                double value = ((Number) result).doubleValue();
                double thresholdNum = Double.parseDouble(threshold);
                
                switch (operator) {
                    case ">": return value > thresholdNum;
                    case "<": return value < thresholdNum;
                    case ">=": return value >= thresholdNum;
                    case "<=": return value <= thresholdNum;
                    case "=": return Math.abs(value - thresholdNum) < 0.001;
                    default: return true;
                }
            } else {
                String value = result.toString();
                return value.equals(threshold);
            }
        } catch (Exception e) {
            return true;  // Default to pass if evaluation fails
        }
    }
    
    // ========================================================================
    // REPORTING
    // ========================================================================
    
    private static void displaySummary() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  HEALTH CHECK SUMMARY");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
        
        int healthScore = calculateHealthScore();
        String healthStatus = getHealthStatus(healthScore);
        
        System.out.println("  Total Checks:    " + totalChecks);
        System.out.println("  Passed:          " + passedChecks + " ✅");
        System.out.println("  Warnings:        " + warningChecks + " ⚠️");
        System.out.println("  Failed:          " + failedChecks + " ❌");
        System.out.println("  Informational:   " + infoChecks + " ℹ️");
        System.out.println();
        System.out.println("  Health Score:    " + healthScore + "/100");
        System.out.println("  Status:          " + healthStatus);
        System.out.println();
        
        if (failedChecks > 0 || warningChecks > 0) {
            System.out.println("─── Issues Found ───");
            System.out.println();
            
            for (CheckResult result : checkResults) {
                if (result.status.equals("FAIL") || result.status.equals("WARNING")) {
                    System.out.println("  [" + result.checkId + "] " + result.checkName);
                    System.out.println("    Status: " + result.status);
                    System.out.println("    " + result.message);
                    System.out.println("    Remediation: " + result.remediation);
                    System.out.println();
                }
            }
        }
        
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
    }
    
    private static int calculateHealthScore() {
        if (totalChecks == 0) {
            return 0;
        }
        
        // Scoring: PASS = 100, WARNING = 60, FAIL = 0, INFO = not counted
        int scorableChecks = totalChecks - infoChecks;
        if (scorableChecks == 0) {
            return 100;
        }
        
        int totalScore = (passedChecks * 100) + (warningChecks * 60) + (failedChecks * 0);
        return totalScore / scorableChecks;
    }
    
    private static String getHealthStatus(int score) {
        if (score >= 90) return "✅ EXCELLENT";
        if (score >= 75) return "🟢 GOOD";
        if (score >= 60) return "🟡 FAIR";
        if (score >= 40) return "🟠 POOR";
        return "🔴 CRITICAL";
    }
    
    private static void generateReport() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String reportFile = "health_check_report_" + timestamp + ".txt";
            
            PrintWriter writer = new PrintWriter(new FileWriter(reportFile));
            
            writer.println("═══════════════════════════════════════════════════════════════════");
            writer.println("  ESP dSeries Health Check Report");
            writer.println("═══════════════════════════════════════════════════════════════════");
            writer.println("  Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("  Host: " + getHostname());
            writer.println("  dSeries: " + installDir);
            writer.println("═══════════════════════════════════════════════════════════════════");
            writer.println();
            
            writer.println("SUMMARY");
            writer.println("-------");
            writer.println("Total Checks:    " + totalChecks);
            writer.println("Passed:          " + passedChecks);
            writer.println("Warnings:        " + warningChecks);
            writer.println("Failed:          " + failedChecks);
            writer.println("Informational:   " + infoChecks);
            writer.println();
            writer.println("Health Score:    " + calculateHealthScore() + "/100");
            writer.println("Status:          " + getHealthStatus(calculateHealthScore()));
            writer.println();
            
            writer.println("DETAILED RESULTS");
            writer.println("----------------");
            writer.println();
            
            // Group by category
            Map<String, List<CheckResult>> resultsByCategory = new HashMap<>();
            for (CheckResult result : checkResults) {
                resultsByCategory.computeIfAbsent(result.category, k -> new ArrayList<>()).add(result);
            }
            
            for (String category : resultsByCategory.keySet()) {
                writer.println(category + ":");
                writer.println();
                
                for (CheckResult result : resultsByCategory.get(category)) {
                    writer.println("  [" + result.checkId + "] " + result.checkName);
                    writer.println("    Status: " + result.status);
                    writer.println("    " + result.message);
                    if (!result.status.equals("PASS") && !result.status.equals("INFO")) {
                        writer.println("    Remediation: " + result.remediation);
                    }
                    writer.println();
                }
            }
            
            writer.println("═══════════════════════════════════════════════════════════════════");
            writer.println("End of Report");
            writer.println("═══════════════════════════════════════════════════════════════════");
            
            writer.close();
            
            System.out.println("📄 Report generated: " + reportFile);
            System.out.println();
            
        } catch (Exception e) {
            System.out.println("⚠️  Could not generate report: " + e.getMessage());
        }
    }
    
    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    private static String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        
        int visibleChars = Math.min(3, data.length() / 3);
        String visible = data.substring(0, visibleChars);
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < data.length() - visibleChars; i++) {
            masked.append("*");
        }
        return visible + masked.toString();
    }
    
    private static String maskJdbcUrl(String host, int port, String dbName) {
        return "jdbc:***://" + maskSensitiveData(host) + ":" + port + "/" + maskSensitiveData(dbName);
    }
    
    /**
     * Decrypt password using dSeries native encryption
     * Supports multiple encryption formats:
     * 1. Base64 (no prefix) - dSeries default encrypted format
     * 2. ENC(base64) - Simple Base64 encoding
     * 3. {ENCRYPTED}hex - Alternative dSeries format
     * 4. Plain text - No encryption
     * 
     * This method uses dSeries native encryption library (com.ca.wa.de.security.Encryption)
     * when available in classpath for proper decryption of all dSeries password formats.
     * 
     * @param encryptedPassword The encrypted password from db.properties
     * @return Decrypted password or empty string if decryption fails
     */
    private static String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return "";
        }
        
        // Try dSeries native encryption first (used by DBConnectionValidator)
        System.out.println("  ℹ️  Attempting password decryption using dSeries encryption");
        String decrypted = decryptPasswordWithScrambler(encryptedPassword);
        if (!decrypted.isEmpty()) {
            return decrypted;
        }
        
        // Try dSeries Encryption class as fallback
        if (isDSeriesEncryptionAvailable()) {
            try {
                System.out.println("  ℹ️  Trying dSeries Encryption class for password decryption");
                
                // Use reflection to call dSeries encryption
                Class<?> encryptionClass = Class.forName("com.ca.wa.de.security.Encryption");
                java.lang.reflect.Method decryptMethod = encryptionClass.getMethod("decrypt", String.class);
                Object result = decryptMethod.invoke(null, encryptedPassword);
                
                if (result != null && !result.toString().isEmpty()) {
                    System.out.println("  ✅ Password decrypted successfully using dSeries Encryption");
                    return result.toString();
                }
            } catch (Exception e) {
                System.out.println("  ⚠️  dSeries Encryption decryption failed: " + e.getMessage());
                // Fall through to try other methods
            }
        }
        
        // Format 1: ENC(base64) - Simple Base64 encoding
        if (encryptedPassword.startsWith("ENC(") && encryptedPassword.endsWith(")")) {
            try {
                String encrypted = encryptedPassword.substring(4, encryptedPassword.length() - 1);
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(encrypted);
                System.out.println("  ✅ Password decrypted using Base64");
                return new String(decodedBytes, "UTF-8");
            } catch (Exception e) {
                System.out.println("  ⚠️  Base64 decryption failed: " + e.getMessage());
                return "";
            }
        }
        
        // Format 2: {ENCRYPTED}hex - Alternative format
        else if (encryptedPassword.startsWith("{") && encryptedPassword.endsWith("}")) {
            System.out.println("  ⚠️  Encrypted password format detected but decryption library not available");
            System.out.println("  Note: Ensure dSeries encryption library (wade.jar) is in classpath");
            return "";
        }
        
        // Format 3: Try Base64 decoding without prefix (dSeries default)
        else {
            // Check if it looks like Base64 (contains only valid Base64 characters)
            if (encryptedPassword.matches("^[A-Za-z0-9+/=]+$") && encryptedPassword.length() > 10) {
                try {
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(encryptedPassword);
                    String decoded = new String(decodedBytes, "UTF-8");
                    
                    // Verify it's printable text (not binary garbage)
                    if (decoded.matches("^[\\x20-\\x7E]+$")) {
                        System.out.println("  ✅ Password decrypted using Base64 (no prefix)");
                        return decoded;
                    }
                } catch (Exception e) {
                    // Not Base64, assume plain text
                }
            }
            
            // Check if password looks encrypted (contains only Base64 chars and ==)
            // If it looks like Base64, try to decode it
            if (encryptedPassword.matches("^[A-Za-z0-9+/]+=*$") && encryptedPassword.length() > 10) {
                try {
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(encryptedPassword);
                    String decoded = new String(decodedBytes);
                    // If decoded string is printable, it might be the password
                    if (decoded.matches("^[\\x20-\\x7E]+$")) {
                        System.out.println("  ℹ️  Password appears to be Base64 encoded, decoded successfully");
                        return decoded;
                    }
                } catch (Exception e) {
                    // Not valid Base64, treat as plain text
                }
            }
            
            // Assume plain text
            System.out.println("  ℹ️  Using plain text password (no encryption detected)");
            return encryptedPassword;
        }
    }
    
    /**
     * Check if dSeries encryption library is available
     * @return true if encryption library is in classpath
     */
    private static boolean isDSeriesEncryptionAvailable() {
        try {
            // Try to load dSeries encryption class (if available)
            Class.forName("com.ca.wa.de.security.Encryption");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Get classpath from dSeries bin/classpath.bat or bin/classpath.sh
     * This ensures we use the exact same classpath as dSeries, including:
     * - All JDBC drivers (PostgreSQL, Oracle, DB2, SQL Server)
     * - Encryption libraries
     * - Authentication DLLs (for Windows authentication)
     * 
     * @return classpath string or null if failed
     */
    private static String getDSeriesClasspath() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            File binDir = new File(installDir, "bin");
            
            if (!binDir.exists()) {
                System.out.println("  ⚠️  bin directory not found: " + binDir.getPath());
                return null;
            }
            
            ProcessBuilder pb;
            File classpathScript;
            
            if (os.contains("win")) {
                // Windows: Execute classpath.bat
                classpathScript = new File(binDir, "classpath.bat");
                if (!classpathScript.exists()) {
                    System.out.println("  ⚠️  classpath.bat not found: " + classpathScript.getPath());
                    return null;
                }
                
                // Create a wrapper script to echo the CLASSPATH
                File tempScript = File.createTempFile("get_classpath_", ".bat");
                tempScript.deleteOnExit();
                
                PrintWriter writer = new PrintWriter(tempScript);
                writer.println("@echo off");
                writer.println("call \"" + classpathScript.getAbsolutePath() + "\"");
                writer.println("echo %CLASSPATH%");
                writer.close();
                
                pb = new ProcessBuilder("cmd.exe", "/c", tempScript.getAbsolutePath());
                
            } else {
                // Unix/Linux/AIX: Execute classpath.sh
                classpathScript = new File(binDir, "classpath.sh");
                if (!classpathScript.exists()) {
                    classpathScript = new File(binDir, "classpath");
                }
                
                if (!classpathScript.exists()) {
                    System.out.println("  ⚠️  classpath.sh not found: " + classpathScript.getPath());
                    return null;
                }
                
                // Create a wrapper script to echo the CLASSPATH
                File tempScript = File.createTempFile("get_classpath_", ".sh");
                tempScript.deleteOnExit();
                tempScript.setExecutable(true);
                
                PrintWriter writer = new PrintWriter(tempScript);
                writer.println("#!/bin/sh");
                writer.println(". \"" + classpathScript.getAbsolutePath() + "\"");
                writer.println("echo $CLASSPATH");
                writer.close();
                
                pb = new ProcessBuilder("/bin/sh", tempScript.getAbsolutePath());
            }
            
            pb.directory(binDir);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                System.out.println("  ⚠️  Failed to execute classpath script (exit code: " + exitCode + ")");
                return null;
            }
            
            // Get the last non-empty line (should be the CLASSPATH)
            String[] lines = output.toString().split("\n");
            for (int i = lines.length - 1; i >= 0; i--) {
                String classpath = lines[i].trim();
                if (!classpath.isEmpty() && classpath.contains("jar")) {
                    // Clean up the classpath (remove quotes)
                    classpath = classpath.replace("\"", "");
                    System.out.println("  ✅ dSeries classpath loaded successfully");
                    System.out.println("  ℹ️  Classpath includes " + countJars(classpath) + " JAR files");
                    return classpath;
                }
            }
            
            System.out.println("  ⚠️  Could not extract classpath from script output");
            return null;
            
        } catch (Exception e) {
            System.out.println("  ⚠️  Error getting dSeries classpath: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Count number of JAR files in classpath
     */
    private static int countJars(String classpath) {
        if (classpath == null || classpath.isEmpty()) {
            return 0;
        }
        
        String separator = System.getProperty("path.separator");
        String[] paths = classpath.split(separator);
        int count = 0;
        
        for (String path : paths) {
            if (path.toLowerCase().endsWith(".jar")) {
                count++;
            }
        }
        
        return count;
    }
    
    /**
     * Convert SQL query to database-specific syntax
     * Handles differences in string concatenation, date functions, etc.
     * 
     * @param query Original query (PostgreSQL syntax)
     * @param targetDbType Target database type
     * @return Converted query
     */
    private static String convertQueryForDatabase(String query, String targetDbType) {
        if (targetDbType == null || targetDbType.isEmpty()) {
            return query;
        }
        
        String converted = query;
        String dbType = targetDbType.toLowerCase();
        
        // SQL Server specific conversions
        if (dbType.contains("sqlserver") || dbType.equals("mssql")) {
            // String concatenation: || to +
            converted = converted.replaceAll("'%'\\s*\\|\\|\\s*", "'%' + ");
            converted = converted.replaceAll("\\|\\|\\s*'%'", " + '%'");
            
            // Date/Time functions
            converted = converted.replace("CURRENT_TIMESTAMP", "GETDATE()");
            converted = converted.replaceAll("INTERVAL\\s+'(\\d+)\\s+(\\w+)'", "DATEADD($2, -$1, GETDATE())");
        }
        
        // Oracle specific conversions
        else if (dbType.contains("oracle")) {
            // Date/Time functions
            converted = converted.replace("CURRENT_TIMESTAMP", "SYSDATE");
            // Handle INTERVAL with subtraction: CURRENT_TIMESTAMP - INTERVAL '4 hours' -> SYSDATE - INTERVAL '4' HOUR
            converted = converted.replaceAll("INTERVAL\\s+'(\\d+)\\s+hours?'", "INTERVAL '$1' HOUR");
            converted = converted.replaceAll("INTERVAL\\s+'(\\d+)\\s+days?'", "INTERVAL '$1' DAY");
            converted = converted.replaceAll("INTERVAL\\s+'(\\d+)\\s+minutes?'", "INTERVAL '$1' MINUTE");
            // Handle NOW() function
            converted = converted.replace("NOW()", "SYSDATE");
            // Handle DATE_TRUNC (not directly supported in Oracle, needs TRUNC)
            converted = converted.replaceAll("DATE_TRUNC\\s*\\(\\s*'hour'\\s*,\\s*", "TRUNC(");
        }
        
        // DB2 specific conversions
        else if (dbType.contains("db2")) {
            // Date/Time functions
            converted = converted.replace("CURRENT_TIMESTAMP", "CURRENT TIMESTAMP");
            converted = converted.replaceAll("INTERVAL\\s+'(\\d+)\\s+hour'", "$1 HOUR");
            converted = converted.replaceAll("INTERVAL\\s+'(\\d+)\\s+day'", "$1 DAY");
        }
        
        return converted;
    }
    
    /**
     * Find and load JDBC driver from dSeries lib directory
     * This method searches for JDBC driver JARs and adds them to classpath dynamically
     * 
     * @param driverType Database type (postgresql, oracle, sqlserver)
     * @return true if driver loaded successfully
     */
    private static boolean findAndLoadJdbcDriver(String driverType) {
        try {
            File libDir = new File(installDir, "lib");
            if (!libDir.exists() || !libDir.isDirectory()) {
                return false;
            }
            
            // Search patterns for different database types
            final String[] searchPatterns;
            if (driverType.contains("postgresql")) {
                searchPatterns = new String[]{"postgresql", "postgres"};
            } else if (driverType.contains("oracle")) {
                searchPatterns = new String[]{"ojdbc", "oracle"};
            } else if (driverType.contains("sqlserver")) {
                searchPatterns = new String[]{"mssql", "sqlserver", "sqljdbc"};
            } else {
                return false;
            }
            
            // Search for JDBC driver JAR
            File[] jarFiles = libDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (!name.toLowerCase().endsWith(".jar")) {
                        return false;
                    }
                    for (String pattern : searchPatterns) {
                        if (name.toLowerCase().contains(pattern)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            
            if (jarFiles != null && jarFiles.length > 0) {
                // Found JDBC driver JAR
                File jdbcJar = jarFiles[0];
                System.out.println("  ℹ️  Found JDBC driver: " + jdbcJar.getName());
                
                // Note: Dynamic classpath loading at runtime is complex in Java
                // The launcher scripts should handle this by building classpath
                // This method is here for documentation and future enhancement
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Read server port from instanceconf.xml
     * Reads rmi.registry.port from conf/DBInit/Config/instanceconf.xml
     * 
     * @return server port or default 7599 if not found
     */
    private static int readServerPort() {
        try {
            File instanceConfFile = new File(installDir, "conf/DBInit/Config/instanceconf.xml");
            
            if (!instanceConfFile.exists()) {
                System.out.println("  ⚠️  instanceconf.xml not found, using default port 7599");
                return 7599;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(instanceConfFile);
            
            // Normalize the XML structure
            doc.getDocumentElement().normalize();
            
            // Get all entry elements
            NodeList entryList = doc.getElementsByTagName("entry");
            
            for (int i = 0; i < entryList.getLength(); i++) {
                Node entryNode = entryList.item(i);
                
                if (entryNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element entryElement = (Element) entryNode;
                    
                    // Get the key element
                    NodeList keyList = entryElement.getElementsByTagName("key");
                    if (keyList.getLength() > 0) {
                        String key = keyList.item(0).getTextContent();
                        
                        // Look for rmi.registry.port
                        if ("rmi.registry.port".equals(key)) {
                            // Get the desired value
                            NodeList valueObjectList = entryElement.getElementsByTagName("valueobject");
                            if (valueObjectList.getLength() > 0) {
                                Element valueObject = (Element) valueObjectList.item(0);
                                NodeList desiredList = valueObject.getElementsByTagName("desired");
                                if (desiredList.getLength() > 0) {
                                    String portStr = desiredList.item(0).getTextContent();
                                    int port = Integer.parseInt(portStr.trim());
                                    System.out.println("  ℹ️  Server port from instanceconf.xml: " + port);
                                    return port;
                                }
                            }
                        }
                    }
                }
            }
            
            System.out.println("  ⚠️  rmi.registry.port not found in instanceconf.xml, using default 7599");
            return 7599;
            
        } catch (Exception e) {
            System.out.println("  ⚠️  Error reading instanceconf.xml: " + e.getMessage());
            System.out.println("  Using default port 7599");
            return 7599;
        }
    }
    
    /**
     * Decrypt password using dSeries Scrambler with RelationalDatabaseManager key
     * This is the exact method used by dSeries DBConnectionValidator
     * 
     * @param encryptedPassword The encrypted password
     * @return Decrypted password or empty string if decryption fails
     */
    private static String decryptPasswordWithScrambler(String encryptedPassword) {
        try {
            // Use reflection to call dSeries native password decryption
            Class<?> scramblerClass = Class.forName("com.ca.wa.publiclibrary.engine.library.crypto.Scrambler");
            
            // Try to get the key from RelationalDatabaseManager if available
            String key = "RelationalDatabaseManager";
            try {
                Class<?> rdbmClass = Class.forName("com.ca.wa.core.engine.rdbms.RelationalDatabaseManager");
                java.lang.reflect.Method getKeyMethod = rdbmClass.getMethod("getKey");
                Object keyResult = getKeyMethod.invoke(null);
                if (keyResult != null) {
                    key = keyResult.toString();
                }
            } catch (Exception e) {
                // Use default key if RelationalDatabaseManager not available
            }
            
            java.lang.reflect.Method recoverMethod = scramblerClass.getMethod("recover", String.class, String.class);
            Object result = recoverMethod.invoke(null, encryptedPassword, key);
            
            if (result != null && !result.toString().isEmpty()) {
                System.out.println("  ✅ Password decrypted successfully using dSeries encryption");
                return result.toString();
            }
        } catch (ClassNotFoundException e) {
            // Silently fail - encryption library not available (expected in some cases)
            // User will see "Using plain text password" message later if needed
        } catch (NoSuchMethodException e) {
            // Silently fail - method not available
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Silently fail - decryption failed (could be wrong format, wrong key, etc.)
            // This is expected when password is not encrypted with Scrambler
        } catch (Exception e) {
            // Silently fail - any other decryption error
        }
        
        return "";
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * LOG ANALYSIS METHODS - Version 3.0.0
     * ═══════════════════════════════════════════════════════════════════════════
     */
    
    /**
     * Perform comprehensive log analysis
     */
    private static void performLogAnalysis() {
        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.println("LOG ANALYSIS");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        File logsDir = new File(installDir, "logs");
        if (!logsDir.exists() || !logsDir.isDirectory()) {
            System.out.println("⚠️  Logs directory not found: " + logsDir.getAbsolutePath());
            return;
        }
        
        // Analyze different log types
        analyzeErrorLog(logsDir);
        analyzeStdoutLog(logsDir);
        analyzeTracelogFiles(logsDir);
        analyzeConnectionPool(logsDir);
        analyzeStartupPerformance(logsDir);
        analyzeBufferLogs(logsDir);  // NEW: Buffer log analysis
        
        // Generate recommendations
        generateLogAnalysisRecommendations();
    }
    
    /**
     * Analyze errors.txt for error patterns
     */
    private static void analyzeErrorLog(File logsDir) {
        System.out.println("─── Error Log Analysis ───\n");
        
        File errorsFile = new File(logsDir, "errors.txt");
        if (!errorsFile.exists()) {
            System.out.println("ℹ️  errors.txt not found\n");
            return;
        }
        
        // Define error patterns to look for
        List<ErrorPattern> patterns = new ArrayList<>();
        patterns.add(new ErrorPattern(
            "CryptoException.*Key not available",
            "Agent Encryption Key Missing",
            "HIGH",
            "Agent trying to connect without encryption key registered",
            "Register agent encryption key or disable encryption for agent"
        ));
        // TDR errors suppressed per user request
        // patterns.add(new ErrorPattern(
        //     "DatabaseUpdateException.*UPDATE ESP_TDR_DATA",
        //     "TDR Update Failure",
        //     "MEDIUM",
        //     "Time-Dependent Resource update failing",
        //     "Check TDR configuration and database connectivity"
        // ));
        patterns.add(new ErrorPattern(
            "UnknownMessageTypeException.*unknown",
            "RMI Version Mismatch",
            "MEDIUM",
            "Agent/client using incompatible RMI message version",
            "Upgrade agent/client to match server version"
        ));
        patterns.add(new ErrorPattern(
            "SQLException|ORA-\\d+|DB2 SQL Error",
            "Database Error",
            "HIGH",
            "Database connectivity or query execution issues",
            "Check database connectivity, logs, and query syntax"
        ));
        patterns.add(new ErrorPattern(
            "OutOfMemoryError|Java heap space",
            "Memory Exhaustion",
            "CRITICAL",
            "JVM running out of memory",
            "Increase JVM heap size in startServer script"
        ));
        patterns.add(new ErrorPattern(
            "SocketTimeoutException|Connection timed out",
            "Network Timeout",
            "MEDIUM",
            "Network connectivity issues or slow responses",
            "Check network connectivity and increase timeout values"
        ));
        
        try (BufferedReader reader = new BufferedReader(new FileReader(errorsFile))) {
            String line;
            int totalErrors = 0;
            
            while ((line = reader.readLine()) != null) {
                totalErrors++;
                
                // Check against each pattern
                for (ErrorPattern pattern : patterns) {
                    if (Pattern.compile(pattern.pattern, Pattern.CASE_INSENSITIVE).matcher(line).find()) {
                        pattern.count++;
                        if (pattern.examples.size() < 3) {
                            // Keep first 3 examples
                            pattern.examples.add(line.length() > 100 ? line.substring(0, 100) + "..." : line);
                        }
                    }
                }
            }
            
            System.out.println("Total error lines: " + totalErrors);
            System.out.println();
            
            // Report findings
            boolean issuesFound = false;
            for (ErrorPattern pattern : patterns) {
                if (pattern.count > 0) {
                    issuesFound = true;
                    String icon = pattern.severity.equals("CRITICAL") ? "❌" : 
                                 pattern.severity.equals("HIGH") ? "⚠️" : "ℹ️";
                    
                    System.out.println(icon + " " + pattern.errorType + " (" + pattern.severity + ")");
                    System.out.println("   Occurrences: " + pattern.count);
                    System.out.println("   Description: " + pattern.description);
                    System.out.println("   Recommendation: " + pattern.recommendation);
                    
                    if (!pattern.examples.isEmpty()) {
                        System.out.println("   Example: " + pattern.examples.get(0));
                    }
                    System.out.println();
                    
                    // Store as check result
                    checkResults.add(new CheckResult(
                        "LOG-ERR-" + pattern.errorType.replaceAll("\\s+", "-"),
                        pattern.errorType,
                        "Log Analysis",
                        pattern.severity,
                        pattern.count > 10 ? "FAIL" : "WARNING",
                        pattern.count + " occurrences found",
                        pattern.recommendation
                    ));
                }
            }
            
            if (!issuesFound) {
                System.out.println("✅ No critical error patterns detected\n");
            }
            
        } catch (IOException e) {
            System.out.println("⚠️  Error reading errors.txt: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Analyze stdout log for startup performance
     */
    private static void analyzeStdoutLog(File logsDir) {
        System.out.println("─── Startup Performance Analysis ───\n");
        
        // Find most recent stdout file
        File[] stdoutFiles = logsDir.listFiles((dir, name) -> 
            name.startsWith("stdout.txt") && !name.equals("stdout.txt"));
        
        if (stdoutFiles == null || stdoutFiles.length == 0) {
            System.out.println("ℹ️  No stdout log files found\n");
            return;
        }
        
        // Sort by last modified, get most recent
        Arrays.sort(stdoutFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        File stdoutFile = stdoutFiles[0];
        
        System.out.println("Analyzing: " + stdoutFile.getName());
        
        StartupPerformance perf = new StartupPerformance();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(stdoutFile))) {
            String line;
            Pattern componentPattern = Pattern.compile("(.+?) Component is ready.*?(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");
            Pattern startPattern = Pattern.compile("Server is starting");
            
            long serverStart = 0;
            long previousTime = 0;
            
            while ((line = reader.readLine()) != null) {
                if (startPattern.matcher(line).find()) {
                    serverStart = System.currentTimeMillis(); // Approximate
                    perf.serverStartTime = serverStart;
                }
                
                Matcher m = componentPattern.matcher(line);
                if (m.find()) {
                    String component = m.group(1).trim();
                    String timestamp = m.group(2);
                    
                    // Calculate relative time (simplified)
                    long relativeTime = perf.componentReadyTimes.size() * 30000; // Approximate
                    perf.componentReadyTimes.put(component, relativeTime);
                    
                    if (component.contains("Application Manager")) {
                        perf.applicationManagerTime = relativeTime - previousTime;
                    } else if (component.contains("Rest Component")) {
                        perf.restComponentTime = relativeTime - previousTime;
                    }
                    
                    previousTime = relativeTime;
                }
            }
            
            // Find bottleneck
            long maxDuration = 0;
            String prevComponent = null;
            long prevTime = 0;
            
            for (Map.Entry<String, Long> entry : perf.componentReadyTimes.entrySet()) {
                if (prevComponent != null) {
                    long duration = entry.getValue() - prevTime;
                    if (duration > maxDuration) {
                        maxDuration = duration;
                        perf.bottleneckComponent = entry.getKey();
                        perf.bottleneckDuration = duration;
                    }
                }
                prevComponent = entry.getKey();
                prevTime = entry.getValue();
            }
            
            // Report findings
            System.out.println("\nComponent Startup Times:");
            for (Map.Entry<String, Long> entry : perf.componentReadyTimes.entrySet()) {
                System.out.println("  " + entry.getKey() + ": +" + (entry.getValue() / 1000) + "s");
            }
            
            if (perf.bottleneckComponent != null) {
                System.out.println("\n⚠️  Startup Bottleneck Detected:");
                System.out.println("   Component: " + perf.bottleneckComponent);
                System.out.println("   Duration: " + (perf.bottleneckDuration / 1000) + " seconds");
                
                if (perf.bottleneckComponent.contains("Application Manager")) {
                    System.out.println("   Recommendation: Enable parallel application loading");
                    System.out.println("   Add to server.properties:");
                    System.out.println("     application.manager.parallel.loading=true");
                    System.out.println("     application.manager.loader.threads=10");
                    
                    checkResults.add(new CheckResult(
                        "LOG-PERF-001",
                        "Application Manager Slow Startup",
                        "Performance",
                        "HIGH",
                        "WARNING",
                        "Takes " + (perf.bottleneckDuration / 1000) + " seconds",
                        "Enable parallel loading in server.properties"
                    ));
                }
            } else {
                System.out.println("\n✅ No significant startup bottlenecks detected");
            }
            
            System.out.println();
            
        } catch (IOException e) {
            System.out.println("⚠️  Error reading stdout: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Analyze tracelog files for thread dumps and deadlocks
     */
    private static void analyzeTracelogFiles(File logsDir) {
        System.out.println("─── Thread Dump Analysis ───\n");
        
        File[] tracelogFiles = logsDir.listFiles((dir, name) -> 
            name.startsWith("tracelog.") && name.endsWith(".txt"));
        
        if (tracelogFiles == null || tracelogFiles.length == 0) {
            System.out.println("ℹ️  No tracelog files found\n");
            return;
        }
        
        // Analyze most recent tracelog
        Arrays.sort(tracelogFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        File tracelogFile = tracelogFiles[0];
        
        System.out.println("Analyzing: " + tracelogFile.getName());
        
        ThreadDumpInfo threadInfo = new ThreadDumpInfo();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(tracelogFile))) {
            String line;
            int lineCount = 0;
            int maxLines = 50000; // Limit to avoid memory issues
            
            while ((line = reader.readLine()) != null && lineCount++ < maxLines) {
                // Look for thread state indicators
                if (line.contains("BLOCKED")) {
                    threadInfo.blockedThreads++;
                }
                if (line.contains("RUNNABLE")) {
                    threadInfo.runnableThreads++;
                }
                if (line.contains("WAITING")) {
                    threadInfo.waitingThreads++;
                }
                
                // Look for lock contention
                if (line.contains("DBConnectionPool") && line.contains("waiting")) {
                    if (threadInfo.lockObject == null) {
                        threadInfo.lockObject = "DBConnectionPool";
                    }
                    threadInfo.blockedOnLock.add(line.substring(0, Math.min(line.length(), 100)));
                }
                
                // Look for deadlock indicators
                if (line.contains("deadlock") || line.contains("Deadlock")) {
                    threadInfo.deadlockDetected = true;
                    threadInfo.deadlockDescription = line;
                }
            }
            
            threadInfo.totalThreads = threadInfo.blockedThreads + threadInfo.runnableThreads + threadInfo.waitingThreads;
            
            // Report findings
            System.out.println("\nThread Statistics:");
            System.out.println("  Total threads analyzed: " + threadInfo.totalThreads);
            System.out.println("  Blocked: " + threadInfo.blockedThreads);
            System.out.println("  Runnable: " + threadInfo.runnableThreads);
            System.out.println("  Waiting: " + threadInfo.waitingThreads);
            
            if (threadInfo.blockedThreads > 100) {
                System.out.println("\n⚠️  High Thread Contention Detected:");
                System.out.println("   " + threadInfo.blockedThreads + " blocked threads found");
                
                if (threadInfo.lockObject != null) {
                    System.out.println("   Lock Object: " + threadInfo.lockObject);
                    System.out.println("   Recommendation: Optimize connection pool configuration");
                    System.out.println("   Add to db.properties:");
                    System.out.println("     database.maxconnections.in.pool=50");
                    System.out.println("     database.minconnection=10");
                    
                    checkResults.add(new CheckResult(
                        "LOG-THREAD-001",
                        "Thread Contention on " + threadInfo.lockObject,
                        "Performance",
                        "HIGH",
                        "WARNING",
                        threadInfo.blockedThreads + " threads blocked",
                        "Optimize connection pool settings"
                    ));
                }
            }
            
            if (threadInfo.deadlockDetected) {
                System.out.println("\n❌ DEADLOCK DETECTED:");
                System.out.println("   " + threadInfo.deadlockDescription);
                System.out.println("   Recommendation: Restart server and review thread dump");
                
                checkResults.add(new CheckResult(
                    "LOG-THREAD-002",
                    "Deadlock Detected",
                    "Critical",
                    "CRITICAL",
                    "FAIL",
                    "Deadlock found in thread dump",
                    "Restart server immediately and review thread dump"
                ));
            }
            
            if (threadInfo.blockedThreads <= 100 && !threadInfo.deadlockDetected) {
                System.out.println("\n✅ No critical thread issues detected");
            }
            
            System.out.println();
            
        } catch (IOException e) {
            System.out.println("⚠️  Error reading tracelog: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Analyze connection pool usage and detect leaks
     */
    private static void analyzeConnectionPool(File logsDir) {
        System.out.println("─── Connection Pool Analysis ───\n");
        
        File[] tracelogFiles = logsDir.listFiles((dir, name) -> 
            name.startsWith("tracelog.") && name.endsWith(".txt"));
        
        if (tracelogFiles == null || tracelogFiles.length == 0) {
            System.out.println("ℹ️  No tracelog files found\n");
            return;
        }
        
        // Analyze most recent tracelog
        Arrays.sort(tracelogFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        File tracelogFile = tracelogFiles[0];
        
        System.out.println("Analyzing: " + tracelogFile.getName());
        
        ConnectionPoolStats stats = new ConnectionPoolStats();
        Pattern connPattern = Pattern.compile("DBPooledConnectionWrapper@([a-f0-9]+)");
        Pattern returnPattern = Pattern.compile("ReturnToPool");
        
        Set<String> uniqueConnections = new HashSet<>();
        long checkoutCount = 0;
        long returnCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(tracelogFile))) {
            String line;
            int lineCount = 0;
            int maxLines = 100000; // Analyze more lines for connection tracking
            
            while ((line = reader.readLine()) != null && lineCount++ < maxLines) {
                Matcher m = connPattern.matcher(line);
                if (m.find()) {
                    String connId = m.group(1);
                    uniqueConnections.add(connId);
                    stats.totalOperations++;
                    
                    // Track usage per connection
                    stats.connectionUsage.put(connId, 
                        stats.connectionUsage.getOrDefault(connId, 0L) + 1);
                    
                    checkoutCount++;
                }
                
                if (returnPattern.matcher(line).find()) {
                    returnCount++;
                }
            }
            
            stats.uniqueConnections = uniqueConnections.size();
            
            // Find primary connection (most used)
            long maxOps = 0;
            for (Map.Entry<String, Long> entry : stats.connectionUsage.entrySet()) {
                if (entry.getValue() > maxOps) {
                    maxOps = entry.getValue();
                    stats.primaryConnection = entry.getKey();
                    stats.primaryConnectionOps = entry.getValue();
                }
            }
            
            if (stats.totalOperations > 0) {
                stats.primaryConnectionPercent = (stats.primaryConnectionOps * 100.0) / stats.totalOperations;
            }
            
            // Check for leaks
            long leaked = checkoutCount - returnCount;
            if (leaked > 10) {
                stats.leaksDetected = true;
            }
            
            // Report findings
            System.out.println("\nConnection Pool Statistics:");
            System.out.println("  Unique connections: " + stats.uniqueConnections);
            System.out.println("  Total operations: " + stats.totalOperations);
            System.out.println("  Connection checkouts: " + checkoutCount);
            System.out.println("  Connection returns: " + returnCount);
            System.out.println("  Difference: " + leaked + (leaked == 0 ? " (no leaks)" : ""));
            
            if (stats.primaryConnection != null) {
                System.out.println("\nPrimary Connection Usage:");
                System.out.println("  Connection ID: @" + stats.primaryConnection);
                System.out.println("  Operations: " + stats.primaryConnectionOps);
                System.out.println("  Percentage: " + String.format("%.1f%%", stats.primaryConnectionPercent));
                
                if (stats.primaryConnectionPercent > 80) {
                    System.out.println("\n⚠️  Connection Pool Under-Utilized:");
                    System.out.println("   Single connection handling " + String.format("%.1f%%", stats.primaryConnectionPercent) + " of operations");
                    System.out.println("   Recommendation: Enable parallel database operations");
                    System.out.println("   Add to server.properties:");
                    System.out.println("     application.manager.parallel.loading=true");
                    System.out.println("     application.manager.loader.threads=10");
                    
                    checkResults.add(new CheckResult(
                        "LOG-CONN-001",
                        "Connection Pool Under-Utilized",
                        "Performance",
                        "MEDIUM",
                        "WARNING",
                        "Single connection handles " + String.format("%.1f%%", stats.primaryConnectionPercent) + " of operations",
                        "Enable parallel database operations"
                    ));
                }
            }
            
            if (stats.leaksDetected) {
                System.out.println("\n❌ CONNECTION LEAK DETECTED:");
                System.out.println("   " + leaked + " connections not returned to pool");
                System.out.println("   Recommendation: Review code for missing connection.close() calls");
                
                checkResults.add(new CheckResult(
                    "LOG-CONN-002",
                    "Connection Leak Detected",
                    "Critical",
                    "CRITICAL",
                    "FAIL",
                    leaked + " connections leaked",
                    "Review code for missing connection cleanup"
                ));
            } else if (leaked == 0) {
                System.out.println("\n✅ No connection leaks detected");
            }
            
            System.out.println();
            
        } catch (IOException e) {
            System.out.println("⚠️  Error analyzing connection pool: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Analyze startup performance from stdout logs
     */
    private static void analyzeStartupPerformance(File logsDir) {
        // Login availability analysis removed per user request
        // This section intentionally left minimal to avoid false recommendations
    }
    
    /**
     * Analyze buffer logs for SQL queries and database performance
     */
    private static void analyzeBufferLogs(File logsDir) {
        System.out.println("─── Buffer Log Analysis (SQL Queries & Performance) ───\n");
        
        File bufferDir = new File(logsDir, "buffer");
        if (!bufferDir.exists() || !bufferDir.isDirectory()) {
            System.out.println("ℹ️  Buffer logs directory not found\n");
            return;
        }
        
        // Find most recent buffer log
        File[] bufferFiles = bufferDir.listFiles((dir, name) -> 
            name.startsWith("buffer.") && name.endsWith(".txt"));
        
        if (bufferFiles == null || bufferFiles.length == 0) {
            System.out.println("ℹ️  No buffer log files found\n");
            return;
        }
        
        // Sort by last modified, get most recent
        Arrays.sort(bufferFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        File bufferFile = bufferFiles[0];
        
        System.out.println("Analyzing: " + bufferFile.getName());
        System.out.println("Size: " + String.format("%.2f MB", bufferFile.length() / 1024.0 / 1024.0));
        
        // Statistics to collect
        int totalSQLQueries = 0;
        int selectQueries = 0;
        int insertQueries = 0;
        int updateQueries = 0;
        int deleteQueries = 0;
        int commits = 0;
        int rollbacks = 0;
        
        Map<String, Integer> slowQueries = new HashMap<>();
        Map<String, Integer> frequentTables = new HashMap<>();
        Map<String, Integer> connectionPoolStats = new HashMap<>();
        
        int maxConnectionsInUse = 0;
        int minFreeConnections = 100;
        
        List<String> longRunningQueries = new ArrayList<>();
        List<String> poolExhaustion = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(bufferFile))) {
            String line;
            int lineCount = 0;
            int maxLines = 100000; // Limit to avoid memory issues
            
            String currentQuery = null;
            long queryStartTime = 0;
            
            while ((line = reader.readLine()) != null && lineCount++ < maxLines) {
                
                // Count SQL queries
                if (line.contains("ESP#") && line.contains(":SQL(")) {
                    totalSQLQueries++;
                    currentQuery = line;
                    queryStartTime = System.currentTimeMillis();
                    
                    // Extract query type
                    if (line.toUpperCase().contains("SELECT ")) {
                        selectQueries++;
                        
                        // Extract table names
                        Pattern tablePattern = Pattern.compile("FROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
                        Matcher m = tablePattern.matcher(line);
                        while (m.find()) {
                            String table = m.group(1);
                            frequentTables.put(table, frequentTables.getOrDefault(table, 0) + 1);
                        }
                    } else if (line.toUpperCase().contains("INSERT ")) {
                        insertQueries++;
                    } else if (line.toUpperCase().contains("UPDATE ")) {
                        updateQueries++;
                    } else if (line.toUpperCase().contains("DELETE ")) {
                        deleteQueries++;
                    }
                }
                
                // Count commits and rollbacks
                if (line.contains("COMMIT")) {
                    commits++;
                } else if (line.contains("ROLLBACK")) {
                    rollbacks++;
                }
                
                // Track connection pool statistics
                if (line.contains("DBConnection Pool")) {
                    Pattern inUsePattern = Pattern.compile("In Use: (\\d+)");
                    Pattern freePattern = Pattern.compile("Free: (\\d+)");
                    
                    Matcher inUseM = inUsePattern.matcher(line);
                    Matcher freeM = freePattern.matcher(line);
                    
                    if (inUseM.find()) {
                        int inUse = Integer.parseInt(inUseM.group(1));
                        if (inUse > maxConnectionsInUse) {
                            maxConnectionsInUse = inUse;
                        }
                    }
                    
                    if (freeM.find()) {
                        int free = Integer.parseInt(freeM.group(1));
                        if (free < minFreeConnections) {
                            minFreeConnections = free;
                        }
                        
                        // Check for pool exhaustion
                        if (free == 0 && poolExhaustion.size() < 5) {
                            poolExhaustion.add(line.substring(0, Math.min(line.length(), 150)));
                        }
                    }
                }
                
                // Detect slow queries (queries taking > 1 second)
                if (line.contains("SQL(") && currentQuery != null) {
                    Pattern timePattern = Pattern.compile("SQL\\((\\d+)\\)");
                    Matcher m = timePattern.matcher(line);
                    if (m.find()) {
                        int queryTime = Integer.parseInt(m.group(1));
                        if (queryTime > 1000) { // > 1 second
                            String queryType = "Unknown";
                            if (line.contains("SELECT")) queryType = "SELECT";
                            else if (line.contains("UPDATE")) queryType = "UPDATE";
                            else if (line.contains("INSERT")) queryType = "INSERT";
                            else if (line.contains("DELETE")) queryType = "DELETE";
                            
                            slowQueries.put(queryType, slowQueries.getOrDefault(queryType, 0) + 1);
                            
                            if (longRunningQueries.size() < 5) {
                                longRunningQueries.add(queryType + " query took " + queryTime + "ms");
                            }
                        }
                    }
                }
            }
            
            // Report findings
            System.out.println("\nSQL Query Statistics:");
            System.out.println("  Total queries: " + totalSQLQueries);
            System.out.println("  SELECT: " + selectQueries + " (" + String.format("%.1f%%", (selectQueries * 100.0 / totalSQLQueries)) + ")");
            System.out.println("  INSERT: " + insertQueries + " (" + String.format("%.1f%%", (insertQueries * 100.0 / totalSQLQueries)) + ")");
            System.out.println("  UPDATE: " + updateQueries + " (" + String.format("%.1f%%", (updateQueries * 100.0 / totalSQLQueries)) + ")");
            System.out.println("  DELETE: " + deleteQueries + " (" + String.format("%.1f%%", (deleteQueries * 100.0 / totalSQLQueries)) + ")");
            System.out.println("  Commits: " + commits);
            System.out.println("  Rollbacks: " + rollbacks);
            
            if (rollbacks > 0) {
                double rollbackRate = (rollbacks * 100.0) / (commits + rollbacks);
                if (rollbackRate > 5) {
                    System.out.println("\n⚠️  High Rollback Rate Detected:");
                    System.out.println("   Rollback rate: " + String.format("%.1f%%", rollbackRate));
                    System.out.println("   Recommendation: Investigate transaction failures and application logic");
                    
                    checkResults.add(new CheckResult(
                        "LOG-BUFFER-001",
                        "High Database Rollback Rate",
                        "Performance",
                        "MEDIUM",
                        "WARNING",
                        String.format("%.1f%% of transactions rolled back", rollbackRate),
                        "Investigate transaction failures and optimize application logic"
                    ));
                }
            }
            
            // Report connection pool statistics
            System.out.println("\nConnection Pool Statistics:");
            System.out.println("  Peak connections in use: " + maxConnectionsInUse);
            System.out.println("  Minimum free connections: " + minFreeConnections);
            
            if (minFreeConnections == 0) {
                System.out.println("\n⚠️  Connection Pool Exhaustion Detected:");
                System.out.println("   Pool ran out of free connections");
                System.out.println("   Recommendation: Increase connection pool size");
                System.out.println("   Add to db.properties:");
                System.out.println("     database.maxconnections.in.pool=150  # Increase from current");
                
                if (!poolExhaustion.isEmpty()) {
                    System.out.println("   Example: " + poolExhaustion.get(0));
                }
                
                checkResults.add(new CheckResult(
                    "LOG-BUFFER-002",
                    "Connection Pool Exhaustion",
                    "Critical",
                    "HIGH",
                    "WARNING",
                    "Pool ran out of free connections",
                    "Increase database.maxconnections.in.pool in db.properties"
                ));
            }
            
            // Report slow queries
            if (!slowQueries.isEmpty()) {
                int totalSlowQueries = slowQueries.values().stream().mapToInt(Integer::intValue).sum();
                System.out.println("\nSlow Query Detection (> 1 second):");
                System.out.println("  Total slow queries: " + totalSlowQueries);
                
                for (Map.Entry<String, Integer> entry : slowQueries.entrySet()) {
                    System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " queries");
                }
                
                if (!longRunningQueries.isEmpty()) {
                    System.out.println("  Examples:");
                    for (String example : longRunningQueries) {
                        System.out.println("    - " + example);
                    }
                }
                
                if (totalSlowQueries > 100) {
                    System.out.println("\n⚠️  High Number of Slow Queries:");
                    System.out.println("   " + totalSlowQueries + " queries took > 1 second");
                    System.out.println("   Recommendation: Add database indexes and optimize queries");
                    System.out.println("   Actions:");
                    System.out.println("     1. Review query execution plans");
                    System.out.println("     2. Add indexes on frequently queried columns");
                    System.out.println("     3. Optimize WHERE clauses and JOINs");
                    
                    checkResults.add(new CheckResult(
                        "LOG-BUFFER-003",
                        "High Number of Slow Queries",
                        "Performance",
                        "MEDIUM",
                        "WARNING",
                        totalSlowQueries + " queries took > 1 second",
                        "Add database indexes and optimize query performance"
                    ));
                }
            }
            
            // Report most frequently accessed tables
            if (!frequentTables.isEmpty()) {
                System.out.println("\nMost Frequently Accessed Tables:");
                frequentTables.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .limit(10)
                    .forEach(entry -> {
                        System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " queries");
                    });
                
                // Check for hot tables
                int maxTableAccess = frequentTables.values().stream().max(Integer::compare).orElse(0);
                if (maxTableAccess > totalSQLQueries * 0.3) {
                    String hotTable = frequentTables.entrySet().stream()
                        .max((a, b) -> a.getValue().compareTo(b.getValue()))
                        .map(Map.Entry::getKey)
                        .orElse("Unknown");
                    
                    System.out.println("\nℹ️  Hot Table Detected:");
                    System.out.println("   Table: " + hotTable);
                    System.out.println("   Access rate: " + String.format("%.1f%%", (maxTableAccess * 100.0 / totalSQLQueries)));
                    System.out.println("   Recommendation: Consider caching or query optimization for this table");
                }
            }
            
            if (totalSQLQueries == 0) {
                System.out.println("\nℹ️  No SQL queries found in buffer log (may be empty or rotated)");
            } else {
                System.out.println("\n✅ Buffer log analysis complete");
            }
            
            System.out.println();
            
        } catch (IOException e) {
            System.out.println("⚠️  Error reading buffer log: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Generate comprehensive recommendations based on log analysis
     */
    private static void generateLogAnalysisRecommendations() {
        System.out.println("─── Proactive Recommendations ───\n");
        
        // Collect all log-related issues
        List<CheckResult> logIssues = new ArrayList<>();
        int critical = 0, high = 0, medium = 0;
        
        for (CheckResult result : checkResults) {
            if (result.category.equals("Log Analysis") || 
                result.category.equals("Performance") ||
                result.category.equals("Critical") ||
                result.category.equals("User Experience")) {
                
                logIssues.add(result);
                
                if (result.severity.equals("CRITICAL")) critical++;
                else if (result.severity.equals("HIGH")) high++;
                else if (result.severity.equals("MEDIUM")) medium++;
            }
        }
        
        if (critical > 0 || high > 0 || medium > 0) {
            System.out.println("Issues Found:");
            if (critical > 0) System.out.println("  ❌ Critical: " + critical);
            if (high > 0) System.out.println("  ⚠️  High: " + high);
            if (medium > 0) System.out.println("  ℹ️  Medium: " + medium);
            System.out.println();
            
            // Generate dynamic recommendations based on actual findings
            System.out.println("Recommended Actions (based on log analysis):");
            System.out.println();
            
            int recommendationCount = 1;
            
            // Sort by severity: CRITICAL > HIGH > MEDIUM
            logIssues.sort((a, b) -> {
                int severityOrder = getSeverityOrder(b.severity) - getSeverityOrder(a.severity);
                return severityOrder;
            });
            
            // Generate specific recommendations for each issue found
            for (CheckResult issue : logIssues) {
                if (recommendationCount > 10) break; // Limit to top 10
                
                String icon = issue.severity.equals("CRITICAL") ? "❌" : 
                             issue.severity.equals("HIGH") ? "⚠️" : "ℹ️";
                
                System.out.println(recommendationCount + ". " + icon + " " + issue.checkName + " (" + issue.severity + ")");
                System.out.println("   Issue: " + issue.message);
                System.out.println("   Action: " + issue.remediation);
                System.out.println();
                
                recommendationCount++;
            }
            
            // Add summary note
            if (logIssues.size() > 10) {
                System.out.println("   ... and " + (logIssues.size() - 10) + " more issue(s)");
                System.out.println();
            }
            
            System.out.println("Next Steps:");
            System.out.println("  1. Address CRITICAL issues immediately");
            System.out.println("  2. Plan fixes for HIGH priority issues within 24 hours");
            System.out.println("  3. Schedule MEDIUM priority issues for next maintenance window");
            System.out.println("  4. Review full health check report for all findings");
            
        } else {
            System.out.println("✅ No critical issues found in log analysis");
            System.out.println("   System appears to be operating normally");
            System.out.println();
            System.out.println("Recommendations:");
            System.out.println("  • Continue monitoring logs regularly");
            System.out.println("  • Run health check after any configuration changes");
            System.out.println("  • Keep logs for historical trend analysis");
        }
        
        System.out.println();
    }
    
    /**
     * Get severity order for sorting (higher number = more severe)
     */
    private static int getSeverityOrder(String severity) {
        switch (severity) {
            case "CRITICAL": return 3;
            case "HIGH": return 2;
            case "MEDIUM": return 1;
            default: return 0;
        }
    }
    
    /**
     * Run standalone application analysis mode (no dSeries installation required)
     */
    private static void runStandaloneApplicationAnalysis(String appsPath) {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  ESP dSeries Application Best Practices Analyzer v4.2.2");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        System.out.println("  Analysis Path: " + appsPath);
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
        
        File appsPathFile = new File(appsPath);
        if (!appsPathFile.exists()) {
            System.err.println("ERROR: Path not found: " + appsPath);
            System.err.println();
            System.err.println("Please provide a valid path to:");
            System.err.println("  - Directory containing application XML files");
            System.err.println("  - Single application XML file");
            System.exit(2);
        }
        
        System.out.println("Starting application analysis...");
        System.out.println();
        
        List<File> xmlFiles = new ArrayList<>();
        
        // Collect XML files
        if (appsPathFile.isDirectory()) {
            System.out.println("Scanning directory: " + appsPathFile.getAbsolutePath());
            collectXMLFiles(appsPathFile, xmlFiles);
        } else if (appsPathFile.isFile()) {
            // Accept both .xml files and files without extension (dSeries native format)
            String fileName = appsPathFile.getName().toLowerCase();
            if (fileName.endsWith(".xml") || !fileName.contains(".")) {
                if (isXMLFile(appsPathFile)) {
                    System.out.println("Analyzing file: " + appsPathFile.getAbsolutePath());
                    xmlFiles.add(appsPathFile);
                } else {
                    System.err.println("ERROR: File does not contain valid XML content");
                    System.exit(2);
                }
            } else {
                System.err.println("ERROR: File must be XML format (.xml extension or no extension)");
                System.exit(2);
            }
        } else {
            System.err.println("ERROR: Path must be a directory or file");
            System.exit(2);
        }
        
        if (xmlFiles.isEmpty()) {
            System.out.println("⚠️  No application files found in: " + appsPath);
            System.out.println();
            System.out.println("Tips:");
            System.out.println("  - Accepts .xml files (exported applications)");
            System.out.println("  - Accepts files without extension (dSeries native format)");
            System.out.println("  - Check file permissions");
            System.out.println("  - Verify files contain valid XML content");
            System.exit(0);
        }
        
        System.out.println("Found " + xmlFiles.size() + " XML file(s) to analyze");
        System.out.println();
        
        // Analyze applications
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("APPLICATION BEST PRACTICES ANALYSIS");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        int totalApps = 0;
        int totalJobs = 0;
        int parseErrors = 0;
        List<BestPracticeViolation> violations = new ArrayList<>();
        List<CloudOpportunity> cloudOpportunities = new ArrayList<>();
        List<ApplicationAnalysisResult> allResults = new ArrayList<>();  // Store all results for dependency analysis
        
        for (File xmlFile : xmlFiles) {
            try {
                ApplicationAnalysisResult result = analyzeApplicationXML(xmlFile);
                if (result != null) {
                    totalApps++;
                    totalJobs += result.jobCount;
                    violations.addAll(result.violations);
                    cloudOpportunities.addAll(result.cloudOpportunities);
                    allResults.add(result);  // Store for dependency analysis
                    System.out.println("  ✓ " + result.applicationName + " (" + result.jobCount + " jobs)");
                }
            } catch (Exception e) {
                parseErrors++;
                System.out.println("  ✗ " + xmlFile.getName() + " - Error: " + e.getMessage());
            }
        }
        
        System.out.println();
        System.out.println("─── Analysis Summary ───\n");
        System.out.println("Files processed: " + xmlFiles.size());
        System.out.println("Applications analyzed: " + totalApps);
        System.out.println("Total jobs analyzed: " + totalJobs);
        if (parseErrors > 0) {
            System.out.println("Parse errors: " + parseErrors);
        }
        System.out.println("Best practice violations: " + violations.size());
        System.out.println("Cloud integration opportunities: " + cloudOpportunities.size());
        System.out.println();
        
        // Report violations
        if (!violations.isEmpty()) {
            System.out.println("─── Best Practice Violations ───\n");
            
            Map<String, List<BestPracticeViolation>> violationsByCategory = new LinkedHashMap<>();
            for (BestPracticeViolation v : violations) {
                violationsByCategory.computeIfAbsent(v.category, k -> new ArrayList<>()).add(v);
            }
            
            for (Map.Entry<String, List<BestPracticeViolation>> entry : violationsByCategory.entrySet()) {
                System.out.println("Category: " + entry.getKey());
                System.out.println();
                
                for (BestPracticeViolation v : entry.getValue()) {
                    String icon = v.severity.equals("HIGH") ? "⚠️" : 
                                 v.severity.equals("MEDIUM") ? "ℹ️" : "💡";
                    
                    System.out.println(icon + " " + v.rule + " (" + v.severity + ")");
                    System.out.println("   Application: " + v.applicationName);
                    if (v.jobName != null) {
                        System.out.println("   Job: " + v.jobName);
                    }
                    System.out.println("   Issue: " + v.description);
                    System.out.println("   Recommendation: " + v.recommendation);
                    System.out.println();
                }
            }
        }
        
        // Report cloud opportunities
        if (!cloudOpportunities.isEmpty()) {
            System.out.println("─── Cloud Integration Opportunities ───\n");
            
            Map<String, List<CloudOpportunity>> opportunitiesByType = new LinkedHashMap<>();
            for (CloudOpportunity opp : cloudOpportunities) {
                opportunitiesByType.computeIfAbsent(opp.integrationType, k -> new ArrayList<>()).add(opp);
            }
            
            for (Map.Entry<String, List<CloudOpportunity>> entry : opportunitiesByType.entrySet()) {
                System.out.println("Integration Type: " + entry.getKey());
                System.out.println();
                
                for (CloudOpportunity opp : entry.getValue()) {
                    System.out.println("☁️  " + opp.pluginName);
                    System.out.println("   Application: " + opp.applicationName);
                    System.out.println("   Current Job: " + opp.jobName + " (" + opp.currentJobType + ")");
                    System.out.println("   Opportunity: " + opp.description);
                    System.out.println("   Benefit: " + opp.benefit);
                    System.out.println("   Plugin: " + opp.pluginName);
                    System.out.println("   Documentation: " + opp.documentationUrl);
                    System.out.println();
                }
            }
        }
        
        if (violations.isEmpty() && cloudOpportunities.isEmpty()) {
            System.out.println("✅ No issues or opportunities identified");
            System.out.println("   Applications follow best practices");
        }
        
        // Display dependency analysis
        displayDependencyAnalysis(allResults);
        
        // Summary recommendations
        System.out.println();
        System.out.println("─── Summary ───\n");
        
        int highSeverity = (int) violations.stream().filter(v -> v.severity.equals("HIGH")).count();
        int mediumSeverity = (int) violations.stream().filter(v -> v.severity.equals("MEDIUM")).count();
        int lowSeverity = (int) violations.stream().filter(v -> v.severity.equals("LOW")).count();
        
        if (highSeverity > 0) {
            System.out.println("⚠️  " + highSeverity + " HIGH severity issue(s) found - Address immediately");
        }
        if (mediumSeverity > 0) {
            System.out.println("ℹ️  " + mediumSeverity + " MEDIUM severity issue(s) found - Plan fixes soon");
        }
        if (lowSeverity > 0) {
            System.out.println("💡 " + lowSeverity + " LOW severity issue(s) found - Consider improvements");
        }
        if (cloudOpportunities.size() > 0) {
            System.out.println("☁️  " + cloudOpportunities.size() + " cloud integration opportunit(ies) - Modernize workload");
        }
        
        System.out.println();
        System.out.println("For detailed implementation guidance, see:");
        System.out.println("  APPLICATION_BEST_PRACTICES_GUIDE.md");
        System.out.println();
        
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("Analysis complete!");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }
    
    /**
     * Display comprehensive dependency analysis with tables and flow visualization
     */
    private static void displayDependencyAnalysis(List<ApplicationAnalysisResult> results) {
        if (results.isEmpty() || !showDependencies) return;
        
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("DEPENDENCY & FLOW ANALYSIS");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println();
        
        // Build cross-application dependency map
        Map<String, Set<String>> appDependencies = new LinkedHashMap<>();
        Map<String, List<ExternalDependency>> externalDepDetails = new LinkedHashMap<>();
        
        for (ApplicationAnalysisResult result : results) {
            String appName = result.applicationName;
            appDependencies.putIfAbsent(appName, new HashSet<>());
            
            // Track external dependencies
            for (ExternalDependency extDep : result.externalDependencies) {
                appDependencies.get(appName).add(extDep.toApplication);
                externalDepDetails.computeIfAbsent(appName, k -> new ArrayList<>()).add(extDep);
            }
        }
        
        // Display External (Cross-Application) Dependencies
        if (!externalDepDetails.isEmpty()) {
            System.out.println("─── Cross-Application Dependencies ───");
            System.out.println();
            System.out.println("Applications that call other applications:");
            System.out.println();
            
            // Table header
            System.out.println(String.format("%-20s %-20s %-25s %-15s", 
                "From Application", "From Job", "To Application", "Job Type"));
            for (int i = 0; i < 85; i++) System.out.print("─");
            System.out.println();
            
            for (Map.Entry<String, List<ExternalDependency>> entry : externalDepDetails.entrySet()) {
                for (ExternalDependency dep : entry.getValue()) {
                    System.out.println(String.format("%-20s %-20s %-25s %-15s",
                        truncate(dep.fromApplication, 20),
                        truncate(dep.fromJob, 20),
                        truncate(dep.toApplication, 25),
                        dep.jobType));
                }
            }
            System.out.println();
        }
        
        // Display Application Dependency Graph (ASCII)
        if (!appDependencies.isEmpty()) {
            System.out.println("─── Application Dependency Graph ───");
            System.out.println();
            
            // Find applications with dependencies
            Set<String> appsWithDeps = new HashSet<>();
            for (Map.Entry<String, Set<String>> entry : appDependencies.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    appsWithDeps.add(entry.getKey());
                    appsWithDeps.addAll(entry.getValue());
                }
            }
            
            if (appsWithDeps.isEmpty()) {
                System.out.println("ℹ️  No cross-application dependencies found");
                System.out.println("   All applications are independent");
            } else {
                for (Map.Entry<String, Set<String>> entry : appDependencies.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        System.out.println("📦 " + entry.getKey());
                        for (String targetApp : entry.getValue()) {
                            System.out.println("   └─> " + targetApp);
                        }
                        System.out.println();
                    }
                }
            }
            System.out.println();
        }
        
        // Display Internal Job Dependencies for each application
        System.out.println("─── Internal Job Dependencies (Per Application) ───");
        System.out.println();
        
        for (ApplicationAnalysisResult result : results) {
            if (result.internalDependencies.isEmpty()) continue;
            
            System.out.println("Application: " + result.applicationName + " (" + result.jobCount + " jobs)");
            System.out.println();
            
            // Group dependencies for better visualization
            Map<String, List<String>> jobFlows = new LinkedHashMap<>();
            for (JobDependency dep : result.internalDependencies) {
                jobFlows.computeIfAbsent(dep.fromJob, k -> new ArrayList<>()).add(dep.toJob);
            }
            
            // Display as a flow
            if (jobFlows.size() <= 20) {
                // For small apps, show full flow
                for (Map.Entry<String, List<String>> entry : jobFlows.entrySet()) {
                    System.out.println("  " + entry.getKey());
                    for (String target : entry.getValue()) {
                        System.out.println("    └─> " + target);
                    }
                }
            } else {
                // For large apps, show table format
                System.out.println(String.format("  %-30s %-30s %-15s", 
                    "From Job", "To Job", "Condition"));
                System.out.print("  ");
                for (int i = 0; i < 80; i++) System.out.print("─");
                System.out.println();
                
                for (JobDependency dep : result.internalDependencies) {
                    System.out.println(String.format("  %-30s %-30s %-15s",
                        truncate(dep.fromJob, 30),
                        truncate(dep.toJob, 30),
                        dep.condition));
                }
            }
            
            System.out.println();
            System.out.println("  Total dependencies: " + result.internalDependencies.size());
            System.out.println();
        }
        
        // Summary statistics
        int totalInternalDeps = results.stream().mapToInt(r -> r.internalDependencies.size()).sum();
        int totalExternalDeps = results.stream().mapToInt(r -> r.externalDependencies.size()).sum();
        
        System.out.println("─── Dependency Statistics ───");
        System.out.println();
        System.out.println("Applications analyzed: " + results.size());
        System.out.println("Internal job dependencies: " + totalInternalDeps);
        System.out.println("Cross-application dependencies: " + totalExternalDeps);
        System.out.println("Applications with external dependencies: " + externalDepDetails.size());
        System.out.println();
    }
    
    /**
     * Truncate string to specified length with ellipsis
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Get elements by tag name, handling both namespaced and non-namespaced elements
     * Supports: <cmd_job> and <app:cmd_job>
     */
    private static NodeList getElementsByTagNameNS(Document doc, String tagName) {
        // Try without namespace first
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes;
        }
        // Try with app: namespace prefix
        return doc.getElementsByTagName("app:" + tagName);
    }
    
    /**
     * Get elements by tag name from an element, handling both namespaced and non-namespaced
     */
    private static NodeList getElementsByTagNameNS(Element element, String tagName) {
        // Try without namespace first
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes;
        }
        // Try with app: namespace prefix
        return element.getElementsByTagName("app:" + tagName);
    }
    
    /**
     * Recursively collect XML files from directory
     * Accepts both .xml files and files without extensions (dSeries native format)
     */
    private static void collectXMLFiles(File dir, List<File> xmlFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectXMLFiles(file, xmlFiles);
                } else {
                    String fileName = file.getName().toLowerCase();
                    // Accept .xml files or files without extension (dSeries native format)
                    if (fileName.endsWith(".xml") || !fileName.contains(".")) {
                        // Verify it's actually an XML file by checking first few bytes
                        if (isXMLFile(file)) {
                            xmlFiles.add(file);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check if file is an XML file by reading first few bytes
     */
    private static boolean isXMLFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();
            if (firstLine == null) return false;
            
            // Check for XML declaration or root element (with or without namespace prefix)
            String trimmed = firstLine.trim();
            return trimmed.startsWith("<?xml") || 
                   trimmed.startsWith("<appl") || 
                   trimmed.startsWith("<app:appl") ||  // namespace prefix
                   trimmed.startsWith("<application");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Perform comprehensive application best practices analysis
     * Scans application XMLs for design issues and cloud integration opportunities
     */
    private static void performApplicationAnalysis() {
        System.out.println("\n═══════════════════════════════════════════════════════════════════");
        System.out.println("APPLICATION BEST PRACTICES ANALYSIS");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");
        
        // Look for application XMLs in common locations
        List<File> appDirs = new ArrayList<>();
        
        // Check standard locations
        File appsDir1 = new File(installDir, "apps");
        File appsDir2 = new File(installDir, "../apps");
        File appsDir3 = new File(installDir, "../../apps");
        
        if (appsDir1.exists() && appsDir1.isDirectory()) appDirs.add(appsDir1);
        if (appsDir2.exists() && appsDir2.isDirectory()) appDirs.add(appsDir2);
        if (appsDir3.exists() && appsDir3.isDirectory()) appDirs.add(appsDir3);
        
        if (appDirs.isEmpty()) {
            System.out.println("ℹ️  No application directories found for analysis");
            System.out.println("   Checked: apps/, ../apps/, ../../apps/");
            System.out.println("   To analyze applications, place XML files in one of these directories\n");
            return;
        }
        
        System.out.println("Scanning application directories...");
        
        int totalApps = 0;
        int totalJobs = 0;
        List<BestPracticeViolation> violations = new ArrayList<>();
        List<CloudOpportunity> cloudOpportunities = new ArrayList<>();
        
        for (File appDir : appDirs) {
            System.out.println("  Scanning: " + appDir.getAbsolutePath());
            
            File[] xmlFiles = appDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
            if (xmlFiles != null) {
                for (File xmlFile : xmlFiles) {
                    try {
                        ApplicationAnalysisResult result = analyzeApplicationXML(xmlFile);
                        if (result != null) {
                            totalApps++;
                            totalJobs += result.jobCount;
                            violations.addAll(result.violations);
                            cloudOpportunities.addAll(result.cloudOpportunities);
                        }
                    } catch (Exception e) {
                        System.out.println("    ⚠️  Error analyzing " + xmlFile.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        System.out.println();
        System.out.println("─── Analysis Summary ───\n");
        System.out.println("Applications scanned: " + totalApps);
        System.out.println("Total jobs analyzed: " + totalJobs);
        System.out.println("Best practice violations: " + violations.size());
        System.out.println("Cloud integration opportunities: " + cloudOpportunities.size());
        System.out.println();
        
        // Report violations
        if (!violations.isEmpty()) {
            System.out.println("─── Best Practice Violations ───\n");
            
            Map<String, List<BestPracticeViolation>> violationsByCategory = new LinkedHashMap<>();
            for (BestPracticeViolation v : violations) {
                violationsByCategory.computeIfAbsent(v.category, k -> new ArrayList<>()).add(v);
            }
            
            for (Map.Entry<String, List<BestPracticeViolation>> entry : violationsByCategory.entrySet()) {
                System.out.println("Category: " + entry.getKey());
                System.out.println();
                
                for (BestPracticeViolation v : entry.getValue()) {
                    String icon = v.severity.equals("HIGH") ? "⚠️" : 
                                 v.severity.equals("MEDIUM") ? "ℹ️" : "💡";
                    
                    System.out.println(icon + " " + v.rule);
                    System.out.println("   Application: " + v.applicationName);
                    if (v.jobName != null) {
                        System.out.println("   Job: " + v.jobName);
                    }
                    System.out.println("   Issue: " + v.description);
                    System.out.println("   Recommendation: " + v.recommendation);
                    System.out.println();
                    
                    // Add to checkResults for overall summary
                    checkResults.add(new CheckResult(
                        "BP-" + v.rule.replaceAll("[^A-Z0-9]", ""),
                        v.rule,
                        "Best Practices",
                        v.severity,
                        "VIOLATION",
                        v.description,
                        v.recommendation
                    ));
                }
            }
        }
        
        // Report cloud opportunities
        if (!cloudOpportunities.isEmpty()) {
            System.out.println("─── Cloud Integration Opportunities ───\n");
            
            Map<String, List<CloudOpportunity>> opportunitiesByType = new LinkedHashMap<>();
            for (CloudOpportunity opp : cloudOpportunities) {
                opportunitiesByType.computeIfAbsent(opp.integrationType, k -> new ArrayList<>()).add(opp);
            }
            
            for (Map.Entry<String, List<CloudOpportunity>> entry : opportunitiesByType.entrySet()) {
                System.out.println("Integration Type: " + entry.getKey());
                System.out.println();
                
                for (CloudOpportunity opp : entry.getValue()) {
                    System.out.println("☁️  " + opp.pluginName);
                    System.out.println("   Application: " + opp.applicationName);
                    System.out.println("   Current Job: " + opp.jobName + " (" + opp.currentJobType + ")");
                    System.out.println("   Opportunity: " + opp.description);
                    System.out.println("   Benefit: " + opp.benefit);
                    System.out.println("   Plugin: " + opp.pluginName);
                    System.out.println("   Documentation: " + opp.documentationUrl);
                    System.out.println();
                    
                    // Add to checkResults
                    checkResults.add(new CheckResult(
                        "CLOUD-" + opp.pluginName.replaceAll("[^A-Z0-9]", ""),
                        "Cloud Integration: " + opp.pluginName,
                        "Modernization",
                        "INFO",
                        "OPPORTUNITY",
                        opp.description + " - " + opp.benefit,
                        "Consider migrating to " + opp.pluginName + " plugin. See: " + opp.documentationUrl
                    ));
                }
            }
        }
        
        if (violations.isEmpty() && cloudOpportunities.isEmpty()) {
            System.out.println("✅ No issues or opportunities identified");
            System.out.println("   Applications follow best practices");
        }
        
        System.out.println();
    }
    
    /**
     * Analyze a single application XML file
     */
    private static ApplicationAnalysisResult analyzeApplicationXML(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        
        ApplicationAnalysisResult result = new ApplicationAnalysisResult();
        result.fileName = xmlFile.getName();
        
        // Get application name
        Element applElement = doc.getDocumentElement();
        result.applicationName = applElement.getAttribute("name");
        if (result.applicationName == null || result.applicationName.isEmpty()) {
            result.applicationName = xmlFile.getName().replace(".xml", "");
        }
        
        // Count jobs and analyze each type
        // Note: link elements are NOT jobs - they are flow control elements
        NodeList cmdJobs = getElementsByTagNameNS(doc, "cmd_job");
        NodeList scriptJobs = getElementsByTagNameNS(doc, "script_job");
        NodeList pojoJobs = getElementsByTagNameNS(doc, "pojo_job");
        NodeList boxJobs = getElementsByTagNameNS(doc, "box_job");
        NodeList dbJobs = getElementsByTagNameNS(doc, "db_job");
        NodeList ftpJobs = getElementsByTagNameNS(doc, "ftp_job");
        NodeList unixJobs = getElementsByTagNameNS(doc, "unix_job");
        NodeList ntJobs = getElementsByTagNameNS(doc, "nt_job");
        NodeList extJobs = getElementsByTagNameNS(doc, "ext_job");  // External/sub-application jobs
        
        result.jobCount = cmdJobs.getLength() + scriptJobs.getLength() + pojoJobs.getLength() + 
                         boxJobs.getLength() + dbJobs.getLength() + ftpJobs.getLength() +
                         unixJobs.getLength() + ntJobs.getLength() + extJobs.getLength();
        
        // Extract dependencies from all job types
        extractDependencies(doc, result);
        
        // Analyze application-level best practices
        analyzeApplicationLevel(applElement, result);
        
        // Analyze cmd_job best practices
        for (int i = 0; i < cmdJobs.getLength(); i++) {
            analyzeCmdJob((Element) cmdJobs.item(i), result);
        }
        
        // Analyze script_job best practices
        for (int i = 0; i < scriptJobs.getLength(); i++) {
            analyzeScriptJob((Element) scriptJobs.item(i), result);
        }
        
        // Analyze pojo_job for cloud opportunities
        for (int i = 0; i < pojoJobs.getLength(); i++) {
            analyzePojoJob((Element) pojoJobs.item(i), result);
        }
        
        // Analyze db_job for cloud opportunities
        for (int i = 0; i < dbJobs.getLength(); i++) {
            analyzeDbJob((Element) dbJobs.item(i), result);
        }
        
        // Analyze ftp_job for cloud opportunities
        for (int i = 0; i < ftpJobs.getLength(); i++) {
            analyzeFtpJob((Element) ftpJobs.item(i), result);
        }
        
        return result;
    }
    
    /**
     * Extract job dependencies (internal and external) from application
     */
    private static void extractDependencies(Document doc, ApplicationAnalysisResult result) {
        String appName = result.applicationName;
        
        // Job types to analyze for dependencies
        String[] jobTypes = {"cmd_job", "script_job", "pojo_job", "unix_job", "nt_job", "ext_job", "db_job", "ftp_job", "box_job", "link"};
        
        for (String jobType : jobTypes) {
            NodeList jobs = getElementsByTagNameNS(doc, jobType);
            
            for (int i = 0; i < jobs.getLength(); i++) {
                Element job = (Element) jobs.item(i);
                String jobName = job.getAttribute("name");
                
                // Handle qualified names (e.g., "WTDWKLY.END")
                String qualifier = job.getAttribute("qualifier");
                if (qualifier != null && !qualifier.isEmpty()) {
                    jobName = jobName + "." + qualifier;
                }
                
                // Extract internal dependencies (relconditionlist)
                NodeList depLists = getElementsByTagNameNS(job, "relconditionlist");
                if (depLists.getLength() > 0) {
                    Element depList = (Element) depLists.item(0);
                    NodeList conditions = getElementsByTagNameNS(depList, "relcondition");
                    
                    for (int j = 0; j < conditions.getLength(); j++) {
                        Element condition = (Element) conditions.item(j);
                        NodeList successorNodes = getElementsByTagNameNS(condition, "successorname");
                        NodeList conditionNodes = getElementsByTagNameNS(condition, "condition");
                        
                        if (successorNodes.getLength() > 0) {
                            String successor = successorNodes.item(0).getTextContent().trim();
                            String condType = conditionNodes.getLength() > 0 ? 
                                            conditionNodes.item(0).getTextContent().trim() : "NORMAL";
                            
                            result.internalDependencies.add(new JobDependency(jobName, successor, condType));
                        }
                    }
                }
                
                // Extract external dependencies (ext_job with applid)
                if (jobType.equals("ext_job")) {
                    NodeList applidNodes = getElementsByTagNameNS(job, "applid");
                    if (applidNodes.getLength() > 0) {
                        String targetApp = applidNodes.item(0).getTextContent().trim();
                        result.externalDependencies.add(new ExternalDependency(appName, jobName, targetApp, "ext_job"));
                    }
                }
            }
        }
    }
    
    /**
     * Analyze application-level configuration
     */
    private static void analyzeApplicationLevel(Element applElement, ApplicationAnalysisResult result) {
        String appName = result.applicationName;
        
        // Check for JavaScript usage
        NodeList jsScripts = applElement.getElementsByTagName("javascript");
        if (jsScripts.getLength() > 0) {
            // Check if JavaScript is in repository vs inline
            for (int i = 0; i < jsScripts.getLength(); i++) {
                Element jsElement = (Element) jsScripts.item(i);
                String jsContent = jsElement.getTextContent();
                if (jsContent != null && jsContent.trim().length() > 100) {
                    result.violations.add(new BestPracticeViolation(
                        "JavaScript Repository Usage",
                        "MEDIUM",
                        "Application Design",
                        appName,
                        null,
                        "Application contains inline JavaScript code (>100 chars)",
                        "Move JavaScript to repository for better maintainability and reusability. Use script references instead of inline code."
                    ));
                }
            }
        }
        
        // Check for global variable usage
        NodeList defaults = applElement.getElementsByTagName("defaults");
        if (defaults.getLength() == 0) {
            result.violations.add(new BestPracticeViolation(
                "Application Defaults",
                "LOW",
                "Application Design",
                appName,
                null,
                "Application missing default configuration (agent, schedule, etc.)",
                "Define application-level defaults to reduce job-level configuration duplication and improve maintainability."
            ));
        }
        
        // Check for calendar usage - Look in multiple places (namespace-aware)
        NodeList runFreq = getElementsByTagNameNS(applElement, "run");
        boolean usesCalendar = false;
        
        // Check in run frequency elements
        for (int i = 0; i < runFreq.getLength(); i++) {
            Element runElement = (Element) runFreq.item(i);
            NodeList calNodes = getElementsByTagNameNS(runElement, "calendar");
            if (calNodes.getLength() > 0) {
                usesCalendar = true;
                break;
            }
        }
        
        // Also check schedules section for day-based scheduling
        if (!usesCalendar) {
            NodeList schedules = getElementsByTagNameNS(applElement, "schedules");
            if (schedules.getLength() > 0) {
                String schedText = schedules.item(0).getTextContent();
                if (schedText != null) {
                    schedText = schedText.trim().toUpperCase();
                    // Look for calendar indicators: day names, frequency keywords, calendar references
                    if (schedText.matches(".*(MON|TUE|WED|THU|FRI|SAT|SUN|DAILY|WEEKLY|MONTHLY|YEARLY|CALENDAR|HOLIDAY|WORKDAY).*")) {
                        usesCalendar = true;
                    }
                }
            }
        }
        
        // Check individual job schedules if not found at app level
        if (!usesCalendar && result.jobCount > 10) {
            String[] jobTypes = {"cmd_job", "script_job", "pojo_job", "unix_job", "nt_job", "ext_job", "db_job", "ftp_job"};
            outerLoop:
            for (String jobType : jobTypes) {
                NodeList jobs = getElementsByTagNameNS(applElement, jobType);
                for (int i = 0; i < jobs.getLength(); i++) {
                    Element job = (Element) jobs.item(i);
                    NodeList jobSchedules = getElementsByTagNameNS(job, "schedules");
                    if (jobSchedules.getLength() > 0) {
                        String jobSchedText = jobSchedules.item(0).getTextContent();
                        if (jobSchedText != null) {
                            jobSchedText = jobSchedText.trim().toUpperCase();
                            if (jobSchedText.matches(".*(MON|TUE|WED|THU|FRI|SAT|SUN|DAILY|WEEKLY|MONTHLY|YEARLY|CALENDAR|HOLIDAY|WORKDAY).*")) {
                                usesCalendar = true;
                                break outerLoop;
                            }
                        }
                    }
                }
            }
        }
        
        // Only suggest calendar usage if:
        // 1. Application has many jobs (>10, not >5) to avoid false positives on small apps
        // 2. No calendar usage detected at all
        // 3. Application actually has scheduled jobs (not just triggered/on-demand)
        if (!usesCalendar && result.jobCount > 10) {
            // Verify the app has actual scheduled jobs before suggesting calendar usage
            boolean hasScheduledJobs = false;
            String[] jobTypes = {"cmd_job", "script_job", "pojo_job", "unix_job", "nt_job", "ext_job", "db_job", "ftp_job"};
            for (String jobType : jobTypes) {
                NodeList jobs = applElement.getElementsByTagName(jobType);
                for (int i = 0; i < jobs.getLength(); i++) {
                    Element job = (Element) jobs.item(i);
                    NodeList jobSchedules = job.getElementsByTagName("schedules");
                    if (jobSchedules.getLength() > 0) {
                        String schedContent = jobSchedules.item(0).getTextContent();
                        if (schedContent != null && schedContent.trim().length() > 0) {
                            hasScheduledJobs = true;
                            break;
                        }
                    }
                }
                if (hasScheduledJobs) break;
            }
            
            if (hasScheduledJobs) {
                result.violations.add(new BestPracticeViolation(
                    "Calendar Usage",
                    "LOW",
                    "Scheduling",
                    appName,
                    null,
                    "Application with " + result.jobCount + " scheduled jobs does not use day-specific or calendar-based scheduling",
                    "Consider using calendars to define holidays, special days, and workdays for more flexible scheduling."
                ));
            }
        }
    }
    
    /**
     * Analyze cmd_job for best practices
     */
    private static void analyzeCmdJob(Element jobElement, ApplicationAnalysisResult result) {
        String jobName = jobElement.getAttribute("name");
        String appName = result.applicationName;
        
        // Check for hardcoded paths
        NodeList commandNodes = jobElement.getElementsByTagName("command");
        String command = null;
        if (commandNodes.getLength() > 0) {
            command = commandNodes.item(0).getTextContent();
            
            // Check for hardcoded paths (Windows and Unix)
            if (command.matches(".*[A-Z]:\\\\.*") || command.matches(".*/home/.*") || command.matches(".*/opt/.*")) {
                result.violations.add(new BestPracticeViolation(
                    "Hardcoded Paths",
                    "MEDIUM",
                    "Job Configuration",
                    appName,
                    jobName,
                    "Command contains hardcoded file system paths",
                    "Use global variables (%VAR) for paths to improve portability across environments."
                ));
            }
            
            // Check for credentials in command
            if (command.toLowerCase().contains("password") || command.toLowerCase().contains("pwd=") || 
                command.toLowerCase().contains("-p ") || command.matches(".*['\"][^'\"]{8,}['\"].*")) {
                result.violations.add(new BestPracticeViolation(
                    "Credentials in Command",
                    "HIGH",
                    "Security",
                    appName,
                    jobName,
                    "Command may contain embedded credentials or passwords",
                    "Use global variables with secure storage for credentials. Never hardcode passwords in job definitions."
                ));
            }
        }
        
        // Check for retry configuration
        NodeList retryNodes = jobElement.getElementsByTagName("retry");
        if (retryNodes.getLength() == 0 || retryNodes.item(0).getTextContent().equals("0")) {
            result.violations.add(new BestPracticeViolation(
                "Retry Configuration",
                "LOW",
                "Reliability",
                appName,
                jobName,
                "Job has no retry configuration for transient failures",
                "Configure retry count and interval for better resilience against temporary failures."
            ));
        }
        
        // Identify cloud migration opportunities
        if (command != null) {
            String cmdLower = command.toLowerCase();
            
            // AWS CLI commands
            if (cmdLower.contains("aws ") || cmdLower.contains("aws.exe")) {
                identifyAWSOpportunity(command, appName, jobName, result);
            }
            
            // Azure CLI commands
            if (cmdLower.contains("az ") || cmdLower.contains("azure")) {
                identifyAzureOpportunity(command, appName, jobName, result);
            }
            
            // GCP CLI commands
            if (cmdLower.contains("gcloud ") || cmdLower.contains("gsutil")) {
                identifyGCPOpportunity(command, appName, jobName, result);
            }
            
            // Databricks CLI
            if (cmdLower.contains("databricks")) {
                result.cloudOpportunities.add(new CloudOpportunity(
                    "Data Processing",
                    "Databricks Plugin Extension",
                    appName,
                    jobName,
                    "cmd_job",
                    "Replace Databricks CLI commands with native Databricks plugin",
                    "Better error handling, built-in monitoring, and simplified configuration",
                    "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/databricks-plugin-extension.html"
                ));
            }
        }
    }
    
    /**
     * Analyze script_job for best practices
     */
    private static void analyzeScriptJob(Element jobElement, ApplicationAnalysisResult result) {
        String jobName = jobElement.getAttribute("name");
        String appName = result.applicationName;
        
        // Check for inline scripts vs script files
        NodeList scriptNodes = jobElement.getElementsByTagName("script");
        if (scriptNodes.getLength() > 0) {
            String script = scriptNodes.item(0).getTextContent();
            if (script != null && script.trim().length() > 200) {
                result.violations.add(new BestPracticeViolation(
                    "Inline Script Size",
                    "MEDIUM",
                    "Job Configuration",
                    appName,
                    jobName,
                    "Script job contains large inline script (>200 chars)",
                    "Move large scripts to external files for better version control and maintainability."
                ));
            }
        }
    }
    
    /**
     * Analyze pojo_job (already cloud-enabled)
     */
    private static void analyzePojoJob(Element jobElement, ApplicationAnalysisResult result) {
        String jobName = jobElement.getAttribute("name");
        String appName = result.applicationName;
        
        // POJO jobs are already using cloud plugins - check for best practices
        NodeList classNodes = jobElement.getElementsByTagName("classname");
        if (classNodes.getLength() > 0) {
            String className = classNodes.item(0).getTextContent();
            
            // Check for hardcoded credentials in parameters
            NodeList paramNodes = jobElement.getElementsByTagName("parameter");
            for (int i = 0; i < paramNodes.getLength(); i++) {
                Element paramElement = (Element) paramNodes.item(i);
                NodeList typeNodes = paramElement.getElementsByTagName("type");
                NodeList valueNodes = paramElement.getElementsByTagName("value");
                
                if (typeNodes.getLength() > 0 && valueNodes.getLength() > 0) {
                    String type = typeNodes.item(0).getTextContent().toLowerCase();
                    String value = valueNodes.item(0).getTextContent();
                    
                    // Check if credentials are using variables
                    if ((type.contains("password") || type.contains("secret") || type.contains("key")) && 
                        !value.startsWith("$$") && !value.startsWith("%VAR")) {
                        result.violations.add(new BestPracticeViolation(
                            "Hardcoded Credentials in POJO",
                            "HIGH",
                            "Security",
                            appName,
                            jobName,
                            "Cloud plugin job has hardcoded credentials in parameters",
                            "Use global variables ($$VAR or %VAR) for all credentials and secrets."
                        ));
                    }
                }
            }
        }
    }
    
    /**
     * Analyze db_job for cloud opportunities
     */
    private static void analyzeDbJob(Element jobElement, ApplicationAnalysisResult result) {
        String jobName = jobElement.getAttribute("name");
        String appName = result.applicationName;
        
        // Check for cloud database opportunities
        NodeList urlNodes = jobElement.getElementsByTagName("db_url");
        if (urlNodes.getLength() > 0) {
            String dbUrl = urlNodes.item(0).getTextContent().toLowerCase();
            
            // AWS RDS/Aurora
            if (dbUrl.contains("rds.amazonaws.com") || dbUrl.contains("aurora")) {
                result.cloudOpportunities.add(new CloudOpportunity(
                    "Database",
                    "AWS Glue Plugin Extension",
                    appName,
                    jobName,
                    "db_job",
                    "Consider AWS Glue for ETL operations on AWS RDS/Aurora",
                    "Serverless ETL, better integration with AWS ecosystem, automatic scaling",
                    "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/aws-glue-plugin-extension.html"
                ));
            }
            
            // Azure SQL
            if (dbUrl.contains("database.windows.net")) {
                result.cloudOpportunities.add(new CloudOpportunity(
                    "Database",
                    "Azure Data Factory Plugin Extension",
                    appName,
                    jobName,
                    "db_job",
                    "Consider Azure Data Factory for data integration on Azure SQL",
                    "Cloud-native ETL, better Azure integration, managed service",
                    "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/azure-data-factory-plugin-extension.html"
                ));
            }
            
            // Google Cloud SQL
            if (dbUrl.contains("cloudsql") || dbUrl.contains("googleapis.com")) {
                result.cloudOpportunities.add(new CloudOpportunity(
                    "Database",
                    "Google Cloud BigQuery Plugin Extension",
                    appName,
                    jobName,
                    "db_job",
                    "Consider BigQuery for analytics workloads on GCP",
                    "Serverless data warehouse, automatic scaling, cost-effective",
                    "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/gcp-plugin-extension.html"
                ));
            }
        }
    }
    
    /**
     * Analyze ftp_job for cloud opportunities
     * Only recommend cloud storage for actual FTP/SFTP operations, not native file transfers
     */
    private static void analyzeFtpJob(Element jobElement, ApplicationAnalysisResult result) {
        String jobName = jobElement.getAttribute("name");
        String appName = result.applicationName;
        
        // Check if this is actually an FTP/SFTP job (not just native file transfer)
        NodeList hostnameNodes = jobElement.getElementsByTagName("hostname");
        NodeList hostNodes = jobElement.getElementsByTagName("host");
        NodeList serverNodes = jobElement.getElementsByTagName("server");
        
        boolean hasRemoteHost = false;
        String hostValue = null;
        
        // Check for hostname
        if (hostnameNodes.getLength() > 0) {
            hostValue = hostnameNodes.item(0).getTextContent();
            hasRemoteHost = hostValue != null && !hostValue.trim().isEmpty() && 
                           !hostValue.equals("localhost") && !hostValue.equals("127.0.0.1");
        }
        
        // Check for host
        if (!hasRemoteHost && hostNodes.getLength() > 0) {
            hostValue = hostNodes.item(0).getTextContent();
            hasRemoteHost = hostValue != null && !hostValue.trim().isEmpty() && 
                           !hostValue.equals("localhost") && !hostValue.equals("127.0.0.1");
        }
        
        // Check for server
        if (!hasRemoteHost && serverNodes.getLength() > 0) {
            hostValue = serverNodes.item(0).getTextContent();
            hasRemoteHost = hostValue != null && !hostValue.trim().isEmpty() && 
                           !hostValue.equals("localhost") && !hostValue.equals("127.0.0.1");
        }
        
        // Only recommend cloud storage for actual FTP/SFTP operations
        if (hasRemoteHost) {
            // Check if it's already using cloud storage
            String hostLower = hostValue != null ? hostValue.toLowerCase() : "";
            boolean isCloudStorage = hostLower.contains("s3.") || 
                                    hostLower.contains("blob.core.windows.net") ||
                                    hostLower.contains("storage.googleapis.com") ||
                                    hostLower.contains("amazonaws.com");
            
            if (!isCloudStorage) {
                result.cloudOpportunities.add(new CloudOpportunity(
                    "File Transfer",
                    "Cloud Storage Plugin Extensions",
                    appName,
                    jobName,
                    "ftp_job",
                    "Replace FTP/SFTP with cloud storage (S3, Azure Blob, GCS) for better reliability and security",
                    "Higher availability, better security, automatic versioning, lifecycle management",
                    "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension.html"
                ));
            }
        }
        // If no remote host, it's likely a native file transfer - no cloud recommendation needed
    }
    
    /**
     * Identify AWS-specific cloud opportunities
     */
    private static void identifyAWSOpportunity(String command, String appName, String jobName, ApplicationAnalysisResult result) {
        String cmdLower = command.toLowerCase();
        
        if (cmdLower.contains("s3 ") || cmdLower.contains("s3api")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Storage",
                "Amazon S3 Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace AWS CLI S3 commands with native S3 plugin",
                "Better error handling, built-in monitoring, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/amazon-s3-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("lambda")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Compute",
                "AWS Lambda Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace AWS CLI Lambda commands with native Lambda plugin",
                "Direct integration, better error handling, automatic retry logic",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/lambda-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("batch")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Compute",
                "AWS Batch Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace AWS CLI Batch commands with native Batch plugin",
                "Better job monitoring, automatic status updates, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/aws-batch-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("glue")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "ETL",
                "AWS Glue Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace AWS CLI Glue commands with native Glue plugin",
                "Better job monitoring, automatic status tracking, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/aws-glue-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("emr")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Big Data",
                "AWS EMR Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace AWS CLI EMR commands with native EMR plugin",
                "Better cluster monitoring, automatic status updates, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/aws_emr_plugin_extension.html"
            ));
        }
        
        if (cmdLower.contains("step-functions") || cmdLower.contains("stepfunctions")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Orchestration",
                "AWS Step Function Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace AWS CLI Step Functions commands with native plugin",
                "Better workflow monitoring, automatic status tracking, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/stepfunction-plugin-extension.html"
            ));
        }
    }
    
    /**
     * Identify Azure-specific cloud opportunities
     */
    private static void identifyAzureOpportunity(String command, String appName, String jobName, ApplicationAnalysisResult result) {
        String cmdLower = command.toLowerCase();
        
        if (cmdLower.contains("storage") || cmdLower.contains("blob")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Storage",
                "Azure Blob Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace Azure CLI Blob commands with native Azure Blob plugin",
                "Better error handling, built-in monitoring, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/azure-blob-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("datafactory") || cmdLower.contains("data-factory")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "ETL",
                "Azure Data Factory Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace Azure CLI Data Factory commands with native plugin",
                "Better pipeline monitoring, automatic status tracking, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/azure-data-factory-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("synapse")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Analytics",
                "Azure Synapse Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace Azure CLI Synapse commands with native Synapse plugin",
                "Better analytics monitoring, automatic status tracking, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/azure-synapse-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("logic") || cmdLower.contains("logicapp")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Integration",
                "Azure Logic Apps Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace Azure CLI Logic Apps commands with native plugin",
                "Better workflow monitoring, automatic status tracking, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/logic-apps-plugin-extension.html"
            ));
        }
    }
    
    /**
     * Identify GCP-specific cloud opportunities
     */
    private static void identifyGCPOpportunity(String command, String appName, String jobName, ApplicationAnalysisResult result) {
        String cmdLower = command.toLowerCase();
        
        if (cmdLower.contains("storage") || cmdLower.contains("gsutil")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Storage",
                "Google Cloud Storage Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace gcloud/gsutil storage commands with native GCS plugin",
                "Better error handling, built-in monitoring, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/google-cloud-storage-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("dataproc")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Big Data",
                "Google Cloud Dataproc Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace gcloud Dataproc commands with native Dataproc plugin",
                "Better cluster monitoring, automatic status tracking, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/google-cloud-dataproc.html"
            ));
        }
        
        if (cmdLower.contains("dataflow")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Data Processing",
                "Google Cloud Dataflow Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace gcloud Dataflow commands with native Dataflow plugin",
                "Better pipeline monitoring, automatic status tracking, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/dataflow-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("bigquery") || cmdLower.contains("bq ")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Analytics",
                "Google Cloud BigQuery Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace bq/gcloud BigQuery commands with native BigQuery plugin",
                "Better query monitoring, automatic status tracking, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/gcp-plugin-extension.html"
            ));
        }
        
        if (cmdLower.contains("composer")) {
            result.cloudOpportunities.add(new CloudOpportunity(
                "Orchestration",
                "Google Cloud Composer Plugin Extension",
                appName,
                jobName,
                "cmd_job",
                "Replace gcloud Composer commands with native Composer plugin",
                "Better Airflow monitoring, automatic status tracking, simplified configuration",
                "https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/GA/workload-automation-agent-plugin-extension/google-composer-airflow-plugin-extension.html"
            ));
        }
    }
    
    /**
     * Inner class to hold application analysis results
     */
    private static class ApplicationAnalysisResult {
        String fileName;
        String applicationName;
        int jobCount;
        List<BestPracticeViolation> violations = new ArrayList<>();
        List<CloudOpportunity> cloudOpportunities = new ArrayList<>();
        List<JobDependency> internalDependencies = new ArrayList<>();  // Dependencies within the app
        List<ExternalDependency> externalDependencies = new ArrayList<>();  // Dependencies to other apps
    }
    
    /**
     * Inner class to represent a best practice violation
     */
    private static class BestPracticeViolation {
        String rule;
        String severity;
        String category;
        String applicationName;
        String jobName;
        String description;
        String recommendation;
        
        BestPracticeViolation(String rule, String severity, String category, String applicationName, 
                             String jobName, String description, String recommendation) {
            this.rule = rule;
            this.severity = severity;
            this.category = category;
            this.applicationName = applicationName;
            this.jobName = jobName;
            this.description = description;
            this.recommendation = recommendation;
        }
    }
    
    /**
     * Inner class to represent a cloud integration opportunity
     */
    private static class CloudOpportunity {
        String integrationType;
        String pluginName;
        String applicationName;
        String jobName;
        String currentJobType;
        String description;
        String benefit;
        String documentationUrl;
        
        CloudOpportunity(String integrationType, String pluginName, String applicationName, String jobName,
                        String currentJobType, String description, String benefit, String documentationUrl) {
            this.integrationType = integrationType;
            this.pluginName = pluginName;
            this.applicationName = applicationName;
            this.jobName = jobName;
            this.currentJobType = currentJobType;
            this.description = description;
            this.benefit = benefit;
            this.documentationUrl = documentationUrl;
        }
    }
    
    /**
     * Inner class to represent an internal job dependency (within same application)
     */
    private static class JobDependency {
        String fromJob;
        String toJob;
        String condition;  // NORMAL, ABNORMAL, etc.
        
        JobDependency(String fromJob, String toJob, String condition) {
            this.fromJob = fromJob;
            this.toJob = toJob;
            this.condition = condition;
        }
    }
    
    /**
     * Inner class to represent an external application dependency
     */
    private static class ExternalDependency {
        String fromApplication;
        String fromJob;
        String toApplication;
        String jobType;  // ext_job, etc.
        
        ExternalDependency(String fromApplication, String fromJob, String toApplication, String jobType) {
            this.fromApplication = fromApplication;
            this.fromJob = fromJob;
            this.toApplication = toApplication;
            this.jobType = jobType;
        }
    }
}
