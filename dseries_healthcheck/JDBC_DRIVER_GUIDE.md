# JDBC Driver Configuration Guide

## Overview

The dSeries Health Check tool requires JDBC drivers to connect to the database and perform database-related health checks. This guide explains how JDBC drivers are automatically detected and loaded.

## Automatic JDBC Driver Detection

### How It Works

The launcher scripts (`dseries_healthcheck.bat` and `dseries_healthcheck.sh`) automatically build the Java classpath by scanning the following directories in your dSeries installation:

```
<install_dir>/lib/*
<install_dir>/lib/jdbc/*
<install_dir>/lib/ext/*
<install_dir>/third-party/*
<install_dir>/ext/*
<install_dir>/webserver/lib/*
```

**All JAR files** in these directories are automatically included in the classpath, including JDBC drivers.

## Supported Databases

The tool supports the following databases:

| Database | JDBC Driver Class | Common JAR Names |
|----------|------------------|------------------|
| **PostgreSQL** | `org.postgresql.Driver` | `postgresql-*.jar` |
| **Oracle** | `oracle.jdbc.driver.OracleDriver` | `ojdbc*.jar` |
| **SQL Server** | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | `mssql-jdbc-*.jar`, `sqljdbc*.jar` |

## Database Configuration

The tool reads database connection details from:

```
<install_dir>/conf/db.properties
```

### Example db.properties

```properties
# PostgreSQL
jdbc.URL=jdbc:postgresql://localhost:5432/dseries
rdbms.userid=dseries_user
rdbms.password=your_password
rdbms.driver=org.postgresql.Driver

# Oracle
jdbc.URL=jdbc:oracle:thin:@localhost:1521:ORCL
rdbms.userid=dseries_user
rdbms.password=your_password
rdbms.driver=oracle.jdbc.driver.OracleDriver

# SQL Server
jdbc.URL=jdbc:sqlserver://localhost:1433;databaseName=dseries
rdbms.userid=dseries_user
rdbms.password=your_password
rdbms.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

## Password Encryption

The tool supports encrypted passwords in `db.properties`:

### Format 1: Base64 Encoding
```properties
rdbms.password=ENC(base64encodedpassword)
```

### Format 2: dSeries Native Encryption
```properties
rdbms.password={ENCRYPTED}hexstring
```

**Note:** For dSeries native encryption, the dSeries encryption library must be in the classpath (automatically included from `lib` directory).

## JDBC Driver Installation

### Option 1: Use Existing dSeries JDBC Driver (Recommended)

If dSeries is already installed and configured with a database, the JDBC driver is likely already present in one of these locations:

- `<install_dir>/lib/`
- `<install_dir>/webserver/lib/`
- `<install_dir>/third-party/`

**No additional action needed** - the launcher scripts will automatically find and use it.

### Option 2: Download JDBC Driver

If the JDBC driver is not present, download it from the database vendor:

#### PostgreSQL
- **Download:** https://jdbc.postgresql.org/download/
- **Recommended:** postgresql-42.5.0.jar or later
- **Place in:** `<install_dir>/lib/` or `<install_dir>/third-party/`

#### Oracle
- **Download:** https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html
- **Recommended:** ojdbc8.jar (for Oracle 12c+)
- **Place in:** `<install_dir>/lib/` or `<install_dir>/third-party/`

#### SQL Server
- **Download:** https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server
- **Recommended:** mssql-jdbc-12.2.0.jre8.jar or later
- **Place in:** `<install_dir>/lib/` or `<install_dir>/third-party/`

### Option 3: Manual Classpath (Advanced)

If you prefer to keep the JDBC driver separate, you can manually specify the classpath:

#### Windows
```cmd
java -cp "C:\path\to\postgresql.jar;dseries-healthcheck.jar" DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4
```

#### Unix/Linux/AIX
```bash
java -cp "/path/to/postgresql.jar:dseries-healthcheck.jar" DSeriesHealthCheck /opt/CA/WA_DE
```

## Troubleshooting

### Error: "JDBC driver not found in classpath"

**Cause:** The JDBC driver JAR is not in any of the scanned directories.

**Solution:**
1. Check if JDBC driver exists in `<install_dir>/lib/` or subdirectories
2. Download the appropriate JDBC driver (see Option 2 above)
3. Place the JAR file in `<install_dir>/lib/`
4. Re-run the health check

### Error: "Could not connect to database"

**Possible Causes:**

1. **Database not running**
   - Start the database service
   - Verify: `psql -h localhost -p 5432 -U dseries_user -d dseries`

2. **Incorrect credentials**
   - Check `<install_dir>/conf/db.properties`
   - Verify username and password
   - Check if password is encrypted

3. **Network/Firewall issues**
   - Verify database host and port are accessible
   - Check firewall rules
   - Test: `telnet <db_host> <db_port>`

4. **Insufficient permissions**
   - Database user must have SELECT permissions on ESP_* tables
   - Grant permissions: `GRANT SELECT ON ALL TABLES IN SCHEMA public TO dseries_user;`

### Error: "Password authentication failed"

**Cause:** Password in `db.properties` is incorrect or encryption is not handled properly.

**Solution:**

1. **Verify password is correct:**
   ```bash
   psql -h localhost -p 5432 -U dseries_user -d dseries
   ```

2. **Check if password is encrypted:**
   - Look for `ENC(...)` or `{ENCRYPTED}...` format
   - Tool automatically decrypts Base64 encoded passwords
   - For dSeries native encryption, ensure encryption library is in classpath

3. **Test with plain text password (temporarily):**
   - Edit `db.properties`
   - Set `rdbms.password=plaintext_password`
   - Run health check
   - **Important:** Re-encrypt password after testing

## Verification

To verify JDBC driver is correctly loaded, run the health check and look for:

```
[DB-CONNECT] Connecting to Database...
  Database connection details:
    Driver: org.postgresql.Driver
    URL: jdbc:***/loc***:5432/dse***
    User: dse***

  Attempting to load JDBC driver: org.postgresql.Driver
  ✅ JDBC driver loaded successfully
  Connecting to database...
  ✅ Database connection established successfully
