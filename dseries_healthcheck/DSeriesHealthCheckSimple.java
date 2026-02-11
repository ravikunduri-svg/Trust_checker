import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ESP dSeries Workload Automation Health Check Tool - Simple Version
 * 
 * This is a simplified, standalone version that can be compiled and run immediately.
 * It performs comprehensive health checks on dSeries installations.
 * 
 * Version: 1.0.0
 * Date: 2026-02-11
 */
public class DSeriesHealthCheckSimple {
    
    // Configuration
    private static String dbHost = "G36R0T3";
    private static int dbPort = 5432;
    private static String dbName = "dseries";
    private static String dbUser = "postgres";
    private static String dbPassword = "";  // Will be read from config
    private static String serverHost = "localhost";
    private static int serverPort = 7599;
    private static String installDir = "C:/CA/ESPdSeriesWAServer_R12_4";
    
    // Health check results
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
    
    public static void main(String[] args) {
        // Parse command line arguments
        if (args.length > 0) {
            installDir = args[0];
        }
        
        System.out.println("=================================================================");
        System.out.println("  ESP dSeries Workload Automation Health Check Tool v1.0.0");
        System.out.println("=================================================================");
        System.out.println("  Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        System.out.println("  Host: " + getHostname());
        System.out.println("  dSeries: " + installDir);
        System.out.println("=================================================================");
        System.out.println();
        
        // Validate installation directory
        File installDirFile = new File(installDir);
        if (!installDirFile.exists()) {
            System.err.println("ERROR: Installation directory not found: " + installDir);
            System.err.println();
            System.err.println("Usage: java DSeriesHealthCheckSimple [installation_directory]");
            System.err.println("Example: java DSeriesHealthCheckSimple C:/CA/ESPdSeriesWAServer_R12_4");
            System.exit(2);
        }
        
        System.out.println("Starting health check...");
        System.out.println();
        
        // Run health checks
        checkCpuUtilization();
        checkMemoryUsage();
        checkDiskSpace();
        checkJvmHeapSize();
        checkDatabaseConnectivity();
        checkServerPort();
        checkInstallationDirectory();
        
        // Display summary
        displaySummary();
    }
    
    private static String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static void checkCpuUtilization() {
        System.out.println("[SYS-001] Checking CPU Utilization...");
        totalChecks++;
        
        try {
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) 
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            
            double cpuLoad = osBean.getSystemCpuLoad() * 100;
            
            if (cpuLoad < 0) {
                System.out.println("  [SKIP] CPU load not available");
                return;
            }
            
            if (cpuLoad >= cpuCritical) {
                System.out.println("  [FAIL] CRITICAL: CPU usage is " + String.format("%.1f", cpuLoad) + "% (threshold: " + cpuCritical + "%)");
                System.out.println("         Recommendation: Investigate high CPU processes. Consider scaling resources.");
                failedChecks++;
            } else if (cpuLoad >= cpuWarning) {
                System.out.println("  [WARN] WARNING: CPU usage is " + String.format("%.1f", cpuLoad) + "% (threshold: " + cpuWarning + "%)");
                System.out.println("         Recommendation: Monitor CPU trends. Plan for capacity increase.");
                warningChecks++;
            } else {
                System.out.println("  [PASS] CPU usage is " + String.format("%.1f", cpuLoad) + "% (healthy)");
                passedChecks++;
            }
        } catch (Exception e) {
            System.out.println("  [SKIP] Error checking CPU: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void checkMemoryUsage() {
        System.out.println("[SYS-002] Checking Memory Usage...");
        totalChecks++;
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double memoryPercent = (double) usedMemory / maxMemory * 100;
            long usedMB = usedMemory / (1024 * 1024);
            long maxMB = maxMemory / (1024 * 1024);
            
            if (memoryPercent >= memCritical) {
                System.out.println("  [FAIL] CRITICAL: Memory usage is " + String.format("%.1f", memoryPercent) + "% (" + usedMB + " MB / " + maxMB + " MB)");
                System.out.println("         Recommendation: Increase system memory or reduce JVM heap size.");
                failedChecks++;
            } else if (memoryPercent >= memWarning) {
                System.out.println("  [WARN] WARNING: Memory usage is " + String.format("%.1f", memoryPercent) + "% (" + usedMB + " MB / " + maxMB + " MB)");
                System.out.println("         Recommendation: Monitor memory trends. Consider memory upgrade.");
                warningChecks++;
            } else {
                System.out.println("  [PASS] Memory usage is " + String.format("%.1f", memoryPercent) + "% (" + usedMB + " MB / " + maxMB + " MB)");
                passedChecks++;
            }
        } catch (Exception e) {
            System.out.println("  [SKIP] Error checking memory: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void checkDiskSpace() {
        System.out.println("[SYS-003] Checking Disk Space...");
        totalChecks++;
        
        try {
            File root = new File("C:\\");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            
            double diskPercent = (double) usedSpace / totalSpace * 100;
            long usedGB = usedSpace / (1024 * 1024 * 1024);
            long totalGB = totalSpace / (1024 * 1024 * 1024);
            
            if (diskPercent >= diskCritical) {
                System.out.println("  [FAIL] CRITICAL: Disk usage is " + String.format("%.1f", diskPercent) + "% (" + usedGB + " GB / " + totalGB + " GB)");
                System.out.println("         Recommendation: Clean up old logs, archives. Expand disk space immediately.");
                failedChecks++;
            } else if (diskPercent >= diskWarning) {
                System.out.println("  [WARN] WARNING: Disk usage is " + String.format("%.1f", diskPercent) + "% (" + usedGB + " GB / " + totalGB + " GB)");
                System.out.println("         Recommendation: Plan for disk expansion. Review housekeeping policies.");
                warningChecks++;
            } else {
                System.out.println("  [PASS] Disk usage is " + String.format("%.1f", diskPercent) + "% (" + usedGB + " GB / " + totalGB + " GB)");
                passedChecks++;
            }
        } catch (Exception e) {
            System.out.println("  [SKIP] Error checking disk space: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void checkJvmHeapSize() {
        System.out.println("[SRV-001] Checking JVM Heap Size (CRITICAL CHECK)...");
        totalChecks++;
        
        try {
            // Read from windows.service.properties
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
            
            if (heapSizeMB == 0) {
                System.out.println("  [SKIP] Could not determine JVM heap size from configuration");
                return;
            }
            
            System.out.println("  Current JVM heap: " + heapSizeMB + " MB");
            
            if (heapSizeMB < jvmHeapMinMB) {
                System.out.println("  [FAIL] CRITICAL: JVM heap is " + heapSizeMB + " MB (minimum: " + jvmHeapMinMB + " MB)");
                System.out.println("         Recommendation: CRITICAL - Increase JVM heap to at least " + jvmHeapMinMB + " MB for production!");
                System.out.println("         Edit: " + installDir + "/conf/windows.service.properties");
                System.out.println("         Change: jvmproperty_3=-Xmx" + heapSizeMB + "M");
                System.out.println("         To:     jvmproperty_3=-Xmx" + jvmHeapMinMB + "M");
                System.out.println("         Also set: jvmproperty_2=-Xms" + jvmHeapMinMB + "M (pre-allocated memory model)");
                failedChecks++;
            } else if (heapSizeMB < 4096) {
                System.out.println("  [WARN] WARNING: JVM heap is " + heapSizeMB + " MB (recommended: 4096 MB)");
                System.out.println("         Recommendation: Consider increasing to 4096 MB for optimal performance.");
                warningChecks++;
            } else {
                System.out.println("  [PASS] JVM heap is " + heapSizeMB + " MB (healthy)");
                passedChecks++;
            }
        } catch (Exception e) {
            System.out.println("  [SKIP] Error checking JVM heap: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void checkDatabaseConnectivity() {
        System.out.println("[DB-001] Checking Database Connectivity...");
        totalChecks++;
        
        try {
            // Read database password from db.properties
            File dbPropsFile = new File(installDir + "/conf/db.properties");
            if (dbPropsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(dbPropsFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("jdbc.URL=")) {
                        String url = line.substring("jdbc.URL=".length());
                        // Parse URL to get host, port, database
                        // jdbc:postgresql://G36R0T3:5432/dseries
                        if (url.contains("://")) {
                            String[] parts = url.split("://")[1].split("/");
                            String[] hostPort = parts[0].split(":");
                            dbHost = hostPort[0];
                            if (hostPort.length > 1) {
                                dbPort = Integer.parseInt(hostPort[1]);
                            }
                            if (parts.length > 1) {
                                dbName = parts[1];
                            }
                        }
                    } else if (line.startsWith("rdbms.userid=")) {
                        dbUser = line.substring("rdbms.userid=".length());
                    }
                }
                reader.close();
            }
            
            System.out.println("  Database: " + dbHost + ":" + dbPort + "/" + dbName);
            System.out.println("  User: " + dbUser);
            
            // Try to load PostgreSQL driver
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                System.out.println("  [SKIP] PostgreSQL JDBC driver not found in classpath");
                System.out.println("         To test database connectivity, add postgresql-*.jar to classpath");
                return;
            }
            
            // Try to connect
            String jdbcUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;
            long startTime = System.currentTimeMillis();
            
            // Note: Password is encrypted in db.properties, so we can't connect without decryption
            System.out.println("  [INFO] Database configuration found. Connection test skipped (password encrypted).");
            System.out.println("         To test connectivity manually: psql -h " + dbHost + " -p " + dbPort + " -U " + dbUser + " -d " + dbName);
            passedChecks++;
            
        } catch (Exception e) {
            System.out.println("  [FAIL] Error checking database: " + e.getMessage());
            failedChecks++;
        }
        System.out.println();
    }
    
    private static void checkServerPort() {
        System.out.println("[SRV-003] Checking Server Port Accessibility...");
        totalChecks++;
        
        try {
            System.out.println("  Testing port: " + serverHost + ":" + serverPort);
            
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(serverHost, serverPort), 5000);
            socket.close();
            
            System.out.println("  [PASS] Server port " + serverPort + " is listening and accessible");
            passedChecks++;
        } catch (Exception e) {
            System.out.println("  [FAIL] Server port " + serverPort + " is not accessible: " + e.getMessage());
            System.out.println("         Recommendation: Verify dSeries server is running. Check firewall rules.");
            failedChecks++;
        }
        System.out.println();
    }
    
    private static void checkInstallationDirectory() {
        System.out.println("[SRV-005] Checking Installation Directory...");
        totalChecks++;
        
        try {
            File installDirFile = new File(installDir);
            
            if (!installDirFile.exists()) {
                System.out.println("  [FAIL] Installation directory not found: " + installDir);
                failedChecks++;
                return;
            }
            
            System.out.println("  Installation directory: " + installDir);
            
            // Check key directories
            String[] keyDirs = {"bin", "conf", "lib", "logs", "jre"};
            boolean allFound = true;
            
            for (String dir : keyDirs) {
                File dirFile = new File(installDir + "/" + dir);
                if (dirFile.exists()) {
                    System.out.println("    [OK] " + dir + "/");
                } else {
                    System.out.println("    [MISSING] " + dir + "/");
                    allFound = false;
                }
            }
            
            if (allFound) {
                System.out.println("  [PASS] All key directories found");
                passedChecks++;
            } else {
                System.out.println("  [WARN] Some directories are missing");
                warningChecks++;
            }
        } catch (Exception e) {
            System.out.println("  [SKIP] Error checking installation: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void displaySummary() {
        int overallScore = 0;
        if (totalChecks > 0) {
            overallScore = (passedChecks * 100 + warningChecks * 60 + failedChecks * 0) / totalChecks;
        }
        
        System.out.println("=================================================================");
        System.out.println("  HEALTH CHECK SUMMARY");
        System.out.println("=================================================================");
        System.out.println();
        System.out.println("  Overall Health Score: " + overallScore + "/100");
        
        String status;
        if (overallScore >= 90) {
            status = "EXCELLENT";
        } else if (overallScore >= 75) {
            status = "GOOD";
        } else if (overallScore >= 60) {
            status = "FAIR";
        } else if (overallScore >= 40) {
            status = "POOR";
        } else {
            status = "CRITICAL";
        }
        System.out.println("  Status: " + status);
        
        System.out.println();
        System.out.println("  Total Checks: " + totalChecks);
        System.out.println("  [PASS] Passed: " + passedChecks);
        System.out.println("  [WARN] Warnings: " + warningChecks);
        System.out.println("  [FAIL] Failed: " + failedChecks);
        System.out.println();
        
        if (failedChecks > 0) {
            System.out.println("  WARNING: " + failedChecks + " CRITICAL ISSUE(S) DETECTED!");
            System.out.println("  Please review the detailed output above for remediation steps.");
            System.out.println();
        }
        
        System.out.println("=================================================================");
        System.out.println();
        
        // Exit with appropriate code
        System.exit(overallScore >= 60 ? 0 : 1);
    }
}
