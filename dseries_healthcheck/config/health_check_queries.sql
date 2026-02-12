-- ============================================================================
-- dSeries Health Check SQL Queries Configuration
-- ============================================================================
-- Version: 1.1.0
-- Date: 2026-02-12
--
-- This file contains all SQL queries used by the health check tool.
-- Users can add custom queries by following the format below.
--
-- Format:
-- -- @CHECK_ID: Unique identifier (e.g., DB-001)
-- -- @CHECK_NAME: Descriptive name
-- -- @CHECK_CATEGORY: Category (Database, Application, Performance, etc.)
-- -- @SEVERITY: CRITICAL, WARNING, or INFO
-- -- @DESCRIPTION: What this check does
-- -- @THRESHOLD_OPERATOR: >, <, =, >=, <=, BETWEEN, EXISTS, NOT_EXISTS
-- -- @THRESHOLD_VALUE: Expected value or range
-- -- @REMEDIATION: How to fix if check fails
-- SELECT ... your query here ...;
--
-- IMPORTANT: Do not include sensitive data (passwords, credentials, etc.)
--
-- DATABASE-SPECIFIC SYNTAX NOTES:
-- ================================
-- String Concatenation:
--   PostgreSQL, Oracle, DB2: Use || operator
--   SQL Server: Use + operator
--   Example: 'text' || column_name || 'text'
--
-- Date/Time Functions:
--   PostgreSQL: CURRENT_TIMESTAMP, INTERVAL '1 hour'
--   Oracle: SYSDATE, INTERVAL '1' HOUR
--   SQL Server: GETDATE(), DATEADD(HOUR, -1, GETDATE())
--   DB2: CURRENT TIMESTAMP, CURRENT TIMESTAMP - 1 HOUR
--
-- IMPORTANT: This file uses PostgreSQL syntax by default.
-- Modify queries if using a different database type.
-- ============================================================================

-- ============================================================================
-- SECTION 1: DATABASE TABLE SIZE CHECKS
-- ============================================================================

-- @CHECK_ID: DB-001
-- @CHECK_NAME: ESP_APPLICATION Table Size
-- @CHECK_CATEGORY: Database
-- @SEVERITY: INFO
-- @DESCRIPTION: Check the size of ESP_APPLICATION table
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 1000000
-- @REMEDIATION: Run housekeeping if table exceeds threshold
SELECT COUNT(*) as record_count FROM ESP_APPLICATION;

-- @CHECK_ID: DB-002
-- @CHECK_NAME: ESP_GENERIC_JOB Table Size
-- @CHECK_CATEGORY: Database
-- @SEVERITY: INFO
-- @DESCRIPTION: Check the size of ESP_GENERIC_JOB table
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 5000000
-- @REMEDIATION: Run housekeeping if table exceeds threshold
SELECT COUNT(*) as record_count FROM ESP_GENERIC_JOB;

-- @CHECK_ID: DB-003
-- @CHECK_NAME: ESP_WSS_APPL Table Size
-- @CHECK_CATEGORY: Database
-- @SEVERITY: INFO
-- @DESCRIPTION: Check the size of ESP_WSS_APPL active table
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 100000
-- @REMEDIATION: Run housekeeping to clean up old workstation data
SELECT COUNT(*) as record_count FROM ESP_WSS_APPL;

-- @CHECK_ID: DB-004
-- @CHECK_NAME: ESP_WSS_JOB Table Size
-- @CHECK_CATEGORY: Database
-- @SEVERITY: INFO
-- @DESCRIPTION: Check the size of ESP_WSS_JOB active table
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 500000
-- @REMEDIATION: Run housekeeping to clean up old workstation job data
SELECT COUNT(*) as record_count FROM ESP_WSS_JOB;

-- @CHECK_ID: DB-005
-- @CHECK_NAME: ESP_RTWOB Table Size
-- @CHECK_CATEGORY: Database
-- @SEVERITY: INFO
-- @DESCRIPTION: Check the size of ESP_RTWOB runtime workload table
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 1000000
-- @REMEDIATION: Run housekeeping to clean up old runtime data
SELECT COUNT(*) as record_count FROM ESP_RTWOB;

-- ============================================================================
-- SECTION 2: DATA INTEGRITY CHECKS
-- ============================================================================

-- @CHECK_ID: DI-001
-- @CHECK_NAME: Orphaned WSS Jobs
-- @CHECK_CATEGORY: Data Integrity
-- @SEVERITY: WARNING
-- @DESCRIPTION: Find WSS jobs without corresponding applications
-- @THRESHOLD_OPERATOR: =
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Run housekeeping or manually clean orphaned records
SELECT COUNT(*) as orphaned_jobs 
FROM ESP_WSS_JOB 
WHERE APPL_ID IN (
    SELECT APPL_ID 
    FROM ESP_WSS_APPL WSS 
    WHERE NOT EXISTS (
        SELECT 1 
        FROM ESP_APPLICATION APPL 
        WHERE WSS.appl_name = APPL.appl_name 
        AND WSS.appl_gen_no = APPL.appl_gen_no
    )
);

