package com.broadcom.dseries.healthcheck;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Generates HTML reports for health check results
 * 
 * Creates a comprehensive, interactive HTML report with:
 * - Executive summary with health score
 * - Visual indicators and status icons
 * - Detailed check results by category
 * - Recommendations for remediation
 * - Mobile-responsive design
 * 
 * @author Broadcom
 * @version 1.0.0
 */
public class HtmlReportGenerator {
    
    private static final Logger LOGGER = Logger.getLogger(HtmlReportGenerator.class.getName());
    
    /**
     * Generate HTML report
     * 
     * @param healthCheck The health check instance with results
     * @param outputFile Path to output HTML file
     * @throws IOException If file cannot be written
     */
    public void generateReport(DSeriesHealthCheck healthCheck, String outputFile) throws IOException {
        LOGGER.info("Generating HTML report: " + outputFile);
        
        StringBuilder html = new StringBuilder();
        
        // Build HTML content
        html.append(buildHtmlHeader(healthCheck));
        html.append(buildSummarySection(healthCheck));
        html.append(buildCriticalIssuesSection(healthCheck));
        html.append(buildDetailedResultsSection(healthCheck));
        html.append(buildHtmlFooter(healthCheck));
        
        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(html.toString());
        }
        
        LOGGER.info("HTML report generated successfully");
    }
    
    /**
     * Build HTML header with CSS styles
     */
    private String buildHtmlHeader(DSeriesHealthCheck healthCheck) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <title>dSeries Health Check Report - " + sdf.format(healthCheck.getStartTime()) + "</title>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <style>\n" +
            "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 20px; background: #f5f5f5; }\n" +
            "        .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n" +
            "        h1 { color: #333; border-bottom: 3px solid #0066cc; padding-bottom: 10px; }\n" +
            "        h2 { color: #0066cc; margin-top: 30px; }\n" +
            "        .header { background: #0066cc; color: white; padding: 20px; margin: -30px -30px 30px -30px; }\n" +
            "        .header h1 { color: white; border: none; margin: 0; }\n" +
            "        .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }\n" +
            "        .summary-card { background: #f9f9f9; padding: 20px; border-radius: 8px; border-left: 4px solid #0066cc; }\n" +
            "        .summary-card h3 { margin: 0 0 10px 0; color: #666; font-size: 14px; }\n" +
            "        .summary-card .value { font-size: 32px; font-weight: bold; color: #333; }\n" +
            "        .score-excellent { color: #28a745; }\n" +
            "        .score-good { color: #5cb85c; }\n" +
            "        .score-fair { color: #ffc107; }\n" +
            "        .score-poor { color: #ff9800; }\n" +
            "        .score-critical { color: #dc3545; }\n" +
            "        .check-table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n" +
            "        .check-table th { background: #0066cc; color: white; padding: 12px; text-align: left; }\n" +
            "        .check-table td { padding: 10px; border-bottom: 1px solid #ddd; }\n" +
            "        .check-table tr:hover { background: #f5f5f5; }\n" +
            "        .status-pass { color: #28a745; font-weight: bold; }\n" +
            "        .status-warning { color: #ffc107; font-weight: bold; }\n" +
            "        .status-fail { color: #dc3545; font-weight: bold; }\n" +
            "        .status-skip { color: #6c757d; font-weight: bold; }\n" +
            "        .status-info { color: #17a2b8; font-weight: bold; }\n" +
            "        .recommendation { background: #fff3cd; padding: 10px; border-left: 4px solid #ffc107; margin: 10px 0; }\n" +
            "        .critical-issues { background: #f8d7da; padding: 15px; border-left: 4px solid #dc3545; margin: 20px 0; }\n" +
            "        .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; font-size: 12px; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>ESP dSeries Health Check Report</h1>\n" +
            "            <p>Generated: " + sdf.format(healthCheck.getStartTime()) + " | Host: " + healthCheck.getHostname() + "</p>\n" +
            "        </div>\n";
    }
    
    /**
     * Build summary section with health score and statistics
     */
    private String buildSummarySection(DSeriesHealthCheck healthCheck) {
        int overallScore = healthCheck.calculateOverallScore();
        
        String statusClass;
        String statusText;
        String statusIcon;
        
        if (overallScore >= 90) {
            statusClass = "excellent";
            statusText = "EXCELLENT";
            statusIcon = "✅";
        } else if (overallScore >= 75) {
            statusClass = "good";
            statusText = "GOOD";
            statusIcon = "🟢";
        } else if (overallScore >= 60) {
            statusClass = "fair";
            statusText = "FAIR";
            statusIcon = "🟡";
        } else if (overallScore >= 40) {
            statusClass = "poor";
            statusText = "POOR";
            statusIcon = "🟠";
        } else {
            statusClass = "critical";
            statusText = "CRITICAL";
            statusIcon = "🔴";
        }
        
        return "        <div class=\"summary\">\n" +
            "            <div class=\"summary-card\">\n" +
            "                <h3>Overall Health Score</h3>\n" +
            "                <div class=\"value score-" + statusClass + "\">" + overallScore + "/100</div>\n" +
            "                <p>" + statusIcon + " " + statusText + "</p>\n" +
            "            </div>\n" +
            "            <div class=\"summary-card\">\n" +
            "                <h3>Total Checks</h3>\n" +
            "                <div class=\"value\">" + healthCheck.getTotalChecks() + "</div>\n" +
            "            </div>\n" +
            "            <div class=\"summary-card\">\n" +
            "                <h3>✅ Passed</h3>\n" +
            "                <div class=\"value\" style=\"color: #28a745;\">" + healthCheck.getPassedChecks() + "</div>\n" +
            "            </div>\n" +
            "            <div class=\"summary-card\">\n" +
            "                <h3>⚠️ Warnings</h3>\n" +
            "                <div class=\"value\" style=\"color: #ffc107;\">" + healthCheck.getWarningChecks() + "</div>\n" +
            "            </div>\n" +
            "            <div class=\"summary-card\">\n" +
            "                <h3>❌ Failed</h3>\n" +
            "                <div class=\"value\" style=\"color: #dc3545;\">" + healthCheck.getFailedChecks() + "</div>\n" +
            "            </div>\n" +
            "        </div>\n";
    }
    
    /**
     * Build critical issues section
     */
    private String buildCriticalIssuesSection(DSeriesHealthCheck healthCheck) {
        List<HealthCheckResult> criticalResults = new ArrayList<>();
        for (HealthCheckResult result : healthCheck.getResults()) {
            if (result.getStatus() == CheckStatus.FAIL) {
                criticalResults.add(result);
            }
        }
        
        if (criticalResults.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("        <div class=\"critical-issues\">\n");
        html.append("            <h3>🔴 Critical Issues Requiring Immediate Attention</h3>\n");
        html.append("            <ul>\n");
        
        for (HealthCheckResult result : criticalResults) {
            html.append("                <li><strong>[")
                .append(result.getCheckId())
                .append("]</strong> ")
                .append(result.getDescription())
                .append(": ")
                .append(result.getMessage())
                .append("</li>\n");
        }
        
        html.append("            </ul>\n");
        html.append("        </div>\n");
        
        return html.toString();
    }
    
    /**
     * Build detailed results section grouped by category
     */
    private String buildDetailedResultsSection(DSeriesHealthCheck healthCheck) {
        StringBuilder html = new StringBuilder();
        
        // Group results by category
        Map<String, List<HealthCheckResult>> categorizedResults = new LinkedHashMap<>();
        for (HealthCheckResult result : healthCheck.getResults()) {
            String category = result.getCategory();
            if (!categorizedResults.containsKey(category)) {
                categorizedResults.put(category, new ArrayList<>());
            }
            categorizedResults.get(category).add(result);
        }
        
        // Build HTML for each category
        for (Map.Entry<String, List<HealthCheckResult>> entry : categorizedResults.entrySet()) {
            String category = entry.getKey();
            List<HealthCheckResult> results = entry.getValue();
            
            html.append("        <h2>").append(category).append("</h2>\n");
            html.append("        <table class=\"check-table\">\n");
            html.append("            <tr>\n");
            html.append("                <th>Check ID</th>\n");
            html.append("                <th>Description</th>\n");
            html.append("                <th>Status</th>\n");
            html.append("                <th>Value</th>\n");
            html.append("                <th>Message</th>\n");
            html.append("            </tr>\n");
            
            for (HealthCheckResult result : results) {
                html.append("            <tr>\n");
                html.append("                <td>").append(result.getCheckId()).append("</td>\n");
                html.append("                <td>").append(result.getDescription()).append("</td>\n");
                html.append("                <td class=\"").append(result.getStatusCssClass()).append("\">")
                    .append(result.getStatusIcon()).append(" ").append(result.getStatus()).append("</td>\n");
                html.append("                <td>").append(escapeHtml(result.getValue())).append("</td>\n");
                html.append("                <td>").append(escapeHtml(result.getMessage())).append("</td>\n");
                html.append("            </tr>\n");
                
                // Add recommendation row if present
                if (result.getRecommendation() != null && !result.getRecommendation().isEmpty()) {
                    html.append("            <tr>\n");
                    html.append("                <td colspan=\"5\">\n");
                    html.append("                    <div class=\"recommendation\">\n");
                    html.append("                        <strong>Recommendation:</strong> ")
                        .append(escapeHtml(result.getRecommendation())).append("\n");
                    html.append("                    </div>\n");
                    html.append("                </td>\n");
                    html.append("            </tr>\n");
                }
            }
            
            html.append("        </table>\n");
        }
        
        return html.toString();
    }
    
    /**
     * Build HTML footer
     */
    private String buildHtmlFooter(DSeriesHealthCheck healthCheck) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        long durationMs = now.getTime() - healthCheck.getStartTime().getTime();
        double durationSec = durationMs / 1000.0;
        
        return "        <div class=\"footer\">\n" +
            "            <p><strong>ESP dSeries Health Check Tool v1.0.0</strong></p>\n" +
            "            <p>Report generated: " + sdf.format(now) + "</p>\n" +
            "            <p>Duration: " + String.format("%.1f", durationSec) + " seconds</p>\n" +
            "            <p>Copyright © 2026 Broadcom. All Rights Reserved.</p>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>\n";
    }
    
    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
