package com.broadcom.dseries.healthcheck;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Generates JSON reports for health check results
 * 
 * Creates machine-readable JSON reports suitable for:
 * - API integration
 * - Monitoring tools
 * - Automated processing
 * - Trend analysis
 * 
 * @author Broadcom
 * @version 1.0.0
 */
public class JsonReportGenerator {
    
    private static final Logger LOGGER = Logger.getLogger(JsonReportGenerator.class.getName());
    
    /**
     * Generate JSON report
     * 
     * @param healthCheck The health check instance with results
     * @param outputFile Path to output JSON file
     * @throws IOException If file cannot be written
     */
    public void generateReport(DSeriesHealthCheck healthCheck, String outputFile) throws IOException {
        LOGGER.info("Generating JSON report: " + outputFile);
        
        StringBuilder json = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        
        json.append("{\n");
        
        // Metadata section
        json.append("  \"metadata\": {\n");
        json.append("    \"version\": \"1.0.0\",\n");
        json.append("    \"timestamp\": \"").append(sdf.format(healthCheck.getStartTime())).append("\",\n");
        json.append("    \"hostname\": \"").append(escapeJson(healthCheck.getHostname())).append("\",\n");
        
        Date now = new Date();
        long durationMs = now.getTime() - healthCheck.getStartTime().getTime();
        double durationSec = durationMs / 1000.0;
        json.append("    \"duration_seconds\": ").append(String.format("%.1f", durationSec)).append("\n");
        json.append("  },\n");
        
        // Summary section
        int overallScore = healthCheck.calculateOverallScore();
        json.append("  \"summary\": {\n");
        json.append("    \"overall_score\": ").append(overallScore).append(",\n");
        json.append("    \"total_checks\": ").append(healthCheck.getTotalChecks()).append(",\n");
        json.append("    \"passed\": ").append(healthCheck.getPassedChecks()).append(",\n");
        json.append("    \"warnings\": ").append(healthCheck.getWarningChecks()).append(",\n");
        json.append("    \"failed\": ").append(healthCheck.getFailedChecks()).append("\n");
        json.append("  },\n");
        
        // Results section
        json.append("  \"results\": [\n");
        
        List<HealthCheckResult> results = healthCheck.getResults();
        for (int i = 0; i < results.size(); i++) {
            HealthCheckResult result = results.get(i);
            
            json.append("    {\n");
            json.append("      \"check_id\": \"").append(escapeJson(result.getCheckId())).append("\",\n");
            json.append("      \"category\": \"").append(escapeJson(result.getCategory())).append("\",\n");
            json.append("      \"description\": \"").append(escapeJson(result.getDescription())).append("\",\n");
            json.append("      \"status\": \"").append(result.getStatus()).append("\",\n");
            json.append("      \"severity\": \"").append(result.getSeverity()).append("\",\n");
            json.append("      \"message\": \"").append(escapeJson(result.getMessage())).append("\",\n");
            json.append("      \"value\": \"").append(escapeJson(result.getValue())).append("\",\n");
            json.append("      \"threshold\": \"").append(escapeJson(result.getThreshold())).append("\",\n");
            json.append("      \"recommendation\": \"").append(escapeJson(result.getRecommendation())).append("\",\n");
            json.append("      \"timestamp\": \"").append(sdf.format(result.getTimestamp())).append("\"\n");
            json.append("    }");
            
            if (i < results.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}\n");
        
        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(json.toString());
        }
        
        LOGGER.info("JSON report generated successfully");
    }
    
    /**
     * Escape JSON special characters
     */
    private String escapeJson(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
