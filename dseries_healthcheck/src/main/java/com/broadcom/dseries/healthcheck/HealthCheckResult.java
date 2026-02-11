package com.broadcom.dseries.healthcheck;

import java.util.Date;

/**
 * Represents the result of a single health check
 * 
 * This class encapsulates all information about a health check result,
 * including the status, severity, messages, and recommendations.
 * 
 * @author Broadcom
 * @version 1.0.0
 */
public class HealthCheckResult {
    
    // Check identification
    private String checkId;
    private String category;
    private String description;
    
    // Check result
    private CheckStatus status;
    private Severity severity;
    private String message;
    
    // Additional information
    private String value;
    private String threshold;
    private String recommendation;
    private Date timestamp;
    
    /**
     * Constructor with all fields
     * 
     * @param checkId Unique identifier for the check (e.g., "SYS-001")
     * @param category Category of the check (e.g., "System Resources")
     * @param description Short description of what is being checked
     * @param status Result status (PASS, WARNING, FAIL, SKIP, INFO)
     * @param severity Severity level (CRITICAL, HIGH, MEDIUM, LOW, INFO)
     * @param message Detailed message about the result
     * @param value Actual value measured
     * @param threshold Threshold values used for comparison
     * @param recommendation Recommendation for remediation
     */
    public HealthCheckResult(String checkId, String category, String description,
                            CheckStatus status, Severity severity, String message,
                            String value, String threshold, String recommendation) {
        this.checkId = checkId;
        this.category = category;
        this.description = description;
        this.status = status;
        this.severity = severity;
        this.message = message;
        this.value = value;
        this.threshold = threshold;
        this.recommendation = recommendation;
        this.timestamp = new Date();
    }
    
    // Getters
    
    /**
     * Get the check ID
     * @return Check ID (e.g., "SYS-001")
     */
    public String getCheckId() {
        return checkId;
    }
    
    /**
     * Get the category
     * @return Category (e.g., "System Resources")
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Get the description
     * @return Description of what is being checked
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the status
     * @return Status (PASS, WARNING, FAIL, SKIP, INFO)
     */
    public CheckStatus getStatus() {
        return status;
    }
    
    /**
     * Get the severity
     * @return Severity (CRITICAL, HIGH, MEDIUM, LOW, INFO)
     */
    public Severity getSeverity() {
        return severity;
    }
    
    /**
     * Get the message
     * @return Detailed message about the result
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get the value
     * @return Actual value measured
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Get the threshold
     * @return Threshold values used
     */
    public String getThreshold() {
        return threshold;
    }
    
    /**
     * Get the recommendation
     * @return Recommendation for remediation
     */
    public String getRecommendation() {
        return recommendation;
    }
    
    /**
     * Get the timestamp
     * @return Timestamp when the check was performed
     */
    public Date getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get status icon for display
     * @return Unicode icon representing the status
     */
    public String getStatusIcon() {
        switch (status) {
            case PASS:
                return "✅";
            case WARNING:
                return "⚠️";
            case FAIL:
                return "❌";
            case SKIP:
                return "⏭️";
            case INFO:
                return "ℹ️";
            default:
                return "❓";
        }
    }
    
    /**
     * Get status CSS class for HTML reports
     * @return CSS class name
     */
    public String getStatusCssClass() {
        return "status-" + status.name().toLowerCase();
    }
    
    /**
     * Get severity CSS class for HTML reports
     * @return CSS class name
     */
    public String getSeverityCssClass() {
        return "severity-" + severity.name().toLowerCase();
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s - %s (Value: %s, Threshold: %s)",
            checkId, status, description, message, value, threshold);
    }
}
