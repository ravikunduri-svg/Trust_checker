-- ============================================================================
-- dSeries Health Check SQL Queries Configuration
-- ============================================================================
-- Version: 1.2.0
-- Date: 2026-02-13
-- 
-- VERIFIED AGAINST ACTUAL dSeries DATABASE SCHEMA
-- All table and column names have been validated
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
SELECT COUNT(*) record_count FROM ESP_APPLICATION;

-- @CHECK_ID: DB-002
-- @CHECK_NAME: ESP_GENERIC_JOB Table Size
-- @CHECK_CATEGORY: Database
-- @SEVERITY: INFO
-- @DESCRIPTION: Check the size of ESP_GENERIC_JOB table
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 5000000
-- @REMEDIATION: Run housekeeping if table exceeds threshold
SELECT COUNT(*) record_count FROM ESP_GENERIC_JOB;

-- @CHECK_ID: DB-003
-- @CHECK_NAME: ESP_WSS_APPL Table Size
-- @CHECK_CATEGORY: Database
-- @SEVERITY: INFO
-- @DESCRIPTION: Check the size of ESP_WSS_APPL active table
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 100000
-- @REMEDIATION: Run housekeeping to clean up old workstation data
SELECT COUNT(*) record_count FROM ESP_WSS_APPL;

-- @CHECK_ID: DB-004
-- @CHECK_NAME: ESP_WSS_JOB Table Size
-- @CHECK_CATEGORY: Database
-- @SEVERITY: INFO
-- @DESCRIPTION: Check the size of ESP_WSS_JOB active table
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 500000
-- @REMEDIATION: Run housekeeping to clean up old workstation job data
SELECT COUNT(*) record_count FROM ESP_WSS_JOB;

-- @CHECK_ID: DB-005
-- @CHECK_NAME: ESP_RTWOB Table Size
-- @CHECK_CATEGORY: Database
-- @SEVERITY: INFO
-- @DESCRIPTION: Check the size of ESP_RTWOB runtime workload table
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 1000000
-- @REMEDIATION: Run housekeeping to clean up old runtime data
SELECT COUNT(*) record_count FROM ESP_RTWOB;

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
SELECT COUNT(*) orphaned_jobs 
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
SELECT COUNT(*) orphaned_applications 
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
SELECT COUNT(*) orphaned_rtwob 
FROM ESP_RTWOB 
WHERE NOT EXISTS (
    SELECT 1 
    FROM ESP_APPLICATION APPL 
    WHERE ESP_RTWOB.APPLID LIKE '%' || APPL.appl_name || '%' 
    AND ESP_RTWOB.GENERATION = APPL.appl_gen_no
);

-- ============================================================================
-- SECTION 3: AGENT HEALTH CHECKS
-- ============================================================================

-- @CHECK_ID: AGENT-001
-- @CHECK_NAME: Total Agent Count
-- @CHECK_CATEGORY: Agent
-- @SEVERITY: INFO
-- @DESCRIPTION: Count total number of registered agents
-- @THRESHOLD_OPERATOR: >
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Ensure agents are properly registered
SELECT COUNT(*) total_agents FROM ESP_AGENT_RP;

-- @CHECK_ID: AGENT-002
-- @CHECK_NAME: Agents with Pending Actions
-- @CHECK_CATEGORY: Agent
-- @SEVERITY: WARNING
-- @DESCRIPTION: Count agents with pending actions (ACTIONSTATUS = 1)
-- @THRESHOLD_OPERATOR: =
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review pending agent actions and complete them
SELECT COUNT(*) pending_actions 
FROM ESP_AGENT_RP 
WHERE ACTIONSTATUS = 1;

-- @CHECK_ID: AGENT-003
-- @CHECK_NAME: Agents with Failed Actions
-- @CHECK_CATEGORY: Agent
-- @SEVERITY: WARNING
-- @DESCRIPTION: Count agents with failed recent actions
-- @THRESHOLD_OPERATOR: =
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review agent action logs and resolve failures
SELECT COUNT(*) failed_actions 
FROM ESP_AGENT_RP 
WHERE ACTIONSTATUS = 2;

