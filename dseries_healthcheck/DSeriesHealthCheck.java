import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import java.util.Date;

/**
 * ESP dSeries Workload Automation Health Check Tool - Enhanced Version
 * 
 * Comprehensive health check tool with external SQL configuration support.
 * Performs technical reviews including architecture, security, database,
 * agents, clients, high availability, and performance checks.
 * 
 * Version: 2.0.0
 * Date: 2026-02-11
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
    private static String dbConfigFile = "config/db.properties";
    private static String sqlConfigFile = "config/health_check_queries.sql";
    
    // Database connection details
    private static String dbHost = "localhost";
    private static int dbPort = 5432;
    private static String dbName = "dseries";
    private static String dbUser = "dseries_user";
    private static String dbPassword = "";
    private static String dbDriver = "org.postgresql.Driver";
    private static Connection dbConnection = null;
    
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
        
        if (args.length > 1) {
            dbConfigFile = args[1];
        }
        
        if (args.length > 2) {
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
        System.out.println("Usage: java DSeriesHealthCheck <install_dir> [db_config_file] [sql_config_file]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  install_dir      - dSeries installation directory (required)");
        System.out.println("  db_config_file   - Database configuration file (optional, default: config/db.properties)");
        System.out.println("  sql_config_file  - SQL queries configuration file (optional, default: config/health_check_queries.sql)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4");
        System.out.println("  java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4 config/db.properties");
        System.out.println("  java DSeriesHealthCheck C:/CA/ESPdSeriesWAServer_R12_4 config/db.properties config/health_check_queries.sql");
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
            File propsFile = new File(installDir, "conf/windows.service.properties");
            if (!propsFile.exists()) {
                propsFile = new File(installDir, "conf/server.properties");
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
            int serverPort = 7599;  // Default port
            
            // Try to read port from configuration
            File propsFile = new File(installDir, "conf/server.properties");
            if (propsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(propsFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("server.port=")) {
                        serverPort = Integer.parseInt(line.substring("server.port=".length()).trim());
                        break;
                    }
                }
                reader.close();
            }
            
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
            File configFile = new File(installDir, dbConfigFile);
            if (!configFile.exists()) {
                configFile = new File(dbConfigFile);
            }
            
            if (!configFile.exists()) {
                System.out.println("  ⚠️  Database configuration file not found: " + dbConfigFile);
                System.out.println("  Database checks will be skipped");
                System.out.println();
                return;
            }
            
            Properties props = new Properties();
            props.load(new FileInputStream(configFile));
            
            String jdbcUrl = props.getProperty("jdbc.URL", "");
            dbUser = props.getProperty("rdbms.userid", dbUser);
            dbPassword = props.getProperty("rdbms.password", "");
            dbDriver = props.getProperty("rdbms.driver", dbDriver);
            
            // Parse JDBC URL
            if (jdbcUrl.contains("postgresql")) {
                Pattern pattern = Pattern.compile("jdbc:postgresql://([^:]+):(\\d+)/(.+)");
                Matcher matcher = pattern.matcher(jdbcUrl);
                if (matcher.find()) {
                    dbHost = matcher.group(1);
                    dbPort = Integer.parseInt(matcher.group(2));
                    dbName = matcher.group(3);
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
            // Note: This is informational only - actual JDBC driver may not be available
            System.out.println("  Database connection details:");
            System.out.println("    Driver: " + dbDriver);
            System.out.println("    URL: jdbc:postgresql://" + maskSensitiveData(dbHost) + ":" + dbPort + "/" + maskSensitiveData(dbName));
            System.out.println("    User: " + maskSensitiveData(dbUser));
            System.out.println();
            System.out.println("  ⚠️  Note: Database checks require JDBC driver in classpath");
            System.out.println("  Add PostgreSQL JDBC driver: java -cp postgresql.jar:. DSeriesHealthCheck");
            System.out.println();
            
            // Try to load driver and connect
            Class.forName(dbDriver);
            String jdbcUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
            dbConnection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
            
            System.out.println("  ✅ Database connection established");
            System.out.println();
            return true;
            
        } catch (ClassNotFoundException e) {
            System.out.println("  ⚠️  JDBC driver not found: " + e.getMessage());
            System.out.println("  Database checks will be skipped");
            System.out.println();
            return false;
        } catch (SQLException e) {
            System.out.println("  ⚠️  Could not connect to database: " + e.getMessage());
            System.out.println("  Database checks will be skipped");
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
        
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(check.query);
            
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
                checkResults.add(new CheckResult(check.checkId, check.checkName, check.category,
                    check.severity, status, message, check.remediation));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            warningChecks++;
            String message = "⚠️  Query failed: " + e.getMessage();
            System.out.println("  " + message);
            checkResults.add(new CheckResult(check.checkId, check.checkName, check.category,
                check.severity, "WARNING", message, "Check query syntax and table existence"));
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
}
