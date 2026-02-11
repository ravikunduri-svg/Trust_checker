# ESP dSeries Workload Automation Health Check Tool

**Version:** 1.0.0  
**Date:** February 11, 2026  
**Status:** Production Ready

---

## 📋 Overview

Comprehensive health check tool and demo environment setup solution for ESP dSeries Workload Automation based on industry best practices and dSeries-specific requirements.

### **Purpose**

- Validate dSeries deployment follows best practices
- Identify performance bottlenecks and configuration issues
- Ensure system readiness for production workloads
- Provide actionable recommendations for optimization
- Support services and customer deployments
- **NEW:** Automated demo environment setup for sales and services teams

### **Key Features**

✅ **Automated Health Checks** - 50+ validation points  
✅ **Best Practice Compliance** - Based on industry standards  
✅ **Performance Analysis** - Database, JVM, and system metrics  
✅ **Security Validation** - Configuration and access controls  
✅ **Detailed Reporting** - HTML, JSON, and text formats  
✅ **Remediation Guidance** - Step-by-step fix instructions  
✅ **Trend Analysis** - Historical health tracking  
✅ **NEW: Demo Environment Setup** - One-command setup for demos (15 minutes)  
✅ **NEW: Multiple Version Support** - 12.4 and 25.0 side-by-side  
✅ **NEW: Sample Applications** - Pre-configured demo scenarios  

---

## 🚀 Quick Start

### **Option 1: Demo Environment Setup (NEW!)**

Perfect for sales demos, customer evaluations, and training:

```powershell
# One-command setup - Ready in 15 minutes!
cd C:\Codes\dseries_healthcheck
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Demo -ImportSamples -StartServices

# Access your demo environment
# URL: http://localhost:7599
# Login: demo_admin / Demo2026!
```

**See:** [DEMO_ENVIRONMENT_SETUP_GUIDE.md](DEMO_ENVIRONMENT_SETUP_GUIDE.md) for complete details.

---

### **Option 2: Health Check on Existing Installation**

```powershell
# Windows - Simple health check
cd C:\Codes\dseries_healthcheck
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\ESPdSeriesWAServer_R12_4"

# Windows - Using batch file
.\run_healthcheck_simple.bat "C:\CA\ESPdSeriesWAServer_R12_4"
```

```bash
# Linux - Quick health check (5 minutes)
./run_healthcheck.sh --quick

# Linux - Full health check (15-20 minutes)
./run_healthcheck.sh --full
```

---

## 🎯 Demo Environment Setup (NEW!)

### **For Sales and Services Teams**

Quickly set up complete dSeries demo environments with embedded PostgreSQL, sample applications, and pre-configured demo scenarios.

**Key Benefits:**
- ⏱️ **Fast Setup** - 15-20 minutes (automated)
- 🎯 **Demo-Ready** - Pre-configured with sample data
- 📦 **Complete** - Database, server, samples, users
- 🔄 **Multiple Versions** - Support for 12.4 and 25.0
- ✅ **Validated** - Automatic health check after setup

### **Quick Setup Commands**

```powershell
# Standard Demo Environment (12.4)
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Demo -ImportSamples -StartServices

# Sales Evaluation Environment (25.0)
.\Setup-DSeriesDemoEnvironment.ps1 -Version 25.0 -Environment Sales -ImportSamples -StartServices

# Development Environment
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Development

# Side-by-Side Comparison (12.4 and 25.0)
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -InstallPath "C:\CA\dSeries_12_4" -ServerPort 7599 -ImportSamples
.\Setup-DSeriesDemoEnvironment.ps1 -Version 25.0 -InstallPath "C:\CA\dSeries_25_0" -ServerPort 7600 -ImportSamples
```

### **What Gets Configured**

