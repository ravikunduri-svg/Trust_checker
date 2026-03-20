# Standalone Application Analysis Mode - User Guide

## Overview

Version 4.1.0 introduces **Standalone Application Analysis Mode** - a powerful feature that allows you to analyze exported dSeries application XML files **without requiring access to the dSeries installation or database**.

This is perfect for:
- **Customers** who want to analyze applications before deployment
- **Consultants** reviewing application designs offline
- **Developers** validating applications during development
- **Architects** performing design reviews
- **Security teams** auditing application configurations

## Key Benefits

✅ **No dSeries Installation Required** - Run anywhere with Java  
✅ **No Database Connection Needed** - Works offline  
✅ **Fast Analysis** - Seconds to analyze hundreds of applications  
✅ **Export from Any Source** - Desktop Client, CLI, or REST API  
✅ **Comprehensive Validation** - Security, best practices, cloud opportunities  
✅ **Portable** - Single JAR file, works on Windows, Linux, AIX, Mac  

## Quick Start

### 1. Export Your Applications

Choose one of these methods to export your applications:

#### Method 1: Desktop Client (GUI)
```
1. Open dSeries Desktop Client
2. Navigate to Services perspective
3. Right-click on Application
4. Select "Export" → "Export Application"
5. Choose location and save as XML
```

#### Method 2: Command Line (espappexport)
```bash
# Export single application
espappexport -A PAYROLL_PROCESSING -f PAYROLL_PROCESSING.xml

# Export multiple applications
espappexport -A "PAYROLL_*" -f payroll_apps.xml

# Export all applications
espappexport -A "*" -f all_applications.xml
```

#### Method 3: REST API
```bash
# Using curl
curl -X GET "http://server:9443/application/PAYROLL_PROCESSING/export" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -o PAYROLL_PROCESSING.xml

# Using PowerShell
Invoke-RestMethod -Uri "http://server:9443/application/PAYROLL_PROCESSING/export" `
  -Headers @{Authorization="Bearer YOUR_TOKEN"} `
  -OutFile "PAYROLL_PROCESSING.xml"
```

### 2. Run Analysis

#### Windows
```batch
REM Analyze directory of XML files
dseries_healthcheck.bat --analyze-apps C:\exports\applications

REM Analyze single XML file
dseries_healthcheck.bat -a C:\exports\PAYROLL_APP.xml

REM Short form
dseries_healthcheck.bat -a C:\exports\
```

#### Linux/Unix/AIX
```bash
# Analyze directory of XML files
./dseries_healthcheck.sh --analyze-apps /home/user/exports/applications

# Analyze single XML file
./dseries_healthcheck.sh -a /tmp/PAYROLL_APP.xml

# Short form
./dseries_healthcheck.sh -a /exports/
```

#### Direct Java Command
```bash
# If you prefer to run JAR directly
java -jar dseries-healthcheck.jar --analyze-apps /path/to/apps

# Or short form
java -jar dseries-healthcheck.jar -a /path/to/apps
```

### 3. Review Results

The tool will output:
- Analysis summary (files processed, jobs analyzed, violations, opportunities)
- Best practice violations (organized by category and severity)
- Cloud integration opportunities (organized by type)
- Specific recommendations with actionable steps

## Sample Output

