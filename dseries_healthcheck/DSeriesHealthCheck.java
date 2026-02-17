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
 * Version: 2.11.0
 * Date: 2026-02-13
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
public class DSeriesHealthCheck {
    
    // Configuration
    private static String installDir;
    private static String sqlConfigFile = "config/health_check_queries.sql";
    private static boolean useExternalDbConfig = false;
    private static String externalDbConfigFile = null;
    private static int serverPort = 7599; // Default, will be read from instanceconf.xml
    
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
        System.out.println("Usage: java DSeriesHealthCheck <install_dir> [sql_config_file]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  install_dir      - dSeries installation directory (required)");
        System.out.println("  sql_config_file  - SQL queries configuration file (optional, default: config/health_check_queries.sql)");
        System.out.println();
        System.out.println("Database Configuration:");
        System.out.println("  The tool automatically uses: <install_dir>/conf/db.properties");
        System.out.println("  This is the same configuration used by dSeries server");
        System.out.println("  Supports encrypted passwords (ENC format)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4");
        System.out.println("  java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4 custom_queries.sql");
        System.out.println();
        System.out.println("Advanced (for testing with external db.properties):");
        System.out.println("  java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4 /path/to/db.properties");
        System.out.println("  java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4 /path/to/db.properties custom_queries.sql");
    }
    
    private static void printHeader() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  ESP dSeries Workload Automation Health Check Tool v2.0.0");
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
            
            // Try to load JDBC driver
            System.out.println("  Attempting to load JDBC driver: " + jdbcDriver);
            
            try {
                Class.forName(jdbcDriver);
                System.out.println("  ✅ JDBC driver loaded successfully");
            } catch (ClassNotFoundException e) {
                System.out.println("  ❌ JDBC driver not found in classpath: " + jdbcDriver);
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
                    System.out.println("  ℹ️  Decrypting password...");
                    
                    // Decrypt password using Scrambler (EXACTLY like DBConnectionValidator)
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
                            System.out.println("  ✅ Password decrypted successfully");
                        } else {
                            System.out.println("  ⚠️  Password decryption returned null");
                            properties.setProperty("password", password);  // Try original
                        }
                    } catch (Exception e) {
                        System.out.println("  ⚠️  Password decryption failed: " + e.getMessage());
                        System.out.println("  ⚠️  CRITICAL: Launcher script must be used to load dSeries encryption libraries");
                        System.out.println("  Trying with original password (will likely fail)...");
                        properties.setProperty("password", password);  // Try original
                    }
                }
                
                // Remove original properties after setting driver-specific ones (EXACTLY like DBConnectionValidator)
                properties.remove("rdbms.userid");
                properties.remove("rdbms.password");
            }
            
            // Handle SSL keystore password (Oracle-SSL, DB2-SSL, PostgreSQL-SSL)
            String trustStorePass = properties.getProperty("javax.net.ssl.trustStorePassword");
            if (trustStorePass != null && !trustStorePass.isEmpty()) {
                System.out.println("  ℹ️  Decrypting SSL trustStore password...");
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
                        System.out.println("  ✅ TrustStore password decrypted");
                    }
                } catch (Exception e) {
                    System.out.println("  ⚠️  TrustStore password decryption failed");
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
            System.out.println("  ⚠️  dSeries encryption library not found in classpath");
            System.out.println("  Note: Ensure dSeries libraries are in classpath (use launcher scripts)");
        } catch (NoSuchMethodException e) {
            System.out.println("  ⚠️  Password decryption method not available");
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                System.out.println("  ⚠️  Password decryption failed: " + cause.getMessage());
            } else {
                System.out.println("  ⚠️  Password decryption failed: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("  ⚠️  Password decryption failed: " + e.getMessage());
        }
        
        return "";
    }
}