```

## Database Checks Performed

Once connected, the tool performs these checks:

1. **Table Size Checks**
   - ESP_APPLICATION
   - ESP_WSS_APPL
   - ESP_WSS_JOB
   - ESP_GENERIC_JOB
   - ESP_RTWOB

2. **Data Integrity Checks**
   - Orphaned WSS jobs
   - Orphaned WSS applications
   - Orphaned runtime objects

3. **Performance Checks**
   - Active generations count
   - Runtime object count
   - Job count

4. **Housekeeping Recommendations**
   - Based on table sizes and data integrity issues

## Best Practices

1. **Use Encrypted Passwords**
   - Never store plain text passwords in production
   - Use dSeries encryption utilities or Base64 encoding

2. **Regular JDBC Driver Updates**
   - Keep JDBC drivers up to date for security patches
   - Test new drivers in non-production first

3. **Minimal Permissions**
   - Database user only needs SELECT permissions
   - No INSERT, UPDATE, or DELETE required

4. **Connection Pooling**
   - For frequent health checks, consider connection pooling
   - Current implementation uses single connection

5. **Secure db.properties**
   - Restrict file permissions: `chmod 600 conf/db.properties`
   - Only dSeries service account should have access

## Additional Resources

- [PostgreSQL JDBC Documentation](https://jdbc.postgresql.org/documentation/)
- [Oracle JDBC Documentation](https://docs.oracle.com/en/database/oracle/oracle-database/21/jjdbc/)
- [SQL Server JDBC Documentation](https://learn.microsoft.com/en-us/sql/connect/jdbc/)
- [dSeries Best Practices](https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/ca-workload-automation-de/12-3/installing/ca-workload-automation-de-deployment-best-practices.html)

## Support

For issues or questions:
1. Check this guide first
2. Review error messages carefully
3. Verify database connectivity independently
4. Check dSeries logs: `<install_dir>/log/`
5. Contact Broadcom Support with:
   - Error messages
   - `db.properties` (with passwords redacted)
   - Health check output
   - Database type and version