```
═══════════════════════════════════════════════════════════════════
  ESP dSeries Application Best Practices Analyzer v4.1.0
═══════════════════════════════════════════════════════════════════
  Date: 2026-02-11 16:30:45
  Analysis Path: C:\exports\applications
═══════════════════════════════════════════════════════════════════

Starting application analysis...

Scanning directory: C:\exports\applications
Found 15 XML file(s) to analyze

═══════════════════════════════════════════════════════════════════
APPLICATION BEST PRACTICES ANALYSIS
═══════════════════════════════════════════════════════════════════

  ✓ PAYROLL_PROCESSING.xml (23 jobs)
  ✓ DATA_SYNC.xml (45 jobs)
  ✓ FILE_PROCESSING.xml (12 jobs)
  ✓ BACKUP_JOBS.xml (8 jobs)
  ... (11 more files)

─── Analysis Summary ───

Files processed: 15
Applications analyzed: 15
Total jobs analyzed: 342
Best practice violations: 23
Cloud integration opportunities: 47

─── Best Practice Violations ───

Category: Security

⚠️ Credentials in Command (HIGH)
   Application: PAYROLL_PROCESSING
   Job: EXPORT_SALARY_DATA
   Issue: Command may contain embedded credentials or passwords
   Recommendation: Use global variables with secure storage for credentials.

⚠️ Hardcoded Credentials in POJO (HIGH)
   Application: DATA_SYNC
   Job: SYNC_TO_S3
   Issue: Cloud plugin job has hardcoded credentials in parameters
   Recommendation: Use global variables ($$VAR or %VAR) for all credentials.

Category: Job Configuration

ℹ️ Hardcoded Paths (MEDIUM)
   Application: FILE_PROCESSING
   Job: PROCESS_FILES
   Issue: Command contains hardcoded file system paths
   Recommendation: Use global variables (%VAR) for paths to improve portability.

Category: Reliability

💡 Retry Configuration (LOW)
   Application: BACKUP_JOBS
   Job: DAILY_BACKUP
   Issue: Job has no retry configuration for transient failures
   Recommendation: Configure retry count and interval for better resilience.

─── Cloud Integration Opportunities ───

Integration Type: Storage

☁️ Amazon S3 Plugin Extension
   Application: FILE_PROCESSING
   Current Job: UPLOAD_TO_S3 (cmd_job)
   Opportunity: Replace AWS CLI S3 commands with native S3 plugin
   Benefit: Better error handling, built-in monitoring, simplified configuration
   Plugin: Amazon S3 Plugin Extension
   Documentation: https://techdocs.broadcom.com/...

☁️ Azure Blob Plugin Extension
   Application: DATA_SYNC
   Current Job: SYNC_TO_AZURE (cmd_job)
   Opportunity: Replace Azure CLI Blob commands with native Azure Blob plugin
   Benefit: Better error handling, built-in monitoring, simplified configuration
   Plugin: Azure Blob Plugin Extension
   Documentation: https://techdocs.broadcom.com/...

Integration Type: Data Processing

☁️ Databricks Plugin Extension
   Application: ANALYTICS_PIPELINE
   Current Job: RUN_SPARK_JOB (cmd_job)
   Opportunity: Replace Databricks CLI commands with native Databricks plugin
   Benefit: Better error handling, built-in monitoring, simplified configuration
   Plugin: Databricks Plugin Extension
   Documentation: https://techdocs.broadcom.com/...

─── Summary ───

⚠️ 8 HIGH severity issue(s) found - Address immediately
ℹ️ 12 MEDIUM severity issue(s) found - Plan fixes soon
💡 3 LOW severity issue(s) found - Consider improvements
☁️ 47 cloud integration opportunit(ies) - Modernize workload

For detailed implementation guidance, see:
  APPLICATION_BEST_PRACTICES_GUIDE.md

═══════════════════════════════════════════════════════════════════
Analysis complete!
═══════════════════════════════════════════════════════════════════
```

## Use Cases

### Use Case 1: Pre-Deployment Validation

**Scenario**: You want to validate applications before deploying to production.

**Steps**:
1. Export applications from development environment
2. Run standalone analysis
3. Fix all HIGH severity issues
4. Address MEDIUM issues if time permits
5. Deploy to production with confidence

**Benefits**:
- Catch security issues before production
- Ensure best practices compliance
- Identify modernization opportunities early

### Use Case 2: Consultant Review

**Scenario**: External consultant needs to review application designs.

**Steps**:
1. Customer exports applications and sends XML files
2. Consultant runs analysis on their laptop (no dSeries access needed)
3. Consultant provides detailed recommendations
4. Customer implements fixes

**Benefits**:
- No need for VPN or dSeries access
- Fast turnaround time
- Comprehensive analysis report

