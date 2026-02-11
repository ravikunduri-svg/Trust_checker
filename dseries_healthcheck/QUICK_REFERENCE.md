# ESP dSeries Health Check - Quick Reference Guide

**Version:** 1.0.0 | **Date:** 2026-02-09

---

## 🚀 Quick Start

### **Run Health Check**

```bash
# Linux/Unix
cd /opt/CA/WA_DE/healthcheck
python3 dseries_healthcheck.py --quick

# Windows
cd C:\CA\WA_DE\healthcheck
python dseries_healthcheck.py --quick
```

### **View Results**

```bash
# Open HTML report
firefox /var/log/dseries/healthcheck/healthcheck_*.html

# View JSON
cat /var/log/dseries/healthcheck/healthcheck_*.json | jq .
```

---

## 📊 Health Score Quick Reference

| Score | Status | Action |
|-------|--------|--------|
| 90-100 | ✅ EXCELLENT | Maintain |
| 75-89 | 🟢 GOOD | Review recommendations |
| 60-74 | 🟡 FAIR | Fix within 1 week |
| 40-59 | 🟠 POOR | Immediate action |
| 0-39 | 🔴 CRITICAL | Emergency |

---

## 🎯 Critical Thresholds

### **System Resources**

| Metric | Warning | Critical |
|--------|---------|----------|
| CPU | 70% | 85% |
| Memory | 80% | 90% |
| Disk | 75% | 85% |
| Swap | 10% | 50% |

### **JVM Configuration**

| Setting | Minimum | Recommended | Maximum |
|---------|---------|-------------|---------|
| Heap Size | 4 GB | 4 GB | 8 GB |
| GC Frequency | - | <10/min | - |
| GC Pause | - | <500ms | - |

### **Database**

| Metric | Threshold |
|--------|-----------|
| Available Connections | ≥5 |
| Query Time | <500ms |
| Backup Age | <24 hours |
| Index Fragmentation | <30% |

### **Workload**

| Metric | Threshold |
|--------|-----------|
| Queue Depth | <1000 jobs |
| Success Rate | >95% |
| Failed Jobs | <5% |
| Job Duration | <4 hours |

---

## 🔧 Thread Pool Configuration

### **Based on Workload Size**

| Size | Daily Jobs | Download | DB Update | Selector |
|------|-----------|----------|-----------|----------|
| Small | 0-15K | 3 | 2 | 4 |
| Medium | 15K-75K | 6 | 4 | 8 |
| Large | 75K-150K | 8 | 8 | 12 |

---

## 📋 Check Categories

### **System Resources (SYS-xxx)**
- ✅ CPU utilization
- ✅ Memory usage
- ✅ Disk space
- ✅ Swap usage
- ✅ Network connectivity

### **Database (DB-xxx)**
- ✅ Connection pool
- ✅ Query performance
- ✅ Backup status
- ✅ Index health
- ✅ Growth rate

### **Server Configuration (SRV-xxx)**
- ✅ JVM heap size
- ✅ GC settings
- ✅ Thread pools
- ✅ Port accessibility
- ✅ License status

### **Agent Health (AGT-xxx)**
- ✅ Connectivity
- ✅ Version compatibility
- ✅ Resource usage
- ✅ Communication lag

### **Workload (WKL-xxx)**
- ✅ Queue depth
- ✅ Job success rate
- ✅ Failed jobs
- ✅ Throughput

### **Security (SEC-xxx)**
- ✅ SSL/TLS configuration
- ✅ Password policy
- ✅ Audit logging
- ✅ User access

---

## 🛠️ Common Commands

### **Health Check Execution**

```bash
# Quick check (5 minutes)
python3 dseries_healthcheck.py --quick

# Full check (15-20 minutes)
python3 dseries_healthcheck.py --full

# With email alert
python3 dseries_healthcheck.py --full --email

# With trending
python3 dseries_healthcheck.py --full --trending

# Custom output directory
python3 dseries_healthcheck.py --quick --output /tmp/reports

# Custom config
python3 dseries_healthcheck.py --full --config /etc/dseries/health.json
```

### **Report Management**

```bash
# List reports
ls -ltr /var/log/dseries/healthcheck/

# View latest HTML
firefox $(ls -t /var/log/dseries/healthcheck/*.html | head -1)

# View latest JSON
cat $(ls -t /var/log/dseries/healthcheck/*.json | head -1) | jq .

# Clean old reports (>90 days)
find /var/log/dseries/healthcheck -name "*.html" -mtime +90 -delete
```

---

## 🔍 Troubleshooting Quick Fixes

### **High CPU**
```bash
# Check processes
top -c | grep java

# Check thread count
ps -eLf | grep java | wc -l

# Review GC logs
tail -f /opt/CA/WA_DE/logs/gc.log
```

### **High Memory**
```bash
# Check JVM heap
jmap -heap $(pgrep -f "WA_DE")

# Generate heap dump
jmap -dump:format=b,file=/tmp/heap.hprof $(pgrep -f "WA_DE")

# Check swap
free -h
```

### **Database Issues**
```bash
# Test connection
psql -h localhost -p 5432 -U wauser -d WADB -c "SELECT 1"

# Check connections
psql -h localhost -p 5432 -U wauser -d WADB -c "SELECT count(*) FROM pg_stat_activity"

# Check slow queries
psql -h localhost -p 5432 -U wauser -d WADB -c "SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10"
```

### **Agent Issues**
```bash
# Ping agent
# Implement dSeries agent health check

# Check agent logs
tail -f /var/log/dseries/agent/*.log

# Restart agent
systemctl restart dseries-agent
```

---

## 📅 Recommended Schedule

### **Daily** (6 AM)
```cron
0 6 * * * /opt/CA/WA_DE/healthcheck/dseries_healthcheck.py --quick --email
```

