# dSeries Sample Applications Template

**Version:** 1.0.0  
**Date:** February 11, 2026  
**Purpose:** Templates for creating demo applications and jobs

---

## 📋 Overview

This document provides templates and guidelines for creating sample applications and jobs for dSeries demo environments. Use these templates to quickly create realistic demo scenarios.

---

## 🎯 Sample Application 1: Daily Operations

### Application Details
- **Name:** Daily_Operations
- **Description:** Common daily operational tasks
- **Category:** Operations
- **Owner:** demo_admin
- **Priority:** High

### Jobs Included

#### Job 1: Morning_Health_Check
```properties
Name: Morning_Health_Check
Description: Daily system health check
Command: C:\Scripts\health_check.bat
Schedule: Daily at 6:00 AM
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 30 minutes
Retry: 2 times
Notification: On failure
```

#### Job 2: Database_Backup
```properties
Name: Database_Backup
Description: Daily database backup
Command: C:\Scripts\backup_database.bat
Schedule: Daily at 2:00 AM
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 60 minutes
Retry: 3 times
Notification: On failure
Dependencies: None
```

#### Job 3: Log_Rotation
```properties
Name: Log_Rotation
Description: Rotate and archive log files
Command: C:\Scripts\rotate_logs.bat
Schedule: Daily at 3:00 AM
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 15 minutes
Retry: 2 times
Notification: On failure
Dependencies: Database_Backup (success)
```

#### Job 4: System_Cleanup
```properties
Name: System_Cleanup
Description: Clean temporary files and directories
Command: C:\Scripts\cleanup_temp.bat
Schedule: Daily at 4:00 AM
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 30 minutes
Retry: 1 time
Notification: On failure
Dependencies: Log_Rotation (success)
```

### Workflow Diagram
```
┌─────────────────────┐
│ Morning_Health_Check│ (6:00 AM)
└─────────────────────┘

┌─────────────────────┐
│  Database_Backup    │ (2:00 AM)
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Log_Rotation      │ (3:00 AM)
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  System_Cleanup     │ (4:00 AM)
└─────────────────────┘
```

---

## 🎯 Sample Application 2: Data Processing

### Application Details
- **Name:** Data_Processing_ETL
- **Description:** Extract, Transform, Load workflow
- **Category:** Data Processing
- **Owner:** demo_admin
- **Priority:** High

### Jobs Included

#### Job 1: Extract_Source_Data
```properties
Name: Extract_Source_Data
Description: Extract data from source systems
Command: C:\Scripts\extract_data.bat
Parameters: --source=production --format=csv
Schedule: Weekdays at 8:00 AM
Run Calendar: WEEKDAYS
Time Zone: Local
Max Runtime: 45 minutes
Retry: 3 times
Notification: On failure
```

#### Job 2: Validate_Data
```properties
Name: Validate_Data
Description: Validate extracted data
Command: C:\Scripts\validate_data.bat
Parameters: --input=extracted_data.csv
Schedule: On demand (triggered by Extract_Source_Data)
Run Calendar: WEEKDAYS
Time Zone: Local
Max Runtime: 20 minutes
Retry: 2 times
Notification: On failure
Dependencies: Extract_Source_Data (success)
```

#### Job 3: Transform_Data
```properties
Name: Transform_Data
Description: Transform and enrich data
Command: C:\Scripts\transform_data.bat
Parameters: --input=validated_data.csv --output=transformed_data.csv
Schedule: On demand (triggered by Validate_Data)
Run Calendar: WEEKDAYS
Time Zone: Local
Max Runtime: 60 minutes
Retry: 2 times
Notification: On failure
Dependencies: Validate_Data (success)
```

#### Job 4: Load_Target_Database
```properties
Name: Load_Target_Database
Description: Load data into target database
Command: C:\Scripts\load_data.bat
Parameters: --input=transformed_data.csv --target=warehouse
Schedule: On demand (triggered by Transform_Data)
Run Calendar: WEEKDAYS
Time Zone: Local
Max Runtime: 45 minutes
Retry: 3 times
Notification: On success and failure
Dependencies: Transform_Data (success)
```

#### Job 5: Generate_ETL_Report
```properties
Name: Generate_ETL_Report
Description: Generate ETL process report
Command: C:\Scripts\generate_etl_report.bat
Schedule: On demand (triggered by Load_Target_Database)
Run Calendar: WEEKDAYS
Time Zone: Local
Max Runtime: 10 minutes
Retry: 1 time
Notification: On success
Dependencies: Load_Target_Database (success)
```