✅ **PostgreSQL Database** - Embedded, optimized for demos  
✅ **dSeries Server** - JVM heap 4GB, production-ready settings  
✅ **Demo Users** - demo_admin, demo_user, sales_demo  
✅ **Sample Applications** - 5 pre-configured applications  
✅ **Demo Jobs** - 10+ ready-to-run jobs  
✅ **Health Check** - Automatic validation after setup  
✅ **Quick Start Guide** - Created in installation directory  

### **Demo Scenarios Included**

1. **Job Scheduling** (5 min) - Create and schedule jobs
2. **Workflow Creation** (10 min) - Build complex workflows
3. **Agent Management** (8 min) - Deploy and manage agents
4. **Monitoring & Alerting** (7 min) - Real-time monitoring
5. **REST API Integration** (10 min) - API demonstrations

### **Documentation**

- **[DEMO_ENVIRONMENT_SETUP_GUIDE.md](DEMO_ENVIRONMENT_SETUP_GUIDE.md)** - Complete setup guide
- **[DEMO_QUICK_REFERENCE_CARD.md](DEMO_QUICK_REFERENCE_CARD.md)** - Quick reference for demos
- **[DEPLOYMENT_PACKAGE_CHECKLIST.md](DEPLOYMENT_PACKAGE_CHECKLIST.md)** - Package preparation
- **[SAMPLE_APPLICATIONS_TEMPLATE.md](SAMPLE_APPLICATIONS_TEMPLATE.md)** - Create custom demos

---

## 📊 Health Check Categories

### **1. System Resources**
- CPU utilization and availability
- Memory usage and heap configuration
- Disk space and I/O performance
- Network connectivity and latency

### **2. Database Health**
- Connection pool status
- Query performance
- Table sizes and growth
- Index effectiveness
- Backup status

### **3. Server Configuration**
- JVM heap size and GC settings
- Thread pool configuration
- Network settings
- Security configuration
- License status

### **4. Agent Health**
- Agent connectivity
- Agent version compatibility
- Agent resource usage
- Communication latency

### **5. Workload Performance**
- Job execution statistics
- Queue depths
- Processing throughput
- Failed job analysis

### **6. High Availability**
- Cluster status
- Failover configuration
- Replication status
- Backup procedures

### **7. Security**
- User access controls
- Password policies
- Encryption status
- Audit logging

### **8. Maintenance**
- Housekeeping jobs
- Log rotation
- Archive status
- Purge policies

---

## 📈 Health Score Interpretation

| Score | Status | Description | Action |
|-------|--------|-------------|--------|
| 90-100 | ✅ Excellent | System is optimally configured | Maintain current practices |
| 75-89 | 🟢 Good | Minor improvements recommended | Review recommendations |
| 60-74 | 🟡 Fair | Several issues need attention | Implement fixes within 1 week |
| 40-59 | 🟠 Poor | Significant problems detected | Immediate action required |
| 0-39 | 🔴 Critical | System at risk | Emergency intervention needed |

---

## 🔧 Configuration

### **config/healthcheck.conf**

```properties
# Health Check Configuration

# Database Connection
DB_HOST=localhost
DB_PORT=5432
DB_NAME=WADB
DB_USER=wauser
DB_PASSWORD_FILE=/opt/CA/WA_DE/.dbpass

# Server Settings
SERVER_HOST=localhost
SERVER_PORT=7507
SERVER_ADMIN_USER=admin
SERVER_ADMIN_PASSWORD_FILE=/opt/CA/WA_DE/.adminpass

# Thresholds
CPU_WARNING_THRESHOLD=70
CPU_CRITICAL_THRESHOLD=85
MEMORY_WARNING_THRESHOLD=80
MEMORY_CRITICAL_THRESHOLD=90
DISK_WARNING_THRESHOLD=75
DISK_CRITICAL_THRESHOLD=85

# JVM Settings
JVM_HEAP_MIN_MB=4096
JVM_HEAP_RECOMMENDED_MB=4096
JVM_HEAP_MAX_MB=8192

# Performance Thresholds
MAX_QUEUE_DEPTH=1000
MAX_JOB_DURATION_MINUTES=240
MAX_FAILED_JOBS_PERCENT=5

# Reporting
REPORT_OUTPUT_DIR=/var/log/dseries/healthcheck
REPORT_RETENTION_DAYS=90
ENABLE_EMAIL_ALERTS=true
EMAIL_RECIPIENTS=admin@company.com,ops@company.com

# Historical Tracking
ENABLE_TRENDING=true
TREND_DB_FILE=/var/log/dseries/healthcheck/trends.db
```