### **Weekly** (Sunday 2 AM)
```cron
0 2 * * 0 /opt/CA/WA_DE/healthcheck/dseries_healthcheck.py --full --email
```

### **Monthly** (1st, 1 AM)
```cron
0 1 1 * * /opt/CA/WA_DE/healthcheck/dseries_healthcheck.py --full --trending --pdf
```

---

## 🎯 Best Practices Checklist

### **Daily**
- [ ] Run quick health check
- [ ] Review critical alerts
- [ ] Check job success rate
- [ ] Verify backups completed

### **Weekly**
- [ ] Run full health check
- [ ] Review performance trends
- [ ] Analyze failed jobs
- [ ] Check disk space growth

### **Monthly**
- [ ] Comprehensive system review
- [ ] Capacity planning analysis
- [ ] Security audit
- [ ] Update documentation

### **Quarterly**
- [ ] Full system audit
- [ ] Performance tuning
- [ ] Disaster recovery test
- [ ] License review

---

## 📞 Quick Support

### **Critical Issues**
- **Phone:** 1-800-DSERIES
- **Email:** dseries-critical@company.com
- **Response:** 15 minutes

### **General Support**
- **Portal:** https://support.company.com/dseries
- **Email:** dseries-support@company.com
- **Response:** 4 hours

---

## 📚 Documentation Links

| Document | Purpose |
|----------|---------|
| `README.md` | Overview and features |
| `INSTALLATION_GUIDE.md` | Setup instructions |
| `BEST_PRACTICES_GUIDE.md` | Detailed best practices |
| `QUICK_REFERENCE.md` | This document |

---

## 🔑 Key Files

| File | Location | Purpose |
|------|----------|---------|
| Health Check Script | `/opt/CA/WA_DE/healthcheck/dseries_healthcheck.py` | Main script |
| Configuration | `/opt/CA/WA_DE/healthcheck/config/healthcheck.json` | Settings |
| Reports | `/var/log/dseries/healthcheck/` | Output reports |
| Logs | `/var/log/dseries/healthcheck/*.log` | Execution logs |
| DB Password | `/opt/CA/WA_DE/.dbpass` | Encrypted password |

---

## ⚡ Emergency Procedures

### **System Down**
1. Check server process: `ps -ef | grep java`
2. Check logs: `tail -100 /opt/CA/WA_DE/logs/server.log`
3. Restart server: `systemctl restart dseries-server`
4. Run health check: `python3 dseries_healthcheck.py --quick`

### **Database Unreachable**
1. Test connectivity: `telnet dbhost 5432`
2. Check database: `systemctl status postgresql`
3. Restart database: `systemctl restart postgresql`
4. Verify connections: `psql -c "SELECT 1"`

### **High Queue Depth**
1. Check queue: Query dSeries queue table
2. Identify stuck jobs: Check job status
3. Kill stuck jobs: Use dSeries admin tools
4. Scale resources: Add agents or increase threads

---

## 📊 Health Check Output

### **Console Output**
```
═══════════════════════════════════════════════════════════════
  HEALTH CHECK SUMMARY
═══════════════════════════════════════════════════════════════

  Overall Health Score: 87/100
  Status: 🟢 GOOD

  Total Checks: 45
  ✅ Passed: 38
  ⚠️  Warnings: 5
  ❌ Failed: 2

  Reports:
    HTML: /var/log/dseries/healthcheck/healthcheck_20260209_060000.html
    JSON: /var/log/dseries/healthcheck/healthcheck_20260209_060000.json
    Log:  /var/log/dseries/healthcheck/healthcheck_20260209_060000.log

═══════════════════════════════════════════════════════════════
```

### **Exit Codes**
- `0` = Healthy (score ≥60)
- `1` = Unhealthy (score <60)

---

## 🔐 Security Notes

### **Password Management**
```bash
# Store passwords securely
echo "password" > /opt/CA/WA_DE/.dbpass
chmod 600 /opt/CA/WA_DE/.dbpass
chown waadmin:waadmin /opt/CA/WA_DE/.dbpass

# Use environment variables
export DB_PASSWORD="password"
```

### **Report Access**
```bash
# Restrict report access
chmod 700 /var/log/dseries/healthcheck
chown waadmin:waadmin /var/log/dseries/healthcheck
```

---

## 📈 Performance Tips

### **Optimize Health Check**
- Use `--quick` for daily checks
- Run `--full` during off-peak hours
- Enable parallel checks in config
- Adjust check timeout as needed

### **Reduce Impact**
- Schedule during maintenance windows
- Use read-only database user
- Limit concurrent checks
- Cache results when possible

---

## ✅ Pre-Production Checklist

Before deploying to production:

- [ ] Python 3.7+ installed
- [ ] All dependencies installed
- [ ] Configuration file updated
- [ ] Database credentials configured
- [ ] Report directory created
- [ ] Permissions set correctly
- [ ] Test execution successful
- [ ] Scheduled tasks configured
- [ ] Email alerts tested (if enabled)
- [ ] Documentation reviewed
- [ ] Team trained
- [ ] Support contacts updated

---

## 🎓 Training Resources

### **For Operators**
1. Run health check manually
2. Interpret health scores
3. Review HTML reports
4. Identify critical issues
5. Execute common fixes

### **For Administrators**
1. Configure thresholds
2. Customize checks
3. Schedule automation
4. Integrate with monitoring
5. Troubleshoot issues

### **For Managers**
1. Understand health scores
2. Review trends
3. Plan capacity
4. Make decisions
5. Allocate resources

---

**Keep this guide handy for quick reference!** 📖

For detailed information, see `BEST_PRACTICES_GUIDE.md`

---

**Version:** 1.0.0  
**Last Updated:** February 9, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
