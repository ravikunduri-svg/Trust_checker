# dSeries Demo Environment - Quick Reference Card

**Version:** 1.0.0 | **Date:** February 11, 2026

---

## 🚀 One-Command Setup

```powershell
# Standard Demo (15 minutes)
.\Setup-DSeriesDemoEnvironment.ps1 -Version 12.4 -Environment Demo -ImportSamples -StartServices

# Sales Demo (15 minutes)
.\Setup-DSeriesDemoEnvironment.ps1 -Version 25.0 -Environment Sales -ImportSamples -StartServices
```

---

## 🔑 Default Credentials

| User | Password | Role |
|------|----------|------|
| demo_admin | Demo2026! | Administrator |
| demo_user | Demo2026! | User |
| sales_demo | Sales2026! | Administrator |

**Database:** dseries_demo / dSeries2026!

---

## 🌐 Access URLs

| Version | URL | Port |
|---------|-----|------|
| 12.4 | http://localhost:7599 | 7599 |
| 25.0 | http://localhost:7600 | 7600 |

**PostgreSQL:** localhost:5432

---

## ⚙️ Common Commands

### Start Services
```powershell
Start-Service "postgresql-5432"
Start-Service "ESP dSeries Workload Automation"
```

### Stop Services
```powershell
Stop-Service "ESP dSeries Workload Automation"
Stop-Service "postgresql-5432"
```

### Health Check
```powershell
cd C:\Codes\dseries_healthcheck
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\dSeries_Demo"
```

### View Logs
```powershell
Get-Content "C:\CA\dSeries_Demo\logs\server.log" -Tail 50
```

---

## 🎬 5-Minute Demo Script

### 1. Login (30 seconds)
- Open: http://localhost:7599
- Login: demo_admin / Demo2026!

### 2. Show Dashboard (1 minute)
- Active jobs
- System status
- Recent activity

### 3. Create Simple Job (2 minutes)
- Navigate to Job Definitions
- Click "New Job"
- Name: "Demo_Report"
- Schedule: Daily 8:00 AM
- Save and run manually

### 4. Monitor Execution (1 minute)
- Show real-time monitoring
- View job log
- Display output

### 5. Show Workflow (30 seconds)
- Open sample workflow
- Show visual designer
- Demonstrate dependencies

---

## 🎯 Demo Scenarios

### Scenario 1: Job Scheduling (5 min)
**Show:** Basic job creation and scheduling

### Scenario 2: Workflow (10 min)
**Show:** Complex workflow with dependencies

### Scenario 3: Monitoring (7 min)
**Show:** Real-time monitoring and alerts

### Scenario 4: Agent Management (8 min)
**Show:** Agent deployment and health

### Scenario 5: REST API (10 min)
**Show:** API integration capabilities

---

## 🔧 Quick Fixes

### Server Won't Start
```powershell
# Check service
Get-Service | Where-Object {$_.DisplayName -like "*dSeries*"}

# Check port
netstat -ano | findstr :7599

# View logs
Get-Content "C:\CA\dSeries_Demo\logs\server.log" -Tail 50
```

### Database Connection Failed
```powershell
# Test connection
psql -h localhost -p 5432 -U dseries_user -d dseries_demo

# Check PostgreSQL
Get-Service "postgresql-5432"
```

### Low Health Score
```powershell
# Fix JVM heap
.\fix_jvm_heap.ps1 -InstallDir "C:\CA\dSeries_Demo" -HeapSize 4096

# Re-run health check
.\Run-HealthCheck.ps1 -InstallDir "C:\CA\dSeries_Demo"
```

---

## 📊 System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| CPU | 4 cores | 8 cores |
| Memory | 8 GB | 16 GB |
| Disk | 50 GB | 100 GB |
| OS | Windows 10 | Windows Server 2019+ |

---

## 🎓 Pre-Demo Checklist

**1 Hour Before:**
- [ ] Start all services
- [ ] Verify login works
- [ ] Run sample job
- [ ] Check system resources
- [ ] Clear old logs

**During Demo:**
- [ ] Have quick start open
- [ ] Monitor system resources
- [ ] Have backup scenarios ready

---

## 📞 Quick Support

**Documentation:** `C:\CA\dSeries_Demo\QUICK_START.md`  
**Health Check:** `.\Run-HealthCheck.ps1`  
**Support Portal:** https://support.broadcom.com  
**Emergency:** Check troubleshooting guide

---

## 🔄 Version Comparison