### Use Case 3: Security Audit

**Scenario**: Security team needs to audit all applications for credential exposure.

**Steps**:
1. Export all applications
2. Run standalone analysis
3. Filter for HIGH severity security issues
4. Generate remediation plan
5. Track fixes

**Benefits**:
- Identifies all credential exposure risks
- No database access needed
- Can be automated in CI/CD pipeline

### Use Case 4: Cloud Migration Assessment

**Scenario**: Planning migration to cloud and need to identify opportunities.

**Steps**:
1. Export all applications
2. Run standalone analysis
3. Review cloud integration opportunities
4. Prioritize migrations by benefit
5. Create migration roadmap

**Benefits**:
- Identifies all cloud opportunities
- Provides specific plugin recommendations
- Links to official documentation

### Use Case 5: Development Validation

**Scenario**: Developers want to validate applications during development.

**Steps**:
1. Export application from Desktop Client
2. Run analysis locally
3. Fix issues immediately
4. Re-export and re-analyze
5. Commit when clean

**Benefits**:
- Catch issues early in development
- No need to deploy to test environment
- Fast feedback loop

## Advanced Usage

### Analyze Multiple Directories

```bash
# Windows
for /d %d in (C:\exports\*) do dseries_healthcheck.bat -a "%d"

# Linux/Unix
for dir in /exports/*/; do ./dseries_healthcheck.sh -a "$dir"; done
```

### Filter Specific Applications

```bash
# Export only applications matching pattern
espappexport -A "PROD_*" -f prod_apps.xml

# Analyze
dseries_healthcheck.bat -a prod_apps.xml
```

### Automate in CI/CD Pipeline

```yaml
# Example GitHub Actions workflow
name: Application Analysis
on: [push]
jobs:
  analyze:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Analyze Applications
        run: |
          java -jar dseries-healthcheck.jar -a ./applications/
          if [ $? -ne 0 ]; then
            echo "Analysis found issues"
            exit 1
          fi
```

### Generate Reports

```bash
# Redirect output to file
dseries_healthcheck.bat -a C:\exports > analysis_report.txt

# Or use tee to see output and save
./dseries_healthcheck.sh -a /exports | tee analysis_report.txt
```

## What Gets Analyzed

### Application-Level
- JavaScript usage (inline vs repository)
- Application defaults configuration
- Calendar usage for scheduling
- Overall application structure

### Job-Level
- **cmd_job**: Hardcoded paths, credentials, CLI commands
- **script_job**: Inline script size
- **pojo_job**: Parameter security, credential management
- **db_job**: Database URLs for cloud opportunities
- **ftp_job**: Cloud storage migration opportunities

### Security Checks
- Hardcoded credentials in commands
- Hardcoded credentials in POJO parameters
- Password exposure in XML
- Credential management best practices

### Cloud Opportunities
- AWS CLI commands → AWS plugins
- Azure CLI commands → Azure plugins
- GCP CLI commands → GCP plugins
- Databricks CLI → Databricks plugin
- FTP jobs → Cloud storage plugins
- Database jobs → Cloud database services

## Limitations

1. **XML Files Only**: Only analyzes application XML files (not other dSeries artifacts)
2. **Static Analysis**: Cannot analyze runtime behavior or actual job execution
3. **Pattern-Based**: Cloud detection based on CLI command patterns
4. **No Database Checks**: Cannot validate against actual database schema
5. **No System Checks**: Cannot check CPU, memory, disk space (use full health check for that)

## Comparison: Standalone vs Full Health Check

| Feature | Standalone Mode | Full Health Check |
|---------|----------------|-------------------|
| **Requires dSeries Installation** | ❌ No | ✅ Yes |
| **Requires Database Connection** | ❌ No | ✅ Yes |
| **Application Analysis** | ✅ Yes | ✅ Yes |
| **Best Practices Validation** | ✅ Yes | ✅ Yes |
| **Cloud Opportunities** | ✅ Yes | ✅ Yes |
| **System Resource Checks** | ❌ No | ✅ Yes |
| **Database Health Checks** | ❌ No | ✅ Yes |
| **Log Analysis** | ❌ No | ✅ Yes |
| **Performance Metrics** | ❌ No | ✅ Yes |
| **Works Offline** | ✅ Yes | ❌ No |
| **Portable** | ✅ Yes | ⚠️ Partial |

