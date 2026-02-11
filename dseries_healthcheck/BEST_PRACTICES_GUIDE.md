# ESP dSeries Workload Automation
## Best Practices & Health Check Guide

**Version:** 1.0.0  
**Date:** February 9, 2026  
**Based on:** Industry standards and dSeries best practices

---

## 📋 Table of Contents

1. [System Resource Best Practices](#system-resource-best-practices)
2. [Database Configuration](#database-configuration)
3. [Server Configuration](#server-configuration)
4. [Agent Management](#agent-management)
5. [Workload Optimization](#workload-optimization)
6. [Security Hardening](#security-hardening)
7. [High Availability](#high-availability)
8. [Maintenance & Housekeeping](#maintenance--housekeeping)
9. [Monitoring & Alerting](#monitoring--alerting)
10. [Troubleshooting Guide](#troubleshooting-guide)

---

## 🖥️ System Resource Best Practices

### **CPU Configuration**

Based on industry and dSeries best practices:

```
Minimum Requirements:
  Small Environment (0-15,000 daily jobs):  4 CPU cores
  Medium Environment (15,001-75,000 jobs): 8 CPU cores
  Large Environment (75,001-150,000 jobs): 16+ CPU cores

Best Practices:
  ✅ Dedicate CPU cores to dSeries server
  ✅ Enable CPU affinity for Java process
  ✅ Monitor CPU utilization: <70% normal, >85% critical
  ✅ Use multi-core processors for parallel processing
```

**Health Check Validation:**
- CPU utilization should be <70% during normal operations
- CPU load average should be <number of cores
- No sustained CPU spikes >85%

---

### **Memory Configuration**

Based on dSeries deployment architecture:

```
JVM Heap Sizing (Critical):
  Default:      1 GB (NOT recommended for production)
  Production:   4 GB minimum (4096 MB)
  High Load:    8 GB or more
  
System Memory:
  Small:   8 GB total (4GB heap + 4GB OS/other)
  Medium:  16 GB total (8GB heap + 8GB OS/other)
  Large:   32 GB+ total (16GB+ heap + 16GB OS/other)

Configuration Files:
  Unix/Linux: Edit startServer script
    -Xms4096m -Xmx4096m
  
  Windows: Edit windows.service.properties
    wrapper.java.maxmemory=4096
```

**Health Check Validation:**
- JVM heap ≥ 4GB for production
- System memory usage <80% normal, >90% critical
- No OutOfMemoryErrors in logs
- GC frequency <10 per minute

---

### **Disk Space Management**

```
Minimum Requirements:
  Installation:     5 GB
  Database:        20 GB minimum (grows with workload)
  Logs:            10 GB (with rotation)
  Archives:        50 GB+ (depends on retention)
  
Recommended:
  Total:          100 GB+ for production
  
Partitioning:
  /opt/CA/WA_DE           (10 GB) - Installation
  /var/log/dseries        (20 GB) - Logs
  /var/lib/dseries/db     (50 GB+) - Database
  /var/lib/dseries/archive (50 GB+) - Archives
```

**Health Check Validation:**
- Disk usage <75% normal, >85% critical
- Separate partitions for database and logs
- Disk I/O wait time <20%
- Sufficient inodes available (>20%)

---

## 🗄️ Database Configuration

### **Database Sizing**

Based on industry and dSeries best practices:

```
PostgreSQL (Recommended):
  Small:   Standard PostgreSQL 12+
  Medium:  PostgreSQL with tuning
  Large:   PostgreSQL with replication
  
Oracle:
  Small:   Oracle 12c+
  Medium:  Oracle RAC (2 nodes)
  Large:   Oracle RAC (3+ nodes)
  
SQL Server:
  Small:   SQL Server 2016+
  Medium:  SQL Server with Always On
  Large:   SQL Server clustered
```

### **Connection Pool Configuration**

```
Recommended Settings:
  Min Connections:  10
  Max Connections:  100 (adjust based on workload)
  Connection Timeout: 30 seconds
  Idle Timeout:     300 seconds
  
Validation:
  Test on Borrow: true
  Validation Query: SELECT 1
  Validation Interval: 30 seconds
```

**Health Check Validation:**
- Connection pool has ≥5 available connections
- No connection timeout errors
- Query response time <500ms
- No deadlocks or blocking queries

---

### **Database Performance Tuning**

Based on general database best practices:

```
PostgreSQL:
  shared_buffers = 25% of RAM
  effective_cache_size = 50% of RAM
  work_mem = 50MB (adjust per workload)
  maintenance_work_mem = 1GB
  max_connections = 200
  
Oracle:
  SGA_TARGET = 60% of RAM
  PGA_AGGREGATE_TARGET = 20% of RAM
  PROCESSES = 300
  SESSIONS = 335
  
SQL Server:
  Max Server Memory = 80% of RAM
  Min Server Memory = 50% of RAM
  Max Degree of Parallelism = Number of cores
```

**Health Check Validation:**
- Database statistics up to date
- Indexes not fragmented (>30%)
- No missing indexes on key tables
- Backup completed within 24 hours

---

## 🖥️ Server Configuration

### **JVM Configuration (CRITICAL)**

Based on dSeries best practices:

```
Production Configuration:
  -Xms4096m                    # Initial heap
  -Xmx4096m                    # Maximum heap (same as initial)
  -XX:+UseG1GC                 # Use G1 garbage collector
  -XX:MaxGCPauseMillis=200     # Target GC pause time
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/var/log/dseries/heapdumps
  
High Load Configuration:
  -Xms8192m
  -Xmx8192m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:G1HeapRegionSize=16m
```

**Why Same Min/Max Heap?**
- Pre-allocates memory (dSeries uses pre-allocated memory model)
- Prevents heap resizing overhead
- More predictable performance

**Health Check Validation:**
- Heap size ≥ 4GB
- GC frequency <10 per minute
- GC pause time <500ms
- No OutOfMemoryErrors

---

### **Thread Pool Configuration**

Based on industry best practices, adapted for dSeries:

```
Small Environment (0-15,000 daily jobs):
  DOWNLOAD_THREADS=3
  DB_UPDATE_THREADS=2
  SELECTOR_THREADS=4
  POST_PROCESS_THREADS=4
  TRACKER_WORKERS=4
  
Medium Environment (15,001-75,000 jobs):
  DOWNLOAD_THREADS=6
  DB_UPDATE_THREADS=4
  SELECTOR_THREADS=8
  POST_PROCESS_THREADS=8
  TRACKER_WORKERS=8
  
Large Environment (75,001-150,000 jobs):
  DOWNLOAD_THREADS=8
  DB_UPDATE_THREADS=8
  SELECTOR_THREADS=12
  POST_PROCESS_THREADS=12
  TRACKER_WORKERS=12
```

**Health Check Validation:**
- Thread pools sized appropriately for workload
- No thread pool exhaustion
- Thread count <1000 (indicates healthy state)

---

## 🤖 Agent Management

### **Agent Health Verification**

Based on industry best practices:

```
Agent Connectivity Check:
  Method: Ping agent from server
  Frequency: Every 5 minutes
  Timeout: 10 seconds
  Retry: 3 attempts
  
Agent Version Compatibility:
  Rule: Agent version should match server major version
  Example: Server 25.x → Agent 25.x
  
Agent Resource Limits:
  CPU: <80%
  Memory: <85%
  Disk: <90%
```

**Health Check Validation:**
- All agents reachable
- Agent versions compatible
- Communication latency <5 seconds
- Failed ping rate <5%

---

### **Agent Topology Best Practices**

```
Topology Configuration:
  ✅ Define all agents in Topology
  ✅ Configure agent users with appropriate permissions
  ✅ Set up security permissions per agent
  ✅ Document agent purpose and owner
  ✅ Regular agent inventory review (quarterly)
  
Agent Naming Convention:
  Format: <environment>_<location>_<purpose>_<number>
  Example: PROD_NYC_BATCH_01, TEST_LON_WEB_02
```

---

## 📊 Workload Optimization

### **Queue Management**

Based on industry monitoring best practices:

```
Queue Depth Thresholds:
  Normal:   <500 jobs
  Warning:  500-1000 jobs
  Critical: >1000 jobs
  
Queue Age:
  Normal:   <5 minutes
  Warning:  5-15 minutes
  Critical: >15 minutes
  
Actions:
  - Monitor queue depth continuously
  - Alert when thresholds exceeded
  - Investigate stuck or slow jobs
  - Scale resources if consistently high
```

**Health Check Validation:**
- Queue depth within thresholds
- No jobs stuck in queue >1 hour
- Queue processing rate matches submission rate

---

### **Job Performance Metrics**

```
Success Rate:
  Target:   >95%
  Warning:  90-95%
  Critical: <90%
  
Job Duration:
  Monitor: Jobs running >4 hours
  Alert:   Jobs running >8 hours
  Action:  Investigate long-running jobs
  
Throughput:
  Measure: Jobs per hour
  Trend:   Should be stable or increasing
  Alert:   >20% decrease from baseline
```

**Health Check Validation:**
- Job success rate >95%
- No jobs stuck (running >expected duration)
- Throughput meets SLA requirements
- Failed jobs <5% of total

---

## 🔐 Security Hardening

### **Access Control**

```
User Management:
  ✅ Use role-based access control (RBAC)
  ✅ Implement least privilege principle
  ✅ Regular access reviews (quarterly)
  ✅ Disable default accounts
  ✅ Strong password policy (12+ chars, complexity)
  
Password Policy:
  Minimum Length: 12 characters
  Complexity: Upper, lower, number, special char
  Expiration: 90 days
  History: Remember last 5 passwords
  Lockout: 5 failed attempts
```

**Health Check Validation:**
- No default passwords in use
- Password policy enforced
- User accounts reviewed regularly
- Inactive accounts disabled

---

### **Encryption**

```
Data in Transit:
  ✅ Enable SSL/TLS for all connections
  ✅ Use TLS 1.2 or higher
  ✅ Strong cipher suites only
  ✅ Valid certificates (not self-signed in production)
  
Data at Rest:
  ✅ Encrypt database (TDE or file-level)
  ✅ Encrypt backup files
  ✅ Encrypt sensitive configuration files
  ✅ Secure key management
```

**Health Check Validation:**
- SSL/TLS enabled
- Certificates valid and not expiring soon (<30 days)
- Strong cipher suites configured
- Database encryption enabled

---

### **Audit Logging**

```
Audit Events to Log:
  ✅ User authentication (success/failure)
  ✅ Authorization changes
  ✅ Job submissions and modifications
  ✅ Configuration changes
  ✅ Security events
  
Log Retention:
  Minimum: 90 days
  Recommended: 1 year
  Compliance: Per regulatory requirements
  
Log Protection:
  ✅ Write-only access for application
  ✅ Read access restricted to auditors
  ✅ Tamper-evident (checksums)
  ✅ Backed up regularly
```

**Health Check Validation:**
- Audit logging enabled
- Logs retained per policy
- No gaps in audit trail
- Logs backed up

---

## 🔄 High Availability

### **Cluster Configuration**

```
Minimum HA Setup:
  - 2 server nodes (active-passive)
  - Shared database
  - Load balancer (optional)
  - Shared file system or replication
  
Recommended HA Setup:
  - 3+ server nodes (active-active)
  - Database cluster (RAC, Always On, etc.)
  - Load balancer with health checks
  - Replicated file systems
  
Failover Testing:
  Frequency: Quarterly
  Duration: 2-4 hours
  Validation: All services failover correctly
```

**Health Check Validation:**
- All cluster nodes healthy
- Failover configuration tested
- Replication lag <5 seconds
- Load balancer health checks passing

---

## 🧹 Maintenance & Housekeeping

### **Housekeeping Jobs**

```
Daily Housekeeping:
  ✅ Purge completed job history (>30 days)
  ✅ Archive old logs
  ✅ Clean temporary files
  ✅ Update database statistics
  
Weekly Housekeeping:
  ✅ Analyze database performance
  ✅ Review disk space usage
  ✅ Check for orphaned records
  ✅ Validate backup integrity
  
Monthly Housekeeping:
  ✅ Review and optimize indexes
  ✅ Analyze growth trends
  ✅ Review security logs
  ✅ Update documentation
```

**Health Check Validation:**
- Housekeeping jobs running successfully
- Database growth rate <10% per month
- No orphaned records
- Logs rotated properly

---

### **Backup Strategy**

```
Database Backups:
  Frequency: Daily (full), Hourly (incremental)
  Retention: 30 days
  Verification: Weekly restore test
  Location: Off-site or cloud storage
  
Configuration Backups:
  Frequency: Before each change
  Retention: 90 days
  Version Control: Git or similar
  
Application Backups:
  Frequency: Weekly
  Retention: 4 weeks
  Include: Job definitions, applications, calendars
```

**Health Check Validation:**
- Backup completed within 24 hours
- Backup verification successful
- Backup size growing appropriately
- Off-site copy exists

---

## 📊 Monitoring & Alerting

### **Key Metrics to Monitor**

Based on industry monitoring best practices:

```
System Metrics:
  - CPU utilization (every 1 minute)
  - Memory usage (every 1 minute)
  - Disk space (every 5 minutes)
  - Disk I/O (every 1 minute)
  - Network latency (every 5 minutes)
  
Database Metrics:
  - Connection pool usage (every 1 minute)
  - Query response time (every query)
  - Transaction log size (every 5 minutes)
  - Deadlocks (real-time)
  - Backup status (daily)
  
Server Metrics:
  - JVM heap usage (every 1 minute)
  - GC frequency and duration (every GC)
  - Thread count (every 1 minute)
  - Response time (every request)
  - Error rate (real-time)
  
Workload Metrics:
  - Queue depth (every 30 seconds)
  - Job success rate (every 5 minutes)
  - Job throughput (every 5 minutes)
  - Failed jobs (real-time)
  - Long-running jobs (every 5 minutes)
  
Agent Metrics:
  - Agent connectivity (every 1 minute)
  - Agent CPU/memory (every 5 minutes)
  - Communication latency (every 1 minute)
  - Failed pings (real-time)
```

---

### **Alert Configuration**

Based on industry alert best practices:

```
Alert Principles:
  ✅ Keep alert definitions low in number
  ✅ Alert only on actionable states
  ✅ Avoid alerting on closed/resolved states
  ✅ Use severity levels appropriately
  ✅ Implement alert escalation
  
Alert Severity Levels:
  CRITICAL: Requires immediate action (15 min response)
    - Server down
    - Database unreachable
    - All agents offline
    - Queue depth >2000
    
  HIGH: Requires urgent action (1 hour response)
    - High CPU/memory (>85%)
    - Disk space critical (>85%)
    - Job success rate <90%
    - Agent connectivity issues
    
  MEDIUM: Requires attention (4 hour response)
    - CPU/memory warning (>70%)
    - Disk space warning (>75%)
    - Job success rate 90-95%
    - Performance degradation
    
  LOW: Informational (24 hour response)
    - Configuration changes
    - Maintenance windows
    - Capacity planning alerts
```

**Health Check Validation:**
- Alerts configured for critical metrics
- Alert recipients defined and tested
- Alert history reviewed monthly
- No alert fatigue (too many alerts)

---

## 🎯 Performance Tuning

### **Database Optimization**

```
Index Strategy:
  ✅ Index all foreign keys
  ✅ Index frequently queried columns
  ✅ Index columns used in WHERE clauses
  ✅ Composite indexes for multi-column queries
  ✅ Regular index maintenance (rebuild/reorganize)
  
Query Optimization:
  ✅ Use prepared statements
  ✅ Avoid SELECT * (specify columns)
  ✅ Use appropriate JOIN types
  ✅ Implement query result caching
  ✅ Monitor slow queries (>500ms)
  
Table Maintenance:
  ✅ Update statistics weekly
  ✅ Rebuild indexes monthly
  ✅ Archive old data (>90 days)
  ✅ Partition large tables (>10M rows)
```

---

### **Workload Balancing**

```
Job Scheduling:
  ✅ Distribute jobs across time windows
  ✅ Avoid peak hour clustering
  ✅ Use job priorities appropriately
  ✅ Implement job dependencies correctly
  ✅ Balance load across agents
  
Resource Allocation:
  ✅ Assign resource-intensive jobs to dedicated agents
  ✅ Use resource pools for similar jobs
  ✅ Implement job throttling for heavy loads
  ✅ Schedule maintenance during low-usage periods
```

---

## 🔧 Troubleshooting Guide

### **Common Issues & Solutions**

#### **Issue 1: High CPU Usage**

```
Symptoms:
  - CPU >85%
  - Slow job processing
  - UI responsiveness issues
  
Diagnosis:
  1. Check running processes: top or Task Manager
  2. Identify CPU-intensive jobs
  3. Review thread pool configuration
  4. Check for infinite loops or stuck jobs
  
Solutions:
  - Kill stuck jobs
  - Optimize thread pool settings
  - Scale CPU resources
  - Optimize database queries
  - Review job scheduling patterns
```

---

#### **Issue 2: Memory Exhaustion**

```
Symptoms:
  - OutOfMemoryError in logs
  - Server crashes
  - Slow performance
  - High GC frequency
  
Diagnosis:
  1. Check JVM heap usage
  2. Review GC logs
  3. Analyze heap dumps
  4. Check for memory leaks
  
Solutions:
  - Increase JVM heap size (4GB → 8GB)
  - Tune GC settings
  - Fix memory leaks in custom code
  - Reduce concurrent job count
  - Implement job throttling
```

---

#### **Issue 3: Database Performance**

```
Symptoms:
  - Slow query response (>500ms)
  - Connection pool exhaustion
  - Deadlocks
  - High disk I/O
  
Diagnosis:
  1. Check slow query log
  2. Review execution plans
  3. Analyze index usage
  4. Check for blocking queries
  
Solutions:
  - Add missing indexes
  - Optimize slow queries
  - Increase connection pool size
  - Update database statistics
  - Partition large tables
  - Upgrade database hardware (SSD)
```

---

#### **Issue 4: Agent Connectivity**

```
Symptoms:
  - Agents showing offline
  - Jobs not executing
  - Communication timeouts
  
Diagnosis:
  1. Ping agent from server
  2. Check agent logs
  3. Verify network connectivity
  4. Check firewall rules
  
Solutions:
  - Restart agent service
  - Fix network issues
  - Update firewall rules
  - Verify agent configuration
  - Check agent resource usage
```

---

## 📈 Capacity Planning

### **Growth Projections**

```
Monitor These Trends:
  - Daily job count growth rate
  - Database size growth rate
  - Peak CPU/memory usage trends
  - Agent count growth
  
Planning Thresholds:
  When CPU consistently >60%:
    → Plan CPU upgrade within 3 months
  
  When Memory consistently >70%:
    → Plan memory upgrade within 3 months
  
  When Disk >60%:
    → Plan disk expansion within 6 months
  
  When Job count growing >10% per month:
    → Plan infrastructure scaling within 6 months
```

---

## ✅ Health Check Schedule

### **Recommended Frequency**

```
Daily:
  - Quick health check (5 minutes)
  - Review critical alerts
  - Check job success rate
  
Weekly:
  - Full health check (20 minutes)
  - Review performance trends
  - Analyze failed jobs
  - Review capacity metrics
  
Monthly:
  - Comprehensive review (2 hours)
  - Capacity planning analysis
  - Security audit
  - Documentation review
  - Disaster recovery test
  
Quarterly:
  - Full system audit
  - Performance tuning
  - License review
  - Upgrade planning
```

---

## 📊 Health Score Interpretation

### **Score Ranges**

```
90-100: ✅ EXCELLENT
  - System optimally configured
  - All best practices followed
  - No action required
  - Maintain current practices
  
75-89: 🟢 GOOD
  - Minor improvements recommended
  - System performing well
  - Review recommendations
  - Plan improvements
  
60-74: 🟡 FAIR
  - Several issues need attention
  - Performance may be impacted
  - Implement fixes within 1 week
  - Monitor closely
  
40-59: 🟠 POOR
  - Significant problems detected
  - Performance degraded
  - Immediate action required
  - Daily monitoring needed
  
0-39: 🔴 CRITICAL
  - System at risk
  - Emergency intervention needed
  - Escalate immediately
  - Consider service outage
```

---

## 📞 Support & Escalation

### **Escalation Path**

```
Level 1: Operations Team
  Response: 15 minutes
  Handles: Routine issues, alerts, monitoring
  
Level 2: System Administrators
  Response: 1 hour
  Handles: Configuration, performance, troubleshooting
  
Level 3: Database Administrators
  Response: 2 hours
  Handles: Database issues, optimization, recovery
  
Level 4: Broadcom Support
  Response: Per support contract
  Handles: Product bugs, advanced troubleshooting, patches
```

---

## 📚 References

### **Industry Best Practices**

1. **Industry Best Practices**
   - Performance tuning for small/medium/large environments
   - Thread pool configuration
   - Resource monitoring
   - Monitoring and reporting
   - Agent health verification
   - Forecast and prediction
   - Alert configuration
   - REST API health checks
   - Process count validation
   - Active execution monitoring
   - Detailed metrics collection

4. **dSeries Specific**
   - Server heap sizing (4GB minimum)
   - Pre-allocated memory model
   - Agent topology configuration
   - Database performance tuning

### **Documentation Links**

- dSeries Administration Guide
- dSeries Performance Tuning Guide
- Database Optimization Guide
- Security Configuration Guide
- High Availability Guide

---

## 🎯 Quick Reference Card

### **Critical Thresholds**

```
System:
  CPU:    70% warning, 85% critical
  Memory: 80% warning, 90% critical
  Disk:   75% warning, 85% critical
  
JVM:
  Heap:   4GB minimum, 8GB recommended
  GC:     <10 per minute, <500ms pause
  
Database:
  Connections: ≥5 available
  Query Time:  <500ms
  Backup Age:  <24 hours
  
Workload:
  Queue:      <1000 jobs
  Success:    >95%
  Failed:     <5%
```

### **Daily Checklist**

```
☐ Run quick health check
☐ Review critical alerts
☐ Check job success rate
☐ Verify backups completed
☐ Review disk space
☐ Check agent connectivity
```

---

**END OF BEST PRACTICES GUIDE**

**For health check execution:** See README.md  
**For installation:** See INSTALLATION_GUIDE.md  
**For troubleshooting:** See this guide, Section 10

---

**Version:** 1.0.0  
**Last Updated:** February 9, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