### Workflow Diagram
```
┌──────────────────────┐
│ Extract_Source_Data  │ (8:00 AM Weekdays)
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   Validate_Data      │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   Transform_Data     │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Load_Target_Database │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Generate_ETL_Report  │
└──────────────────────┘
```

---

## 🎯 Sample Application 3: File Transfer

### Application Details
- **Name:** File_Transfer_Automation
- **Description:** Automated file transfer between systems
- **Category:** File Management
- **Owner:** demo_admin
- **Priority:** Medium

### Jobs Included

#### Job 1: Monitor_Source_Directory
```properties
Name: Monitor_Source_Directory
Description: Monitor source directory for new files
Command: C:\Scripts\monitor_directory.bat
Parameters: --path=C:\Source --pattern=*.dat
Schedule: Every 15 minutes
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 5 minutes
Retry: 1 time
Notification: On failure
```

#### Job 2: Validate_Files
```properties
Name: Validate_Files
Description: Validate file format and integrity
Command: C:\Scripts\validate_files.bat
Schedule: On demand (triggered by Monitor_Source_Directory)
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 10 minutes
Retry: 2 times
Notification: On failure
Dependencies: Monitor_Source_Directory (success)
```

#### Job 3: Transfer_Files
```properties
Name: Transfer_Files
Description: Transfer files to target system
Command: C:\Scripts\transfer_files.bat
Parameters: --source=C:\Source --target=\\server\share\Target
Schedule: On demand (triggered by Validate_Files)
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 30 minutes
Retry: 3 times
Notification: On failure
Dependencies: Validate_Files (success)
```

#### Job 4: Archive_Files
```properties
Name: Archive_Files
Description: Archive transferred files
Command: C:\Scripts\archive_files.bat
Parameters: --source=C:\Source --archive=C:\Archive
Schedule: On demand (triggered by Transfer_Files)
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 15 minutes
Retry: 2 times
Notification: On failure
Dependencies: Transfer_Files (success)
```

#### Job 5: Send_Transfer_Notification
```properties
Name: Send_Transfer_Notification
Description: Send email notification of transfer completion
Command: C:\Scripts\send_notification.bat
Parameters: --recipient=admin@company.com --subject="File Transfer Complete"
Schedule: On demand (triggered by Archive_Files)
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 5 minutes
Retry: 1 time
Notification: On failure
Dependencies: Archive_Files (success)
```

### Workflow Diagram
```
┌──────────────────────────┐
│ Monitor_Source_Directory │ (Every 15 min)
└───────────┬──────────────┘
            │
            ▼
┌──────────────────────────┐
│    Validate_Files        │
└───────────┬──────────────┘
            │
            ▼
┌──────────────────────────┐
│    Transfer_Files        │
└───────────┬──────────────┘
            │
            ▼
┌──────────────────────────┐
│    Archive_Files         │
└───────────┬──────────────┘
            │
            ▼
┌──────────────────────────┐
│ Send_Transfer_Notification│
└──────────────────────────┘
```

---

## 🎯 Sample Application 4: Report Generation

### Application Details
- **Name:** Automated_Reporting
- **Description:** Automated report generation and distribution
- **Category:** Reporting
- **Owner:** demo_admin
- **Priority:** High

### Jobs Included

#### Job 1: Extract_Report_Data
```properties
Name: Extract_Report_Data
Description: Extract data for reports
Command: C:\Scripts\extract_report_data.bat
Parameters: --date=today --format=csv
Schedule: Daily at 6:00 PM
Run Calendar: WEEKDAYS
Time Zone: Local
Max Runtime: 30 minutes
Retry: 2 times
Notification: On failure
```

#### Job 2: Generate_Daily_Report
```properties
Name: Generate_Daily_Report
Description: Generate daily operations report
Command: C:\Scripts\generate_report.bat
Parameters: --template=daily --output=daily_report.pdf
Schedule: On demand (triggered by Extract_Report_Data)
Run Calendar: WEEKDAYS
Time Zone: Local
Max Runtime: 20 minutes
Retry: 2 times
Notification: On failure
Dependencies: Extract_Report_Data (success)
```

#### Job 3: Generate_Summary_Report
```properties
Name: Generate_Summary_Report
Description: Generate executive summary report
Command: C:\Scripts\generate_report.bat
Parameters: --template=summary --output=summary_report.pdf
Schedule: On demand (triggered by Extract_Report_Data)
Run Calendar: WEEKDAYS
Time Zone: Local
Max Runtime: 15 minutes
Retry: 2 times
Notification: On failure
Dependencies: Extract_Report_Data (success)
```