-- ============================================================================
-- SECTION 4: EVENT AND TRIGGER CHECKS
-- ============================================================================

-- @CHECK_ID: EVENT-001
-- @CHECK_NAME: Total Events
-- @CHECK_CATEGORY: Event
-- @SEVERITY: INFO
-- @DESCRIPTION: Count total events defined in the system
-- @THRESHOLD_OPERATOR: >=
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review event definitions
SELECT COUNT(*) total_events 
FROM ESP_EVENT_RP;

-- @CHECK_ID: EVENT-002
-- @CHECK_NAME: Events with Hold Status
-- @CHECK_CATEGORY: Event
-- @SEVERITY: WARNING
-- @DESCRIPTION: Count events that are on hold
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 10
-- @REMEDIATION: Review and release held events
SELECT COUNT(*) held_events 
FROM ESP_EVENT_RP 
WHERE HOLD_COUNT > 0;

-- ============================================================================
-- SECTION 5: HIGH AVAILABILITY CHECKS
-- ============================================================================

-- @CHECK_ID: HA-001
-- @CHECK_NAME: HA Configuration Status
-- @CHECK_CATEGORY: High Availability
-- @SEVERITY: INFO
-- @DESCRIPTION: Check if HA is configured
-- @THRESHOLD_OPERATOR: >=
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review HA configuration if needed
SELECT COUNT(*) ha_config_count 
FROM ESP_CONFIG_GROUP 
WHERE TYPE = 'CONFIG_INSTANCE';

-- @CHECK_ID: HA-002
-- @CHECK_NAME: HA Stage2 Nodes
-- @CHECK_CATEGORY: High Availability
-- @SEVERITY: INFO
-- @DESCRIPTION: Count HA Stage2 nodes
-- @THRESHOLD_OPERATOR: >=
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Ensure all HA nodes are properly configured
SELECT COUNT(*) ha_nodes 
FROM ESP_STAGE2HAC;

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
SELECT COUNT(DISTINCT appl_gen_no) active_generations FROM ESP_APPLICATION;

-- @CHECK_ID: PERF-002
-- @CHECK_NAME: Long Running Jobs
-- @CHECK_CATEGORY: Performance
-- @SEVERITY: WARNING
-- @DESCRIPTION: Find jobs running longer than expected
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 10
-- @REMEDIATION: Investigate and optimize long-running jobs
SELECT COUNT(*) long_running_jobs 
FROM ESP_GENERIC_JOB 
WHERE STATUS = 'EXEC' 
AND START_DATE_TIME < CURRENT_TIMESTAMP - INTERVAL '4 hours';

-- @CHECK_ID: PERF-003
-- @CHECK_NAME: Failed Jobs Last 24 Hours
-- @CHECK_CATEGORY: Performance
-- @SEVERITY: WARNING
-- @DESCRIPTION: Count failed jobs in last 24 hours
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 50
-- @REMEDIATION: Investigate job failures and fix root causes
SELECT COUNT(*) failed_jobs 
FROM ESP_GENERIC_JOB 
WHERE STATUS = 'FAILED' 
AND END_DATE_TIME > CURRENT_TIMESTAMP - INTERVAL '24 hours';

-- @CHECK_ID: PERF-004
-- @CHECK_NAME: Jobs in EXEC Status
-- @CHECK_CATEGORY: Performance
-- @SEVERITY: INFO
-- @DESCRIPTION: Count currently executing jobs
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 1000
-- @REMEDIATION: Monitor for job bottlenecks
SELECT COUNT(*) executing_jobs 
FROM ESP_GENERIC_JOB 
WHERE STATUS = 'EXEC';

-- @CHECK_ID: PERF-005
-- @CHECK_NAME: Jobs Waiting to Run
-- @CHECK_CATEGORY: Performance
-- @SEVERITY: INFO
-- @DESCRIPTION: Count jobs waiting to execute
-- @THRESHOLD_OPERATOR: <
-- @THRESHOLD_VALUE: 500
-- @REMEDIATION: Check agent availability and resource constraints
SELECT COUNT(*) waiting_jobs 
FROM ESP_GENERIC_JOB 
WHERE STATUS = 'WAITING';