| Feature | 12.4 | 25.0 |
|---------|------|------|
| REST API | ✅ | ✅ Enhanced |
| Workflow Designer | ✅ | ✅ Improved UI |
| Agent Management | ✅ | ✅ Auto-discovery |
| Monitoring | ✅ | ✅ Real-time |
| Cloud Integration | ❌ | ✅ New |

---

## 📁 Key Files

```
C:\CA\dSeries_Demo\
├── QUICK_START.md          # Setup guide
├── SETUP_SUMMARY.txt       # Environment details
├── conf\
│   ├── db.properties       # Database config
│   ├── server.properties   # Server config
│   └── windows.service.properties  # JVM config
└── logs\
    └── server.log          # Server logs
```

---

## 💡 Pro Tips

✅ **Warm up JVM** - Run test jobs before demo  
✅ **Clear logs** - Remove old logs for clarity  
✅ **Test scenarios** - Validate all demos work  
✅ **Have backup** - Prepare alternate scenarios  
✅ **Monitor resources** - Watch CPU/memory during demo  
✅ **Take notes** - Document customer questions  

---

## 🚨 Emergency Procedures

### Demo Environment Crashes
1. Check service status
2. Review server logs
3. Restart services
4. Switch to backup environment

### Performance Issues
1. Check system resources
2. Clear old logs
3. Restart services
4. Reduce concurrent jobs

### Database Issues
1. Check PostgreSQL service
2. Test database connection
3. Review database logs
4. Restart PostgreSQL

---

## 📈 Success Metrics

**Environment Ready:**
✅ Health score ≥ 85  
✅ Services running  
✅ Login successful  
✅ Sample job executes  

**Demo Ready:**
✅ All scenarios tested  
✅ Performance smooth  
✅ UI responsive  
✅ Backup available  

---

## 🎁 Sample Data Included

**Applications (5):**
- Daily Operations
- Data Processing
- File Transfer
- Database Maintenance
- Report Generation

**Jobs (5):**
- Daily_Backup
- Data_Processing
- Report_Generation
- File_Transfer
- Database_Maintenance

**Workflows (3):**
- ETL Workflow
- Batch Processing
- Multi-Step Workflow

---

## 🔐 Security Notes

**Demo Environment:**
- Simple passwords (documented)
- No password expiration
- Extended session timeout
- Audit logging enabled

**Production Environment:**
- Complex passwords required
- 90-day expiration
- Standard timeout
- Full audit logging

---

## 📦 Package Variants

**Sales Package (2 GB):**
- One version (12.4 or 25.0)
- 5 sample apps
- Basic documentation

**Services Package (4 GB):**
- Both versions (12.4 and 25.0)
- 10 sample apps
- Complete documentation

**Development Package (5 GB):**
- Both versions
- All samples
- Development tools
- API documentation

---

## 🎯 Key Talking Points

**For Sales:**
- Easy to deploy (15 minutes)
- Intuitive UI
- Powerful workflow engine
- REST API for integration
- Enterprise-grade reliability

**For Services:**
- Production-ready configuration
- Best practices included
- Health monitoring built-in
- Comprehensive documentation
- Professional support

---

## 📞 Contact Information

**Sales Support:**  
Email: dseries-sales@broadcom.com  
Phone: 1-800-XXX-XXXX

**Technical Support:**  
Portal: https://support.broadcom.com  
Email: dseries-support@broadcom.com

**Documentation:**  
Web: https://techdocs.broadcom.com  
Community: https://community.broadcom.com

---

## 🔄 Quick Setup Comparison

| Method | Time | Effort | Consistency |
|--------|------|--------|-------------|
| Automated | 15 min | Low | ✅ High |
| Manual | 70 min | High | ⚠️ Variable |

**Recommendation:** Always use automated setup!

---

## 📝 Post-Demo Actions

**Immediately After:**
- [ ] Document customer questions
- [ ] Capture requirements
- [ ] Schedule follow-up
- [ ] Export demo config (if interested)

**Within 24 Hours:**
- [ ] Send follow-up email
- [ ] Share relevant documentation
- [ ] Provide trial license (if applicable)
- [ ] Schedule next meeting

---

## ✅ Final Checklist

**Before Demo:**
✅ Environment setup complete  
✅ Health check passed  
✅ All scenarios tested  
✅ System resources adequate  
✅ Backup plan ready  

**During Demo:**
✅ Confident presentation  
✅ Answer questions clearly  
✅ Show relevant features  
✅ Document requirements  
✅ Close with next steps  

**After Demo:**
✅ Follow-up scheduled  
✅ Materials shared  
✅ Questions answered  
✅ Next steps defined  

---

**Print this card and keep it handy during demos!**

**Version:** 1.0.0  
**Last Updated:** February 11, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
