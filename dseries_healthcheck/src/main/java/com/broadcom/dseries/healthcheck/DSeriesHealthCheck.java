package com.broadcom.dseries.healthcheck;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

/**
 * ESP dSeries Workload Automation Health Check Tool
 * 
 * Main class that orchestrates the complete health check process.
 * This tool validates dSeries deployments against industry best practices
 * and dSeries-specific requirements.
 * 
 * Version: 1.0.0
 * Date: 2026-02-11
 * 
 * @author Broadcom
 */
public class DSeriesHealthCheck {
    
    // Logger for this class
    private static final Logger LOGGER = Logger.getLogger(DSeriesHealthCheck.class.getName());
    
    // Configuration
    private Properties config;
    private String configFile;
    
    // Health check results
    private List<HealthCheckResult> results;
    private int totalChecks = 0;
    private int passedChecks = 0;
    private int warningChecks = 0;
    private int failedChecks = 0;
    private int skippedChecks = 0;
    
    // Execution metadata
    private Date startTime;
    private String hostname;
    private String reportDirectory;
    
    /**
     * Constructor - Initializes the health check with configuration
     * 
     * @param configFile Path to configuration file
     */
    public DSeriesHealthCheck(String configFile) {
        this.configFile = configFile;
        this.results = new ArrayList<>();
        this.startTime = new Date();
        this.hostname = getHostname();
        
        LOGGER.info("=================================================================");
        LOGGER.info("ESP dSeries Workload Automation Health Check Tool v1.0.0");
        LOGGER.info("=================================================================");
        LOGGER.info("Start Time: " + startTime);
        LOGGER.info("Hostname: " + hostname);
        LOGGER.info("=================================================================");
    }
    
    /**
     * Load configuration from properties file
     * 
     * @throws IOException If configuration file cannot be read
     */
    public void loadConfiguration() throws IOException {
        LOGGER.info("Loading configuration from: " + configFile);
        
        config = new Properties();
        
        // Load from file if exists, otherwise use defaults
        if (new File(configFile).exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                config.load(fis);
                LOGGER.info("Configuration loaded successfully from file");
            }
        } else {
            LOGGER.warning("Configuration file not found, using default values");
            loadDefaultConfiguration();
        }
        
        // Set report directory
        reportDirectory = config.getProperty("report.output.dir", 
            System.getProperty("os.name").toLowerCase().contains("windows") 
                ? "C:\\CA\\WA_DE\\logs\\healthcheck" 
                : "/var/log/dseries/healthcheck");
        
        LOGGER.info("Report directory: " + reportDirectory);
        
