package com.broadcom.dseries.healthcheck;

/**
 * Enumeration of severity levels for health check issues
 * 
 * Indicates the importance and urgency of addressing an issue.
 * 
 * @author Broadcom
 * @version 1.0.0
 */
public enum Severity {
    /**
     * Critical severity - requires immediate action
     * System is at risk or not functioning properly
     */
    CRITICAL,
    
    /**
     * High severity - requires urgent action
     * Significant problems that need attention within hours
     */
    HIGH,
    
    /**
     * Medium severity - requires attention
     * Issues that should be addressed within days
     */
    MEDIUM,
    
    /**
     * Low severity - low priority
     * Minor issues that can be addressed during maintenance
     */
    LOW,
    
    /**
     * Informational - no action required
     * Provides information only
     */
    INFO
}