#### Job 4: Distribute_Reports
```properties
Name: Distribute_Reports
Description: Email reports to stakeholders
Command: C:\Scripts\distribute_reports.bat
Parameters: --recipients=management@company.com --attach=*.pdf
Schedule: On demand (triggered by Generate_Daily_Report and Generate_Summary_Report)
Run Calendar: WEEKDAYS
Time Zone: Local
Max Runtime: 10 minutes
Retry: 3 times
Notification: On success and failure
Dependencies: Generate_Daily_Report (success), Generate_Summary_Report (success)
```

### Workflow Diagram
```
┌──────────────────────┐
│ Extract_Report_Data  │ (6:00 PM Weekdays)
└──────────┬───────────┘
           │
           ├──────────────────────┐
           │                      │
           ▼                      ▼
┌──────────────────────┐ ┌──────────────────────┐
│ Generate_Daily_Report│ │Generate_Summary_Report│
└──────────┬───────────┘ └──────────┬───────────┘
           │                        │
           └────────────┬───────────┘
                        │
                        ▼
              ┌──────────────────────┐
              │  Distribute_Reports  │
              └──────────────────────┘
```

---

## 🎯 Sample Application 5: Database Maintenance

### Application Details
- **Name:** Database_Maintenance
- **Description:** Automated database maintenance tasks
- **Category:** Database
- **Owner:** demo_admin
- **Priority:** High

### Jobs Included

#### Job 1: Database_Health_Check
```properties
Name: Database_Health_Check
Description: Check database health and performance
Command: C:\Scripts\db_health_check.bat
Schedule: Daily at 1:00 AM
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 15 minutes
Retry: 2 times
Notification: On failure
```

#### Job 2: Update_Statistics
```properties
Name: Update_Statistics
Description: Update database statistics
Command: C:\Scripts\update_statistics.bat
Schedule: Daily at 1:30 AM
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 45 minutes
Retry: 2 times
Notification: On failure
Dependencies: Database_Health_Check (success)
```

#### Job 3: Rebuild_Indexes
```properties
Name: Rebuild_Indexes
Description: Rebuild fragmented indexes
Command: C:\Scripts\rebuild_indexes.bat
Schedule: Weekly on Sunday at 2:00 AM
Run Calendar: SUNDAYS
Time Zone: Local
Max Runtime: 120 minutes
Retry: 1 time
Notification: On failure
Dependencies: Update_Statistics (success)
```

#### Job 4: Purge_Old_Data
```properties
Name: Purge_Old_Data
Description: Purge data older than 90 days
Command: C:\Scripts\purge_old_data.bat
Parameters: --retention=90
Schedule: Weekly on Sunday at 4:00 AM
Run Calendar: SUNDAYS
Time Zone: Local
Max Runtime: 60 minutes
Retry: 2 times
Notification: On success and failure
Dependencies: Rebuild_Indexes (success)
```

#### Job 5: Database_Backup_Verify
```properties
Name: Database_Backup_Verify
Description: Verify database backup integrity
Command: C:\Scripts\verify_backup.bat
Schedule: Daily at 5:00 AM
Run Calendar: ALL_DAYS
Time Zone: Local
Max Runtime: 30 minutes
Retry: 2 times
Notification: On failure
Dependencies: Purge_Old_Data (success)
```

### Workflow Diagram
```
┌──────────────────────┐
│Database_Health_Check │ (1:00 AM Daily)
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Update_Statistics   │ (1:30 AM Daily)
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Rebuild_Indexes     │ (2:00 AM Sunday)
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Purge_Old_Data      │ (4:00 AM Sunday)
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│Database_Backup_Verify│ (5:00 AM Daily)
└──────────────────────┘
```

---

## 📝 Script Templates

### Template 1: Simple Batch Script