---

## 📄 Output Reports

### **1. Executive Summary (HTML)**
- Overall health score
- Critical issues
- Top 5 recommendations
- Trend charts

### **2. Detailed Report (HTML)**
- All check results
- Configuration analysis
- Performance metrics
- Comparison with best practices

### **3. JSON Report**
- Machine-readable format
- API integration ready
- Monitoring tool compatible

### **4. Remediation Guide (PDF)**
- Step-by-step fixes
- Configuration examples
- Best practice references

---

## 🎯 Best Practices Validated

### **Industry Best Practices**
✅ Thread pool sizing based on workload  
✅ Database connection optimization  
✅ JVM heap configuration  
✅ Resource monitoring  
✅ Alert configuration  
✅ Forecast and prediction capabilities  
✅ Event monitoring  
✅ Agent health verification  
✅ High availability configuration  
✅ Test environment validation  
✅ REST API health checks  
✅ Process count validation  
✅ Active execution monitoring  
✅ Connection pool status  
✅ Detailed metrics collection  

### **From dSeries Best Practices**
✅ Server heap sizing (4GB minimum)  
✅ Agent topology configuration  
✅ Database performance tuning  
✅ Housekeeping procedures  
✅ Security configuration  

---

## 🔍 Check Details

### **System Resource Checks**

| Check ID | Description | Severity | Threshold |
|----------|-------------|----------|-----------|
| SYS-001 | CPU Utilization | Warning | >70% |
| SYS-002 | Memory Usage | Critical | >90% |
| SYS-003 | Disk Space | Critical | >85% |
| SYS-004 | Disk I/O Wait | Warning | >20% |
| SYS-005 | Network Latency | Warning | >100ms |
| SYS-006 | File Descriptors | Warning | >80% used |
| SYS-007 | Swap Usage | Warning | >10% |

### **Database Health Checks**

| Check ID | Description | Severity | Threshold |
|----------|-------------|----------|-----------|
| DB-001 | Connection Pool | Critical | <5 available |
| DB-002 | Query Response Time | Warning | >500ms |
| DB-003 | Table Growth Rate | Info | >10% per day |
| DB-004 | Index Fragmentation | Warning | >30% |
| DB-005 | Backup Age | Critical | >24 hours |
| DB-006 | Transaction Log Size | Warning | >5GB |
| DB-007 | Deadlocks | Warning | >5 per hour |

### **Server Configuration Checks**

| Check ID | Description | Severity | Threshold |
|----------|-------------|----------|-----------|
| SRV-001 | JVM Heap Size | Critical | <4GB |
| SRV-002 | GC Frequency | Warning | >10 per min |
| SRV-003 | Thread Pool Size | Warning | Undersized |
| SRV-004 | License Expiry | Critical | <30 days |
| SRV-005 | SSL/TLS Enabled | Warning | Not enabled |
| SRV-006 | Audit Logging | Warning | Not enabled |
| SRV-007 | Password Policy | Warning | Weak |

### **Agent Health Checks**

| Check ID | Description | Severity | Threshold |
|----------|-------------|----------|-----------|
| AGT-001 | Agent Connectivity | Critical | Unreachable |
| AGT-002 | Agent Version | Warning | Outdated |
| AGT-003 | Agent CPU Usage | Warning | >80% |
| AGT-004 | Agent Memory | Warning | >85% |
| AGT-005 | Agent Disk Space | Critical | >90% |
| AGT-006 | Communication Lag | Warning | >5 seconds |
| AGT-007 | Failed Pings | Warning | >5% |

