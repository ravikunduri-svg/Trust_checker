package com.broadcom.dseries.healthcheck;

/**
 * Enumeration of health check status values
 * 
 * Represents the outcome of a health check.
 * 
 * @author Broadcom
 * @version 1.0.0
 */
public enum CheckStatus {
    /**
     * Check passed successfully - no issues detected
     */
    PASS,
    
    /**
     * Check passed with warnings - minor issues detected
     */
    WARNING,
    
    /**
     * Check failed - critical issues detected
     */
    FAIL,
    
    /**
     * Check was skipped - unable to perform check
     */
    SKIP,
    
    /**
     * Informational check - provides information only
     */
    INFO
}