**Recommendation**: Use standalone mode for application-specific analysis. Use full health check for comprehensive system validation.

## Troubleshooting

### Problem: "No XML files found"

**Solution**:
- Verify files have `.xml` extension
- Check file permissions
- Ensure you're pointing to correct directory
- Try analyzing a single file first

### Problem: "Parse errors"

**Solution**:
- Verify XML files are valid dSeries application exports
- Check for XML syntax errors
- Ensure files are not corrupted
- Try exporting again from dSeries

### Problem: "Java not found"

**Solution**:
- Install Java 8 or later
- Add Java to PATH
- Use full path to java command

### Problem: "JAR not found"

**Solution**:
- Ensure `dseries-healthcheck.jar` is in same directory as script
- Check file permissions
- Verify JAR file is not corrupted

## Best Practices for Using Standalone Mode

1. **Regular Analysis**: Run analysis on all applications monthly
2. **Pre-Deployment**: Always analyze before deploying to production
3. **Version Control**: Store analysis reports in version control
4. **Track Progress**: Compare reports over time to track improvements
5. **Automate**: Integrate into CI/CD pipeline for continuous validation
6. **Share Results**: Share reports with development teams for awareness
7. **Prioritize Fixes**: Address HIGH severity issues first
8. **Plan Migrations**: Use cloud opportunities for migration roadmap

## Integration with Development Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                    Development Workflow                      │
└─────────────────────────────────────────────────────────────┘

1. Developer creates/modifies application in Desktop Client
                           ↓
2. Export application to XML file
                           ↓
3. Run standalone analysis
   dseries_healthcheck.bat -a app.xml
                           ↓
4. Review violations and opportunities
                           ↓
5. Fix issues in Desktop Client
                           ↓
6. Re-export and re-analyze until clean
                           ↓
7. Deploy to dSeries environment
                           ↓
8. Run full health check for comprehensive validation
```

## FAQ

**Q: Can I analyze applications from older dSeries versions?**  
A: Yes, as long as they export to standard XML format.

**Q: Does this replace the full health check?**  
A: No, it complements it. Use standalone for application analysis, full check for system validation.

**Q: Can I analyze applications from multiple environments?**  
A: Yes, just export from each environment and analyze separately.

**Q: How long does analysis take?**  
A: Typically <5 seconds for 100 applications.

**Q: Can I customize the rules?**  
A: Currently no, but rules are based on Broadcom's official best practices.

**Q: Does it modify my XML files?**  
A: No, it's read-only analysis.

**Q: Can I run this in production?**  
A: Yes, it's safe - no database or system changes.

**Q: What if I have thousands of applications?**  
A: The tool handles large volumes efficiently. Consider analyzing by category.

## Support

For questions or issues:
1. Review this guide
2. Check APPLICATION_BEST_PRACTICES_GUIDE.md
3. Refer to Broadcom's official documentation
4. Contact your dSeries support team

## Version History

- **v4.1.0** (2026-02-11): Initial release of standalone application analysis mode
  - Standalone mode with `--analyze-apps` option
  - No dSeries installation required
  - No database connection needed
  - Full application analysis capabilities
  - Updated launcher scripts for both Windows and Unix

## References

- [APPLICATION_BEST_PRACTICES_GUIDE.md](APPLICATION_BEST_PRACTICES_GUIDE.md) - Detailed best practices guide
- [VERSION_4.0_RELEASE_NOTES.md](VERSION_4.0_RELEASE_NOTES.md) - Version 4.0 release notes
- [ESP dSeries Documentation](https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/ca-workload-automation-de/25-0.html)
- [Plugin Extensions](https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/workload-automation-plugin-extensions/)