```batch
@echo off
REM ============================================================================
REM Script: sample_job.bat
REM Description: Sample job script template
REM Author: Demo Admin
REM Date: 2026-02-11
REM ============================================================================

echo [%DATE% %TIME%] Starting job: %~n0

REM Set variables
set LOG_DIR=C:\Logs
set LOG_FILE=%LOG_DIR%\%~n0_%DATE:~-4,4%%DATE:~-10,2%%DATE:~-7,2%.log

REM Create log directory if not exists
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM Start logging
echo [%DATE% %TIME%] Job started >> "%LOG_FILE%"

REM ============================================================================
REM Main job logic here
REM ============================================================================

echo [%DATE% %TIME%] Processing data...
REM Your processing logic here

REM Simulate processing
timeout /t 5 /nobreak >nul

echo [%DATE% %TIME%] Processing complete

REM ============================================================================
REM Cleanup and exit
REM ============================================================================

echo [%DATE% %TIME%] Job completed successfully >> "%LOG_FILE%"
echo [%DATE% %TIME%] Job completed: %~n0

exit /b 0
```

---

### Template 2: PowerShell Script

```powershell
<#
.SYNOPSIS
    Sample job script template

.DESCRIPTION
    Detailed description of what this job does

.PARAMETER InputPath
    Path to input files

.PARAMETER OutputPath
    Path to output files

.EXAMPLE
    .\sample_job.ps1 -InputPath "C:\Input" -OutputPath "C:\Output"

.NOTES
    Author: Demo Admin
    Date: 2026-02-11
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$false)]
    [string]$InputPath = "C:\Input",
    
    [Parameter(Mandatory=$false)]
    [string]$OutputPath = "C:\Output"
)

$ErrorActionPreference = "Stop"

# Setup logging
$logDir = "C:\Logs"
$logFile = Join-Path $logDir "$($MyInvocation.MyCommand.Name)_$(Get-Date -Format 'yyyyMMdd_HHmmss').log"

if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}

function Write-Log {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "[$timestamp] $Message"
    Write-Host $logMessage
    $logMessage | Out-File -FilePath $logFile -Append
}

try {
    Write-Log "Job started: $($MyInvocation.MyCommand.Name)"
    Write-Log "Input path: $InputPath"
    Write-Log "Output path: $OutputPath"
    
    # ========================================================================
    # Main job logic here
    # ========================================================================
    
    Write-Log "Processing data..."
    
    # Your processing logic here
    Start-Sleep -Seconds 5
    
    Write-Log "Processing complete"
    
    # ========================================================================
    # Cleanup and exit
    # ========================================================================
    
    Write-Log "Job completed successfully"
    exit 0
    
} catch {
    Write-Log "ERROR: $($_.Exception.Message)"
    Write-Log "Stack trace: $($_.ScriptStackTrace)"
    exit 1
}
```

---

## 🎯 Creating Custom Applications

### Step 1: Define Application

```
Application Name: [Your_Application_Name]
Description: [Brief description]
Category: [Operations/Data Processing/Reporting/etc.]
Owner: demo_admin
Priority: [High/Medium/Low]
```

### Step 2: Define Jobs

For each job:
```
Job Name: [Job_Name]
Description: [What the job does]
Command: [Script path and name]
Parameters: [Command-line parameters]
Schedule: [When it runs]
Run Calendar: [Which days]
Max Runtime: [Maximum execution time]
Retry: [Number of retries]
Notification: [When to notify]
Dependencies: [Parent jobs]
```

### Step 3: Create Workflow Diagram

Use ASCII art to show job flow:
```
┌─────────┐
│  Job 1  │
└────┬────┘
     │
     ▼
┌─────────┐
│  Job 2  │
└─────────┘
```

### Step 4: Create Scripts

Use templates above to create actual scripts.

### Step 5: Test

1. Create application in dSeries
2. Add jobs
3. Define dependencies
4. Test execution
5. Verify results

---

## 📊 Application Categories

### Operations
- System maintenance
- Health checks
- Backups
- Cleanup tasks

### Data Processing
- ETL workflows
- Data validation
- Data transformation
- Data loading

### File Management
- File transfers
- File monitoring
- File archiving
- File validation

### Reporting
- Report generation
- Data extraction
- Report distribution
- Dashboard updates

### Database
- Database maintenance
- Index rebuilding
- Statistics updates
- Data purging

---

## ✅ Best Practices

### Naming Conventions
- Use descriptive names
- Use underscores for spaces
- Include category prefix
- Keep names concise

### Scheduling
- Avoid peak hours
- Consider dependencies
- Allow buffer time
- Use appropriate calendars

### Error Handling
- Always include retry logic
- Log all errors
- Send notifications
- Have fallback procedures

### Documentation
- Document each job
- Include parameters
- Note dependencies
- Explain purpose

---

**Use these templates to create realistic demo scenarios!**

**Version:** 1.0.0  
**Last Updated:** February 11, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