-- @CHECK_ID: DI-002
-- @CHECK_NAME: Orphaned WSS Applications
-- @CHECK_CATEGORY: Data Integrity
-- @SEVERITY: WARNING
-- @DESCRIPTION: Find WSS applications without corresponding active applications
-- @THRESHOLD_OPERATOR: =
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Run housekeeping or manually clean orphaned records
SELECT COUNT(*) as orphaned_applications 
FROM ESP_WSS_APPL 
WHERE NOT EXISTS (
    SELECT 1 
    FROM ESP_APPLICATION APPL 
    WHERE ESP_WSS_APPL.appl_name = APPL.appl_name 
    AND ESP_WSS_APPL.appl_gen_no = APPL.appl_gen_no
);

-- @CHECK_ID: DI-003
-- @CHECK_NAME: Orphaned Runtime Workload Objects
-- @CHECK_CATEGORY: Data Integrity
-- @SEVERITY: WARNING
-- @DESCRIPTION: Find runtime workload objects without corresponding applications
-- @THRESHOLD_OPERATOR: =
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Run housekeeping or manually clean orphaned runtime data
SELECT COUNT(*) as orphaned_rtwob 
FROM ESP_RTWOB 
WHERE NOT EXISTS (
    SELECT 1 
    FROM ESP_APPLICATION APPL 
    WHERE ESP_RTWOB.APPLID LIKE '%' || APPL.appl_name || '%' 
    AND ESP_RTWOB.GENERATION = APPL.appl_gen_no
);

-- ============================================================================
-- SECTION 3: AGENT CONNECTIVITY CHECKS
-- ============================================================================

-- @CHECK_ID: AG-001
-- @CHECK_NAME: Total Agents Connected
-- @CHECK_CATEGORY: Agents
-- @SEVERITY: INFO
-- @DESCRIPTION: Count total number of connected agents
-- @THRESHOLD_OPERATOR: >
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Verify agent connectivity if no agents connected
SELECT COUNT(*) as total_agents FROM esp_agent_rp WHERE STATUS = 'ACTIVE';

-- @CHECK_ID: AG-002
-- @CHECK_NAME: Disconnected Agents
-- @CHECK_CATEGORY: Agents
-- @SEVERITY: WARNING
-- @DESCRIPTION: Count agents that are disconnected
-- @THRESHOLD_OPERATOR: =
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Check agent logs and network connectivity
SELECT COUNT(*) as disconnected_agents FROM esp_agent_rp WHERE STATUS != 'ACTIVE';

-- @CHECK_ID: AG-003
-- @CHECK_NAME: Agent Version Compatibility
-- @CHECK_CATEGORY: Agents
-- @SEVERITY: WARNING
-- @DESCRIPTION: Check for agents with outdated versions
-- @THRESHOLD_OPERATOR: =
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Upgrade agents to match server version
SELECT COUNT(*) as outdated_agents 
FROM esp_agent_rp 
WHERE VERSION < (SELECT VERSION FROM ESP_SERVER_INFO);

-- ============================================================================
-- SECTION 4: CLIENT CONNECTIVITY CHECKS
-- ============================================================================

-- @CHECK_ID: CL-001
-- @CHECK_NAME: Active Client Connections
-- @CHECK_CATEGORY: Clients
-- @SEVERITY: INFO
-- @DESCRIPTION: Count active client connections
-- @THRESHOLD_OPERATOR: >=
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: N/A - Informational only
SELECT COUNT(*) as active_clients FROM ESP_CLIENT_SESSION WHERE STATUS = 'ACTIVE';

-- @CHECK_ID: CL-002
-- @CHECK_NAME: Client Connection Failures
-- @CHECK_CATEGORY: Clients
-- @SEVERITY: WARNING
-- @DESCRIPTION: Count recent client connection failures
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 10
-- @REMEDIATION: Check network connectivity and client configurations
SELECT COUNT(*) as failed_connections 
FROM ESP_CONNECTION_LOG 
WHERE STATUS = 'FAILED' 
AND TIMESTAMP > CURRENT_TIMESTAMP - INTERVAL '1 hour';

-- ============================================================================
-- SECTION 5: HIGH AVAILABILITY CHECKS
-- ============================================================================

-- @CHECK_ID: HA-001
-- @CHECK_NAME: HA Mode Status
-- @CHECK_CATEGORY: High Availability
-- @SEVERITY: CRITICAL
-- @DESCRIPTION: Check if HA mode is properly configured
-- @THRESHOLD_OPERATOR: =
-- @THRESHOLD_VALUE: ACTIVE
-- @REMEDIATION: Review HA configuration and ensure all nodes are operational

-- @CHECK_ID: HA-002
-- @CHECK_NAME: HA Node Status
-- @CHECK_CATEGORY: High Availability
-- @SEVERITY: CRITICAL
-- @DESCRIPTION: Check status of all HA nodes
-- @THRESHOLD_OPERATOR: ALL_ACTIVE
-- @THRESHOLD_VALUE: true
-- @REMEDIATION: Investigate and restart failed nodes