-- ============================================================================
-- SECTION 7: ALERT AND CONNECTION PROFILE CHECKS
-- ============================================================================

-- @CHECK_ID: ALERT-001
-- @CHECK_NAME: Total Alerts
-- @CHECK_CATEGORY: Alert
-- @SEVERITY: INFO
-- @DESCRIPTION: Count total alert definitions in the system
-- @THRESHOLD_OPERATOR: >=
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review alert definitions
SELECT COUNT(*) total_alerts 
FROM ESP_ALERT_RP;

-- @CHECK_ID: CONN-001
-- @CHECK_NAME: Connection Profiles
-- @CHECK_CATEGORY: Connection
-- @SEVERITY: INFO
-- @DESCRIPTION: Count connection profiles in the system
-- @THRESHOLD_OPERATOR: >=
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review connection profile definitions
SELECT COUNT(*) total_conn_profiles 
FROM ESP_CONN_PROFILE_RP;

-- ============================================================================
-- SECTION 8: RESOURCE MANAGER CHECKS
-- ============================================================================

-- @CHECK_ID: RM-001
-- @CHECK_NAME: Total Resources
-- @CHECK_CATEGORY: Resource
-- @SEVERITY: INFO
-- @DESCRIPTION: Count total resource definitions
-- @THRESHOLD_OPERATOR: >=
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review resource definitions
SELECT COUNT(*) total_resources FROM ESP_RESOURCE_RP;

-- @CHECK_ID: RM-002
-- @CHECK_NAME: Resources with Low Availability
-- @CHECK_CATEGORY: Resource
-- @SEVERITY: WARNING
-- @DESCRIPTION: Find resources with availability below 10%
-- @THRESHOLD_OPERATOR: =
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review and increase resource availability
SELECT COUNT(*) low_availability_resources 
FROM ESP_RESOURCE_RP 
WHERE MAX_AVAILABILITY > 0 
AND (AVAILABILITY * 100 / MAX_AVAILABILITY) < 10;

-- ============================================================================
-- SECTION 9: SECURITY AND USER CHECKS
-- ============================================================================

-- @CHECK_ID: SEC-001
-- @CHECK_NAME: Total Users
-- @CHECK_CATEGORY: Security
-- @SEVERITY: INFO
-- @DESCRIPTION: Count total users in the system
-- @THRESHOLD_OPERATOR: >
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review user accounts
SELECT COUNT(*) total_users FROM ESP_USER;

-- @CHECK_ID: SEC-002
-- @CHECK_NAME: User Groups
-- @CHECK_CATEGORY: Security
-- @SEVERITY: INFO
-- @DESCRIPTION: Count user groups in the system
-- @THRESHOLD_OPERATOR: >=
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: Review user group definitions
SELECT COUNT(*) total_usergroups 
FROM ESP_USERGROUP;

-- ============================================================================
-- SECTION 10: CUSTOM QUERIES (Add your own queries below)
-- ============================================================================

-- Example custom query:
-- @CHECK_ID: CUSTOM-001
-- @CHECK_NAME: dseries heat map based on the schedules
-- @CHECK_CATEGORY: Custom
-- @SEVERITY: INFO
-- @DESCRIPTION: dseries heat map based on the schedules (PostgreSQL only - disabled for Oracle)
-- @THRESHOLD_OPERATOR: >
-- @THRESHOLD_VALUE: 0
-- @REMEDIATION: What to do if check fails
-- NOTE: This query uses PostgreSQL-specific functions (generate_series, EXTRACT DOW, ::int casting)
-- For Oracle, this query needs to be rewritten using Oracle-specific syntax
-- Commented out for Oracle compatibility
-- SELECT COUNT(*) custom_metric FROM YOUR_TABLE WHERE YOUR_CONDITION;
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