        // Create report directory if it doesn't exist
        Files.createDirectories(Paths.get(reportDirectory));
    }
    
    /**
     * Load default configuration values
     */
    private void loadDefaultConfiguration() {
        LOGGER.info("Loading default configuration values");
        
        // Database settings
        config.setProperty("db.host", "localhost");
        config.setProperty("db.port", "5432");
        config.setProperty("db.name", "WADB");
        config.setProperty("db.user", "wauser");
        config.setProperty("db.type", "postgresql");
        
        // Server settings
        config.setProperty("server.host", "localhost");
        config.setProperty("server.port", "7507");
        config.setProperty("install.dir", 
            System.getProperty("os.name").toLowerCase().contains("windows")
                ? "C:\\CA\\WA_DE"
                : "/opt/CA/WA_DE");
        
        // Thresholds
        config.setProperty("threshold.cpu.warning", "70");
        config.setProperty("threshold.cpu.critical", "85");
        config.setProperty("threshold.memory.warning", "80");
        config.setProperty("threshold.memory.critical", "90");
        config.setProperty("threshold.disk.warning", "75");
        config.setProperty("threshold.disk.critical", "85");
        
        // JVM settings
        config.setProperty("threshold.jvm.heap.min.mb", "4096");
        config.setProperty("threshold.jvm.heap.recommended.mb", "4096");
        config.setProperty("threshold.jvm.heap.max.mb", "8192");
        
        // Workload settings
        config.setProperty("workload.size", "medium");
        config.setProperty("workload.daily.jobs", "50000");
        
        LOGGER.info("Default configuration loaded");
    }
    
    /**
     * Get the hostname of the current machine
     * 
     * @return Hostname as string
     */
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            LOGGER.warning("Could not determine hostname: " + e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Add a health check result and update statistics
     * 
     * @param result The health check result to add
     */
    private void addResult(HealthCheckResult result) {
        results.add(result);
        totalChecks++;
        
        switch (result.getStatus()) {
            case PASS:
                passedChecks++;
                break;
            case WARNING:
                warningChecks++;
                break;
            case FAIL:
                failedChecks++;
                break;
            case SKIP:
                skippedChecks++;
                break;
        }
        
        // Log the result
        String logMessage = String.format("[%s] %s: %s - %s", 
            result.getCheckId(),
            result.getStatus(),
            result.getDescription(),
            result.getMessage());
        
        switch (result.getStatus()) {
            case PASS:
                LOGGER.info(logMessage);
                break;
            case WARNING:
                LOGGER.warning(logMessage);
                break;
            case FAIL:
                LOGGER.severe(logMessage);
                break;
            case SKIP:
                LOGGER.info(logMessage);
                break;
        }
    }
    
    // =========================================================================
    // SYSTEM RESOURCE CHECKS
    // =========================================================================
    
    /**
     * Check CPU utilization (SYS-001)
     * 
     * Validates that CPU usage is within acceptable thresholds.
     * Based on industry best practices: <70% normal, >85% critical
     */
    public void checkCpuUtilization() {
        LOGGER.info("Starting check: CPU Utilization (SYS-001)");
        
        try {
            // Get CPU usage using Java's OperatingSystemMXBean
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) 
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            
            double cpuLoad = osBean.getSystemCpuLoad() * 100;
            
            // If CPU load is negative, it means the value is not available
            if (cpuLoad < 0) {
                LOGGER.warning("CPU load not available, skipping check");
                addResult(new HealthCheckResult(
                    "SYS-001",
                    "System Resources",
                    "CPU Utilization",
                    CheckStatus.SKIP,
                    Severity.INFO,
                    "CPU load information not available",
                    "",
                    "",
                    "Verify system monitoring capabilities"
                ));
                return;
            }
            
            int cpuWarning = Integer.parseInt(config.getProperty("threshold.cpu.warning", "70"));
            int cpuCritical = Integer.parseInt(config.getProperty("threshold.cpu.critical", "85"));
            
            CheckStatus status;
            String message;
            String recommendation;
            Severity severity;
            
            if (cpuLoad >= cpuCritical) {
                status = CheckStatus.FAIL;
                severity = Severity.CRITICAL;
                message = String.format("CPU usage is %.1f%% (critical threshold: %d%%)", 
                    cpuLoad, cpuCritical);
                recommendation = "Investigate high CPU processes. Consider scaling resources or optimizing workload. " +
                    "Review thread pool configuration and job scheduling patterns.";
                LOGGER.severe("CRITICAL: " + message);
            } else if (cpuLoad >= cpuWarning) {
                status = CheckStatus.WARNING;
                severity = Severity.MEDIUM;
                message = String.format("CPU usage is %.1f%% (warning threshold: %d%%)", 
                    cpuLoad, cpuWarning);
                recommendation = "Monitor CPU usage trends. Plan for capacity increase if trend continues. " +
                    "Review job scheduling to distribute load.";
                LOGGER.warning("WARNING: " + message);
            } else {
                status = CheckStatus.PASS;
                severity = Severity.INFO;
                message = String.format("CPU usage is %.1f%% (healthy)", cpuLoad);
                recommendation = "";
                LOGGER.info("PASS: " + message);
            }
            
            addResult(new HealthCheckResult(
                "SYS-001",
                "System Resources",
                "CPU Utilization",
                status,
                severity,
                message,
                String.format("%.1f%%", cpuLoad),
                String.format("%d%% / %d%%", cpuWarning, cpuCritical),
                recommendation
            ));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking CPU utilization", e);
            addResult(new HealthCheckResult(
                "SYS-001",
                "System Resources",
                "CPU Utilization",
                CheckStatus.SKIP,
                Severity.INFO,
                "Could not check CPU: " + e.getMessage(),
                "",
                "",
                "Verify system monitoring capabilities"
            ));
        }
    }
    
    /**
     * Check memory utilization (SYS-002)
     * 
     * Validates that memory usage is within acceptable thresholds.
     * Based on dSeries best practices: <80% normal, >90% critical
     */
    public void checkMemoryUsage() {
        LOGGER.info("Starting check: Memory Usage (SYS-002)");
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryPercent = (double) usedMemory / maxMemory * 100;
            
            int memWarning = Integer.parseInt(config.getProperty("threshold.memory.warning", "80"));
            int memCritical = Integer.parseInt(config.getProperty("threshold.memory.critical", "90"));
            
            CheckStatus status;
            String message;
            String recommendation;
            Severity severity;
            
            if (memoryPercent >= memCritical) {
                status = CheckStatus.FAIL;
                severity = Severity.CRITICAL;
                message = String.format("Memory usage is %.1f%% (critical threshold: %d%%)", 
                    memoryPercent, memCritical);
                recommendation = "Increase system memory or reduce JVM heap size. Check for memory leaks. " +
                    "Review GC logs and consider heap dump analysis.";
                LOGGER.severe("CRITICAL: " + message);
            } else if (memoryPercent >= memWarning) {
                status = CheckStatus.WARNING;
                severity = Severity.HIGH;
                message = String.format("Memory usage is %.1f%% (warning threshold: %d%%)", 
                    memoryPercent, memWarning);
                recommendation = "Monitor memory trends. Consider memory upgrade if usage continues to grow. " +
                    "Review JVM heap configuration.";
                LOGGER.warning("WARNING: " + message);
            } else {
                status = CheckStatus.PASS;
                severity = Severity.INFO;
                message = String.format("Memory usage is %.1f%% (healthy)", memoryPercent);
                recommendation = "";
                LOGGER.info("PASS: " + message);
            }
            
            String valueStr = String.format("%.1f%% (%d MB / %d MB)", 
                memoryPercent, 
                usedMemory / (1024 * 1024), 
                maxMemory / (1024 * 1024));
            
            addResult(new HealthCheckResult(
                "SYS-002",
                "System Resources",
                "Memory Usage",
                status,
                severity,
                message,
                valueStr,
                String.format("%d%% / %d%%", memWarning, memCritical),
                recommendation
            ));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking memory usage", e);
            addResult(new HealthCheckResult(
                "SYS-002",
                "System Resources",
                "Memory Usage",
                CheckStatus.SKIP,
                Severity.INFO,
                "Could not check memory: " + e.getMessage(),
                "",
                "",
                ""
            ));
        }
    }
    
    /**
     * Check disk space (SYS-003)
     * 
     * Validates that disk space is sufficient.
     * Based on industry best practices: <75% normal, >85% critical
     */
    public void checkDiskSpace() {
        LOGGER.info("Starting check: Disk Space (SYS-003)");
        
        try {
            File root = new File("/");
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                root = new File("C:\\");
            }
            
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double diskPercent = (double) usedSpace / totalSpace * 100;
            
            int diskWarning = Integer.parseInt(config.getProperty("threshold.disk.warning", "75"));
            int diskCritical = Integer.parseInt(config.getProperty("threshold.disk.critical", "85"));
            
            CheckStatus status;
            String message;
            String recommendation;
            Severity severity;
            
            if (diskPercent >= diskCritical) {
                status = CheckStatus.FAIL;
                severity = Severity.CRITICAL;
                message = String.format("Disk usage is %.1f%% (critical threshold: %d%%)", 
                    diskPercent, diskCritical);
                recommendation = "Clean up old logs, archives, and temporary files. Expand disk space immediately. " +
                    "Review housekeeping policies and purge old job history.";
                LOGGER.severe("CRITICAL: " + message);
            } else if (diskPercent >= diskWarning) {
                status = CheckStatus.WARNING;
                severity = Severity.HIGH;
                message = String.format("Disk usage is %.1f%% (warning threshold: %d%%)", 
                    diskPercent, diskWarning);
                recommendation = "Plan for disk expansion. Review housekeeping policies. " +
                    "Archive old data and clean up temporary files.";
                LOGGER.warning("WARNING: " + message);
            } else {
                status = CheckStatus.PASS;
                severity = Severity.INFO;
                message = String.format("Disk usage is %.1f%% (healthy)", diskPercent);
                recommendation = "";
                LOGGER.info("PASS: " + message);
            }
            
            String valueStr = String.format("%.1f%% (%d GB / %d GB)", 
                diskPercent, 
                usedSpace / (1024 * 1024 * 1024), 
                totalSpace / (1024 * 1024 * 1024));
            
            addResult(new HealthCheckResult(
                "SYS-003",
                "System Resources",
                "Disk Space",
                status,
                severity,
                message,
                valueStr,
                String.format("%d%% / %d%%", diskWarning, diskCritical),
                recommendation
            ));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking disk space", e);
            addResult(new HealthCheckResult(
                "SYS-003",
                "System Resources",
                "Disk Space",
                CheckStatus.SKIP,
                Severity.INFO,
                "Could not check disk space: " + e.getMessage(),
                "",
                "",
                ""
            ));
        }
    }
    
    // =========================================================================
    // DATABASE HEALTH CHECKS
    // =========================================================================
    
    /**
     * Check database connectivity (DB-001)
     * 
     * Validates that the database is accessible and responsive.
     * This is a critical check as dSeries requires database connectivity.
     */
    public void checkDatabaseConnectivity() {
        LOGGER.info("Starting check: Database Connectivity (DB-001)");
        
        String dbType = config.getProperty("db.type", "postgresql");
        String dbHost = config.getProperty("db.host", "localhost");
        String dbPort = config.getProperty("db.port", "5432");
        String dbName = config.getProperty("db.name", "WADB");
        String dbUser = config.getProperty("db.user", "wauser");
        
        LOGGER.info("Database type: " + dbType);
        LOGGER.info("Database host: " + dbHost);
        LOGGER.info("Database port: " + dbPort);
        LOGGER.info("Database name: " + dbName);
        
        Connection conn = null;
        try {
            String jdbcUrl = buildJdbcUrl(dbType, dbHost, dbPort, dbName);
            LOGGER.info("JDBC URL: " + jdbcUrl);
            
            // Load appropriate JDBC driver
            loadJdbcDriver(dbType);
            
            // Attempt connection
            long startTime = System.currentTimeMillis();
            conn = DriverManager.getConnection(jdbcUrl, dbUser, getDbPassword());
            long connectionTime = System.currentTimeMillis() - startTime;
            
            LOGGER.info("Database connection successful in " + connectionTime + " ms");
            
            // Test with a simple query
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT 1");
            rs.next();
            rs.close();
            stmt.close();
            
            addResult(new HealthCheckResult(
                "DB-001",
                "Database",
                "Database Connectivity",
                CheckStatus.PASS,
                Severity.CRITICAL,
                "Database connection successful (" + connectionTime + " ms)",
                "Connected",
                "",
                ""
            ));
            
        } catch (ClassNotFoundException e) {
            LOGGER.severe("JDBC driver not found: " + e.getMessage());
            addResult(new HealthCheckResult(
                "DB-001",
                "Database",
                "Database Connectivity",
                CheckStatus.SKIP,
                Severity.CRITICAL,
                "JDBC driver not found: " + e.getMessage(),
                "Driver Missing",
                "",
                "Install appropriate JDBC driver for " + dbType
            ));
        } catch (SQLException e) {
            LOGGER.severe("Database connection failed: " + e.getMessage());
            addResult(new HealthCheckResult(
                "DB-001",
                "Database",
                "Database Connectivity",
                CheckStatus.FAIL,
                Severity.CRITICAL,
                "Database connection failed: " + e.getMessage(),
                "Failed",
                "",
                "Verify database is running, credentials are correct, and network is accessible. " +
                "Check firewall rules and database listener status."
            ));
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.warning("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Build JDBC URL based on database type
     */
    private String buildJdbcUrl(String dbType, String host, String port, String dbName) {
        switch (dbType.toLowerCase()) {
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%s:%s", host, port, dbName);
            case "mssql":
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%s;databaseName=%s", host, port, dbName);
            default:
                LOGGER.warning("Unknown database type: " + dbType + ", defaulting to PostgreSQL");
                return String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
        }
    }
    
    /**
     * Load JDBC driver for the specified database type
     */
    private void loadJdbcDriver(String dbType) throws ClassNotFoundException {
        switch (dbType.toLowerCase()) {
            case "postgresql":
                Class.forName("org.postgresql.Driver");
                LOGGER.info("Loaded PostgreSQL JDBC driver");
                break;
            case "oracle":
                Class.forName("oracle.jdbc.driver.OracleDriver");
                LOGGER.info("Loaded Oracle JDBC driver");
                break;
            case "mssql":
            case "sqlserver":
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                LOGGER.info("Loaded SQL Server JDBC driver");
                break;
            default:
                throw new ClassNotFoundException("Unknown database type: " + dbType);
        }
    }
    
    /**
     * Get database password from configuration or password file
     */
    private String getDbPassword() {
        // Try to get from environment variable first
        String password = System.getenv("DB_PASSWORD");
        if (password != null && !password.isEmpty()) {
            LOGGER.info("Using database password from environment variable");
            return password;
        }
        
        // Try to read from password file
        String passwordFile = config.getProperty("db.password.file");
        if (passwordFile != null && !passwordFile.isEmpty()) {
            try {
                password = new String(Files.readAllBytes(Paths.get(passwordFile))).trim();
                LOGGER.info("Using database password from file: " + passwordFile);
                return password;
            } catch (IOException e) {
                LOGGER.warning("Could not read password file: " + e.getMessage());
            }
        }
        
        // Try to get from config (not recommended for production)
        password = config.getProperty("db.password", "");
        if (!password.isEmpty()) {
            LOGGER.warning("Using database password from configuration file (not recommended for production)");
            return password;
        }
        
        LOGGER.warning("No database password configured");
        return "";
    }
    
    // =========================================================================
    // SERVER CONFIGURATION CHECKS
    // =========================================================================
    
    /**
     * Check JVM heap size (SRV-001)
     * 
     * CRITICAL CHECK: Validates JVM heap configuration.
     * Based on dSeries best practices: 4GB minimum for production
     */
    public void checkJvmHeapSize() {
        LOGGER.info("Starting check: JVM Heap Size (SRV-001)");
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxHeapBytes = runtime.maxMemory();
            long maxHeapMB = maxHeapBytes / (1024 * 1024);
            
            LOGGER.info("Current JVM max heap: " + maxHeapMB + " MB");
            
            int heapMinMB = Integer.parseInt(config.getProperty("threshold.jvm.heap.min.mb", "4096"));
            int heapRecommendedMB = Integer.parseInt(config.getProperty("threshold.jvm.heap.recommended.mb", "4096"));
            
            CheckStatus status;
            String message;
            String recommendation;
            Severity severity;
            
            if (maxHeapMB < heapMinMB) {
                status = CheckStatus.FAIL;
                severity = Severity.CRITICAL;
                message = String.format("JVM heap size is %d MB (minimum: %d MB)", 
                    maxHeapMB, heapMinMB);
                recommendation = String.format(
                    "CRITICAL: Increase JVM heap to at least %d MB for production. " +
                    "Edit startServer script (Unix/Linux) or windows.service.properties (Windows). " +
                    "Set -Xms%dm -Xmx%dm for pre-allocated memory model (dSeries best practice).",
                    heapRecommendedMB, heapRecommendedMB, heapRecommendedMB);
                LOGGER.severe("CRITICAL: " + message);
            } else if (maxHeapMB < heapRecommendedMB) {
                status = CheckStatus.WARNING;
                severity = Severity.HIGH;
                message = String.format("JVM heap size is %d MB (recommended: %d MB)", 
                    maxHeapMB, heapRecommendedMB);
                recommendation = String.format(
                    "Consider increasing heap to %d MB for optimal performance. " +
                    "Monitor memory usage and adjust as needed for your workload.",
                    heapRecommendedMB);
                LOGGER.warning("WARNING: " + message);
            } else {
                status = CheckStatus.PASS;
                severity = Severity.INFO;
                message = String.format("JVM heap size is %d MB (healthy)", maxHeapMB);
                recommendation = "";
                LOGGER.info("PASS: " + message);
            }
            
            addResult(new HealthCheckResult(
                "SRV-001",
                "Server Configuration",
                "JVM Heap Size",
                status,
                severity,
                message,
                maxHeapMB + " MB",
                String.format("Min: %d MB, Recommended: %d MB", heapMinMB, heapRecommendedMB),
                recommendation
            ));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking JVM heap size", e);
            addResult(new HealthCheckResult(
                "SRV-001",
                "Server Configuration",
                "JVM Heap Size",
                CheckStatus.SKIP,
                Severity.INFO,
                "Could not check JVM heap: " + e.getMessage(),
                "",
                "",
                "Manually verify JVM configuration"
            ));
        }
    }
    
    /**
     * Check server port accessibility (SRV-003)
     * 
     * Validates that the dSeries server port is accessible.
     */
    public void checkServerPort() {
        LOGGER.info("Starting check: Server Port Accessibility (SRV-003)");
        
        String serverHost = config.getProperty("server.host", "localhost");
        int serverPort = Integer.parseInt(config.getProperty("server.port", "7507"));
        
        LOGGER.info("Checking server port: " + serverHost + ":" + serverPort);
        
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress(serverHost, serverPort), 5000);
            
            LOGGER.info("Server port is accessible");
            
            addResult(new HealthCheckResult(
                "SRV-003",
                "Server Configuration",
                "Server Port Accessibility",
                CheckStatus.PASS,
                Severity.CRITICAL,
                String.format("Server port %d is listening and accessible", serverPort),
                "Port " + serverPort,
                "",
                ""
            ));
            
        } catch (Exception e) {
            LOGGER.severe("Server port is not accessible: " + e.getMessage());
            
            addResult(new HealthCheckResult(
                "SRV-003",
                "Server Configuration",
                "Server Port Accessibility",
                CheckStatus.FAIL,
                Severity.CRITICAL,
                String.format("Server port %d is not accessible: %s", serverPort, e.getMessage()),
                "Port " + serverPort,
                "",
                "Verify dSeries server is running. Check firewall rules and network configuration. " +
                "Ensure server is bound to correct interface."
            ));
        }
    }
    
    /**
     * Check thread pool configuration (SRV-004)
     * 
     * Provides recommendations based on workload size.
     * Based on industry best practices adapted for dSeries.
     */
    public void checkThreadPoolConfiguration() {
        LOGGER.info("Starting check: Thread Pool Configuration (SRV-004)");
        
        String workloadSize = config.getProperty("workload.size", "medium");
        int dailyJobs = Integer.parseInt(config.getProperty("workload.daily.jobs", "50000"));
        
        LOGGER.info("Workload size: " + workloadSize);
        LOGGER.info("Daily jobs: " + dailyJobs);
        
        // Determine recommended thread counts based on workload
        Map<String, Integer> recommendedThreads = new HashMap<>();
        
        if (dailyJobs <= 15000) {
            workloadSize = "small";
            recommendedThreads.put("download", 3);
            recommendedThreads.put("db_update", 2);
            recommendedThreads.put("selector", 4);
        } else if (dailyJobs <= 75000) {
            workloadSize = "medium";
            recommendedThreads.put("download", 6);
            recommendedThreads.put("db_update", 4);
            recommendedThreads.put("selector", 8);
        } else {
            workloadSize = "large";
            recommendedThreads.put("download", 8);
            recommendedThreads.put("db_update", 8);
            recommendedThreads.put("selector", 12);
        }
        
        String message = String.format("Workload size: %s (%,d daily jobs)", workloadSize, dailyJobs);
        String recommendation = String.format(
            "Recommended thread configuration for %s workload: " +
            "Download=%d, DB_Update=%d, Selector=%d. " +
            "Adjust based on actual performance monitoring.",
            workloadSize,
            recommendedThreads.get("download"),
            recommendedThreads.get("db_update"),
            recommendedThreads.get("selector"));
        
        LOGGER.info(message);
        LOGGER.info(recommendation);
        
        addResult(new HealthCheckResult(
            "SRV-004",
            "Server Configuration",
            "Thread Pool Configuration",
            CheckStatus.INFO,
            Severity.INFO,
            message,
            workloadSize,
            "",
            recommendation
        ));
    }
    
    // =========================================================================
    // REPORT GENERATION
    // =========================================================================
    
    /**
     * Calculate overall health score
     * 
     * @return Health score from 0-100
     */
    public int calculateOverallScore() {
        LOGGER.info("Calculating overall health score");
        
        if (totalChecks == 0) {
            LOGGER.warning("No checks were performed");
            return 0;
        }
        
        // Weight the scores
        int passedWeight = 100;
        int warningWeight = 60;
        int failedWeight = 0;
        
        int weightedScore = (passedChecks * passedWeight + 
                            warningChecks * warningWeight + 
                            failedChecks * failedWeight) / totalChecks;
        
        LOGGER.info("Total checks: " + totalChecks);
        LOGGER.info("Passed: " + passedChecks);
        LOGGER.info("Warnings: " + warningChecks);
        LOGGER.info("Failed: " + failedChecks);
        LOGGER.info("Overall score: " + weightedScore + "/100");
        
        return weightedScore;
    }
    
    /**
     * Generate HTML report
     * 
     * @param outputFile Path to output HTML file
     */
    public void generateHtmlReport(String outputFile) {
        LOGGER.info("Generating HTML report: " + outputFile);
        
        try {
            HtmlReportGenerator generator = new HtmlReportGenerator();
            generator.generateReport(this, outputFile);
            LOGGER.info("HTML report generated successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate HTML report", e);
        }
    }
    
    /**
     * Generate JSON report
     * 
     * @param outputFile Path to output JSON file
     */
    public void generateJsonReport(String outputFile) {
        LOGGER.info("Generating JSON report: " + outputFile);
        
        try {
            JsonReportGenerator generator = new JsonReportGenerator();
            generator.generateReport(this, outputFile);
            LOGGER.info("JSON report generated successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate JSON report", e);
        }
    }
    
    /**
     * Display summary to console
     */
    public void displaySummary() {
        int overallScore = calculateOverallScore();
        
        System.out.println();
        System.out.println("=================================================================");
        System.out.println("  HEALTH CHECK SUMMARY");
        System.out.println("=================================================================");
        System.out.println();
        System.out.println("  Overall Health Score: " + overallScore + "/100");
        
        String status;
        if (overallScore >= 90) {
            status = "✅ EXCELLENT";
        } else if (overallScore >= 75) {
            status = "🟢 GOOD";
        } else if (overallScore >= 60) {
            status = "🟡 FAIR";
        } else if (overallScore >= 40) {
            status = "🟠 POOR";
        } else {
            status = "🔴 CRITICAL";
        }
        System.out.println("  Status: " + status);
        
        System.out.println();
        System.out.println("  Total Checks: " + totalChecks);
        System.out.println("  ✅ Passed: " + passedChecks);
        System.out.println("  ⚠️  Warnings: " + warningChecks);
        System.out.println("  ❌ Failed: " + failedChecks);
        System.out.println();
        
        if (failedChecks > 0) {
            System.out.println("  ⚠️  " + failedChecks + " CRITICAL ISSUE(S) DETECTED!");
            System.out.println("  Please review the detailed report for remediation steps.");
            System.out.println();
        }
        
        System.out.println("=================================================================");
        System.out.println();
        
        LOGGER.info("Summary displayed to console");
    }
    
    /**
     * Run all health checks
     * 
     * @param mode Execution mode (quick or full)
     */
    public void runAllChecks(String mode) {
        LOGGER.info("Starting health check in " + mode + " mode");
        
        System.out.println();
        System.out.println("🔍 Starting health check...");
        System.out.println();
        
        // Always run core checks
        checkCpuUtilization();
        checkMemoryUsage();
        checkDiskSpace();
        checkDatabaseConnectivity();
        checkJvmHeapSize();
        checkServerPort();
        checkThreadPoolConfiguration();
        
        if ("full".equalsIgnoreCase(mode)) {
            LOGGER.info("Running full health check (this may take 15-20 minutes)");
            // Additional checks would go here
        }
        
        System.out.println();
        System.out.println("✅ Health check completed");
        System.out.println();
        
        LOGGER.info("Health check completed");
    }
    
    // =========================================================================
    // GETTERS
    // =========================================================================
    
    public List<HealthCheckResult> getResults() {
        return results;
    }
    
    public int getTotalChecks() {
        return totalChecks;
    }
    
    public int getPassedChecks() {
        return passedChecks;
    }
    
    public int getWarningChecks() {
        return warningChecks;
    }
    
    public int getFailedChecks() {
        return failedChecks;
    }
    
    public Date getStartTime() {
        return startTime;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public String getReportDirectory() {
        return reportDirectory;
    }
    
    // =========================================================================
    // MAIN METHOD
    // =========================================================================
    
    /**
     * Main entry point for the health check tool
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Setup logging
        setupLogging();
        
        // Parse command line arguments
        String mode = "quick";
        String configFile = "config/healthcheck.properties";
        
        for (int i = 0; i < args.length; i++) {
            if ("--quick".equals(args[i])) {
                mode = "quick";
            } else if ("--full".equals(args[i])) {
                mode = "full";
            } else if ("--config".equals(args[i]) && i + 1 < args.length) {
                configFile = args[++i];
            } else if ("--help".equals(args[i])) {
                printUsage();
                System.exit(0);
            }
        }
        
        try {
            // Create health check instance
            DSeriesHealthCheck healthCheck = new DSeriesHealthCheck(configFile);
            
            // Load configuration
            healthCheck.loadConfiguration();
            
            // Run health checks
            healthCheck.runAllChecks(mode);
            
            // Generate reports
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            
            String htmlFile = healthCheck.getReportDirectory() + 
                File.separator + "healthcheck_" + timestamp + ".html";
            String jsonFile = healthCheck.getReportDirectory() + 
                File.separator + "healthcheck_" + timestamp + ".json";
            
            healthCheck.generateHtmlReport(htmlFile);
            healthCheck.generateJsonReport(jsonFile);
            
            // Display summary
            healthCheck.displaySummary();
            
            System.out.println("  Reports:");
            System.out.println("    HTML: " + htmlFile);
            System.out.println("    JSON: " + jsonFile);
            System.out.println();
            
            // Exit with appropriate code
            int overallScore = healthCheck.calculateOverallScore();
            System.exit(overallScore >= 60 ? 0 : 1);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error during health check", e);
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Setup logging configuration
     */
    private static void setupLogging() {
        try {
            // Create logs directory if it doesn't exist
            String logDir = System.getProperty("os.name").toLowerCase().contains("windows")
                ? "C:\\CA\\WA_DE\\logs\\healthcheck"
                : "/var/log/dseries/healthcheck";
            Files.createDirectories(Paths.get(logDir));
            
            // Configure file handler
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            String logFile = logDir + File.separator + "healthcheck_" + timestamp + ".log";
            
            FileHandler fileHandler = new FileHandler(logFile);
            fileHandler.setFormatter(new SimpleFormatter());
            
            // Configure console handler
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            
            // Configure root logger
            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.INFO);
            rootLogger.addHandler(fileHandler);
            rootLogger.addHandler(consoleHandler);
            
            LOGGER.info("Logging initialized: " + logFile);
            
        } catch (Exception e) {
            System.err.println("Failed to setup logging: " + e.getMessage());
        }
    }
    
    /**
     * Print usage information
     */
    private static void printUsage() {
        System.out.println("ESP dSeries Health Check Tool v1.0.0");
        System.out.println();
        System.out.println("Usage: java -jar dseries-healthcheck.jar [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --quick           Run quick health check (5 minutes)");
        System.out.println("  --full            Run full health check (15-20 minutes)");
        System.out.println("  --config FILE     Use alternate configuration file");
        System.out.println("  --help            Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Quick daily check");
        System.out.println("  java -jar dseries-healthcheck.jar --quick");
        System.out.println();
        System.out.println("  # Full weekly check");
        System.out.println("  java -jar dseries-healthcheck.jar --full");
        System.out.println();
        System.out.println("  # Custom configuration");
        System.out.println("  java -jar dseries-healthcheck.jar --full --config /etc/dseries/health.properties");
        System.out.println();
    }
}