-- @CHECK_ID: HA-003
-- @CHECK_NAME: HA Failover History
-- @CHECK_CATEGORY: High Availability
-- @SEVERITY: INFO
-- @DESCRIPTION: Check recent failover events
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 5
-- @REMEDIATION: Investigate frequent failovers

-- ============================================================================
-- SECTION 6: PERFORMANCE METRICS
-- ============================================================================

-- @CHECK_ID: PERF-001
-- @CHECK_NAME: Active Generations
-- @CHECK_CATEGORY: Performance
-- @SEVERITY: INFO
-- @DESCRIPTION: Count active application generations
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 10000
-- @REMEDIATION: Review and clean up old generations
SELECT COUNT(DISTINCT appl_gen_no) as active_generations FROM ESP_APPLICATION;

-- @CHECK_ID: PERF-002
-- @CHECK_NAME: Long Running Jobs
-- @CHECK_CATEGORY: Performance
-- @SEVERITY: WARNING
-- @DESCRIPTION: Find jobs running longer than expected
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 10
-- @REMEDIATION: Investigate and optimize long-running jobs
SELECT COUNT(*) as long_running_jobs 
FROM ESP_GENERIC_JOB 
WHERE STATUS = 'RUNNING' 
AND START_TIME < CURRENT_TIMESTAMP - INTERVAL '4 hours';

-- @CHECK_ID: PERF-003
-- @CHECK_NAME: Failed Jobs Last 24 Hours
-- @CHECK_CATEGORY: Performance
-- @SEVERITY: WARNING
-- @DESCRIPTION: Count failed jobs in last 24 hours
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 50
-- @REMEDIATION: Investigate job failures and fix root causes
SELECT COUNT(*) as failed_jobs 
FROM ESP_GENERIC_JOB 
WHERE STATUS = 'FAILED' 
AND END_TIME > CURRENT_TIMESTAMP - INTERVAL '24 hours';

-- ============================================================================
-- SECTION 10: CUSTOM QUERIES (Add your own queries below)
-- ============================================================================

-- Example custom query:
-- @CHECK_ID: CUSTOM-001
-- @CHECK_NAME: Your Custom Check
-- @CHECK_CATEGORY: Custom
-- @SEVERITY: INFO
-- @DESCRIPTION: Description of your custom check
-- @THRESHOLD_OPERATOR: >
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: What to do if check fails
-- SELECT COUNT(*) as custom_metric FROM YOUR_TABLE WHERE YOUR_CONDITION;
SELECT 
    time_slots.slot_time as timestamp,
    EXTRACT(HOUR FROM time_slots.slot_time)::int as hour,
    EXTRACT(DOW FROM time_slots.slot_time)::int as day_of_week,
    COALESCE(job_counts.jobs_run, 0) as jobs_run,
    COALESCE(job_counts.avg_runtime, 0) as avg_runtime
FROM (
    -- Generate hourly slots for the last N days
    SELECT generate_series(
        DATE_TRUNC('hour', NOW() - INTERVAL '30 days'),
        DATE_TRUNC('hour', NOW() - INTERVAL '1 hour'),
        INTERVAL '1 hour'
    ) as slot_time
) time_slots
LEFT JOIN (
    -- Aggregate jobs per hour
    SELECT 
        DATE_TRUNC('hour', all_jobs.start_date_time) as slot_time,
        COUNT(*) as jobs_run,
        AVG(CASE 
            WHEN all_jobs.end_date_time IS NOT NULL AND all_jobs.start_date_time IS NOT NULL 
            THEN EXTRACT(EPOCH FROM (all_jobs.end_date_time - all_jobs.start_date_time))/60 
            ELSE 0 
        END) as avg_runtime
    FROM (
        SELECT start_date_time, end_date_time
        FROM esp_generic_job
        WHERE start_date_time >= NOW() - INTERVAL '30 days'
          AND start_date_time IS NOT NULL
        UNION ALL
        SELECT start_date_time, end_date_time
        FROM h_generic_job
        WHERE start_date_time >= NOW() - INTERVAL '30 days'
          AND start_date_time IS NOT NULL
    ) all_jobs
    GROUP BY DATE_TRUNC('hour', all_jobs.start_date_time)
) job_counts ON time_slots.slot_time = job_counts.slot_time
ORDER BY time_slots.slot_time
-- ============================================================================
-- END OF CONFIGURATION
-- ============================================================================
-- 
-- NOTES:
-- 1. Always test queries in a development environment first
-- 2. Ensure queries do not expose sensitive data
-- 3. Use appropriate indexes for performance
-- 4. Keep queries simple and focused on single metrics
-- 5. Document all custom queries with proper metadata
-- 
-- For more information, see:
-- https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/ca-workload-automation-de/12-3/maintaining.html
-- ============================================================================