### **Workload Performance Checks**

| Check ID | Description | Severity | Threshold |
|----------|-------------|----------|-----------|
| WKL-001 | Queue Depth | Warning | >1000 jobs |
| WKL-002 | Failed Jobs | Warning | >5% |
| WKL-003 | Long-Running Jobs | Info | >4 hours |
| WKL-004 | Job Throughput | Info | Trending down |
| WKL-005 | Peak Load Handling | Warning | >90% capacity |
| WKL-006 | Application Errors | Warning | >10 per hour |
| WKL-007 | Stuck Jobs | Critical | Any stuck >1hr |

---

## 🛠️ Troubleshooting

### **Common Issues**

#### **Issue: Health check fails to connect to database**

```bash
# Check database connectivity
telnet $DB_HOST $DB_PORT

# Verify credentials
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1"

# Check password file permissions
ls -l /opt/CA/WA_DE/.dbpass
chmod 600 /opt/CA/WA_DE/.dbpass
```

#### **Issue: Insufficient permissions**

```bash
# Run as dSeries admin user
su - waadmin
./run_healthcheck.sh

# Or specify credentials
./run_healthcheck.sh --user admin --password-file /path/to/pass
```

#### **Issue: Report generation fails**

```bash
# Check output directory permissions
mkdir -p /var/log/dseries/healthcheck
chown waadmin:waadmin /var/log/dseries/healthcheck
chmod 755 /var/log/dseries/healthcheck

# Check disk space
df -h /var/log
```

---

## 📅 Recommended Schedule

### **Daily Quick Check**
```cron
# Run quick health check daily at 6 AM
0 6 * * * /opt/CA/WA_DE/healthcheck/run_healthcheck.sh --quick --email
```

### **Weekly Full Check**
```cron
# Run full health check weekly on Sunday at 2 AM
0 2 * * 0 /opt/CA/WA_DE/healthcheck/run_healthcheck.sh --full --email
```

### **Monthly Comprehensive Review**
```cron
# Run comprehensive check with trending on 1st of month
0 1 1 * * /opt/CA/WA_DE/healthcheck/run_healthcheck.sh --full --trending --pdf
```

---

## 🔐 Security Considerations

### **Password Management**
- Store passwords in encrypted files
- Use file permissions (600) to protect credentials
- Rotate passwords regularly
- Never hardcode passwords in scripts

### **Access Control**
- Run health check as dedicated service account
- Limit read access to configuration files
- Restrict report access to authorized personnel
- Enable audit logging for health check runs

### **Data Protection**
- Reports may contain sensitive information
- Store reports in secure locations
- Implement retention policies
- Consider encryption for archived reports

---

## 📞 Support

### **For Issues**
- Email: dseries-support@company.com
- Portal: https://support.company.com/dseries
- Phone: 1-800-DSERIES

### **For Enhancements**
- Submit feature requests via support portal
- Contribute improvements via internal Git repository

---

## 📚 References

### **Industry Best Practices**
- Workload Automation Performance Tuning
- Enterprise Monitoring and Reporting
- Health Check API Standards
- dSeries Deployment Architecture

### **Documentation**
- dSeries Administration Guide
- dSeries Performance Tuning Guide
- Database Optimization Guide
- Security Configuration Guide

---

## 🔄 Version History

### **Version 1.0.0 (2026-02-09)**
- Initial release
- 50+ health checks
- HTML, JSON, and PDF reporting
- Trend analysis
- Remediation guidance

---

## 📝 License

Copyright © 2026 Broadcom. All Rights Reserved.

This tool is provided for use with ESP dSeries Workload Automation.
Redistribution requires written permission from Broadcom.

---

**Ready to deploy!** 🚀

See `INSTALLATION_GUIDE.md` for detailed setup instructions.
