# Universal Database Support Guide - dSeries Health Check v2.3.0

## Overview

Version 2.3.0 introduces **universal database support** with automatic JDBC driver loading, native dSeries encryption/decryption, and Windows authentication support for all major database platforms.

## Key Features

### 1. Dynamic Classpath Loading

The tool now uses the dSeries native `bin/classpath.bat` (Windows) or `bin/classpath.sh` (Unix/Linux/AIX) to automatically load:

- ✅ **All JDBC Drivers** - PostgreSQL, Oracle, DB2, SQL Server, and more
- ✅ **Encryption Libraries** - dSeries native encryption (wade.jar, bc-fips.jar)
- ✅ **Authentication DLLs** - Windows authentication libraries (sqljdbc_auth.dll)
- ✅ **All dSeries Dependencies** - Complete classpath matching dSeries server

### 2. Native dSeries Password Encryption

Automatically uses dSeries encryption library (`com.ca.wa.de.security.Encryption`) to decrypt passwords in `db.properties`:

- ✅ **Base64 Format** (no prefix) - dSeries default: `4CzoT8OQjaDxtgMnFtRU2w==`
- ✅ **ENC Format** - Simple Base64: `ENC(base64string)`
- ✅ **ENCRYPTED Format** - Alternative: `{ENCRYPTED}hexstring`
- ✅ **Plain Text** - Legacy support (not recommended)

### 3. Universal Database Support

| Database | JDBC Driver | Default Port | Windows Auth | Status |
|----------|-------------|--------------|--------------|--------|
| **PostgreSQL** | `org.postgresql.Driver` | 5432 | N/A | ✅ Fully Supported |
| **Oracle** | `oracle.jdbc.driver.OracleDriver` | 1521 | N/A | ✅ Fully Supported |
| **SQL Server** | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | 1433 | ✅ Yes | ✅ Fully Supported |
| **DB2** | `com.ibm.db2.jcc.DB2Driver` | 50000 | N/A | ✅ Fully Supported |
| **MySQL** | `com.mysql.cj.jdbc.Driver` | 3306 | N/A | ✅ Supported |
| **MariaDB** | `org.mariadb.jdbc.Driver` | 3306 | N/A | ✅ Supported |

### 4. Windows Authentication Support

For SQL Server with Windows authentication:

```properties
# db.properties
jdbc.URL=jdbc:sqlserver://hostname:1433;databaseName=dseries;integratedSecurity=true
rdbms.userid=
rdbms.password=
rdbms.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

The tool automatically:
- Detects `integratedSecurity=true` in JDBC URL
- Adds `sqljdbc_auth.dll` to `java.library.path`
- Sets up native library paths (PATH, LD_LIBRARY_PATH, LIBPATH)

## How It Works

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  dSeries Health Check Tool (v2.3.0)                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ├─► Execute bin/classpath.bat or .sh
                     │   └─► Get complete dSeries classpath
                     │
                     ├─► Load dSeries encryption library
                     │   └─► com.ca.wa.de.security.Encryption
                     │
                     ├─► Read conf/db.properties
                     │   ├─► Detect database type
                     │   ├─► Parse JDBC URL
                     │   └─► Decrypt password using dSeries encryption
                     │
                     ├─► Setup native libraries
                     │   ├─► Add bin/ to PATH/LD_LIBRARY_PATH
                     │   └─► Set java.library.path
                     │
                     ├─► Load JDBC driver
                     │   └─► From dSeries classpath
                     │
                     └─► Connect to database
                         └─► Run health checks
```

### Execution Flow

1. **Launcher Script Execution**
   ```bash
   # Windows
   dseries_healthcheck.bat C:\CA\ESPdSeriesWAServer_R12_4
   
   # Unix/Linux/AIX
   ./dseries_healthcheck.sh /opt/CA/WA_DE
   ```

2. **Dynamic Classpath Loading**
   ```
   [3/6] Building classpath...
     Using dSeries native classpath.bat...
     OK: dSeries classpath loaded successfully
     ℹ️  Includes all JDBC drivers, encryption libs, and auth DLLs
   ```

3. **Database Configuration**
   ```
   [DB-CONFIG] Loading Database Configuration...
     Using dSeries database config: C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties
     ℹ️  Detected database type: POSTGRESQL
     ℹ️  Using dSeries native encryption library for password decryption
     ✅ Password decrypted successfully using dSeries encryption
     ✅ Database configuration loaded
   ```

4. **Database Connection**
   ```
   [DB-CONNECT] Connecting to Database...
     Database connection details:
       Driver: org.postgresql.Driver
       URL: jdbc:***/G36***:5432/dse***
       User: pos***
   
     Attempting to load JDBC driver: org.postgresql.Driver
     ✅ JDBC driver loaded successfully
     Connecting to database...
     ✅ Database connection established successfully
   ```

## Configuration Examples

### PostgreSQL (Embedded or External)

```properties
# conf/db.properties
jdbc.Driver=org.postgresql.Driver
jdbc.URL=jdbc:postgresql://localhost:5432/dseries
rdbms.type=PostgreSQL
rdbms.userid=postgres
rdbms.password=4CzoT8OQjaDxtgMnFtRU2w==  # dSeries encrypted
```

### Oracle Database

```properties
# conf/db.properties
jdbc.Driver=oracle.jdbc.driver.OracleDriver
jdbc.URL=jdbc:oracle:thin:@hostname:1521:ORCL
rdbms.type=Oracle
rdbms.userid=dseries_user
rdbms.password=ENC(base64encodedpassword)
```

### SQL Server (Username/Password)

```properties
# conf/db.properties
jdbc.Driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
jdbc.URL=jdbc:sqlserver://hostname:1433;databaseName=dseries
rdbms.type=SQLServer
rdbms.userid=dseries_user
rdbms.password=4CzoT8OQjaDxtgMnFtRU2w==
```

### SQL Server (Windows Authentication)

```properties
# conf/db.properties
jdbc.Driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
jdbc.URL=jdbc:sqlserver://hostname:1433;databaseName=dseries;integratedSecurity=true
rdbms.type=SQLServer
rdbms.userid=
rdbms.password=
```

**Requirements:**
- `sqljdbc_auth.dll` must be in `<install_dir>/bin/` directory
- Windows service account must have database access
- Tool automatically sets up `java.library.path`

### DB2

```properties
# conf/db.properties
jdbc.Driver=com.ibm.db2.jcc.DB2Driver
jdbc.URL=jdbc:db2://hostname:50000/dseries
rdbms.type=DB2
rdbms.userid=dseries_user
rdbms.password=4CzoT8OQjaDxtgMnFtRU2w==
```

## Password Encryption

### Using dSeries Native Encryption

The tool automatically uses dSeries encryption when `wade.jar` is in the classpath (loaded from `bin/classpath.bat`).

**Encryption Method:**
```java
// Automatic via dSeries encryption library
com.ca.wa.de.security.Encryption.decrypt(encryptedPassword)
```

**Supported Formats:**
1. **Base64 (no prefix)** - dSeries default
   ```
   4CzoT8OQjaDxtgMnFtRU2w==
   ```

2. **ENC Format**
   ```
   ENC(base64encodedpassword)
   ```

3. **ENCRYPTED Format**
   ```
   {ENCRYPTED}hexstring
   ```

### Encrypting Passwords

To encrypt passwords for `db.properties`, use dSeries encryption utilities:

```bash
# Using dSeries encryption tool (if available)
cd <install_dir>/bin
./encrypt_password.sh "mypassword"

# Or use dSeries Manager to encrypt passwords
```

## Native Library Support

### Windows

**DLL Files Supported:**
- `sqljdbc_auth.dll` - SQL Server Windows authentication
- Other database-specific authentication DLLs

**Automatic Setup:**
```batch
REM Launcher script automatically adds to PATH
set "PATH=%DSERIES_HOME%\bin;%PATH%"

REM And sets java.library.path
set "JAVA_OPTS=-Djava.library.path=%DSERIES_HOME%\bin"
```

### Unix/Linux

**Library Path Setup:**
```bash
# Linux
export LD_LIBRARY_PATH="${DSERIES_HOME}/bin:${LD_LIBRARY_PATH}"

# AIX
export LIBPATH="${DSERIES_HOME}/bin:${LIBPATH}"

# HP-UX
export SHLIB_PATH="${DSERIES_HOME}/bin:${SHLIB_PATH}"
```

## Troubleshooting

### Issue 1: JDBC Driver Not Found

**Error:**
```
⚠️  JDBC driver not found in classpath: org.postgresql.Driver
```

**Solution:**
1. Ensure you're using the launcher scripts (`.bat` or `.sh`)
2. Verify `bin/classpath.bat` exists and is executable
3. Check that JDBC driver JAR is in `<install_dir>/lib/`

### Issue 2: Password Decryption Failed

**Error:**
```
⚠️  dSeries decryption failed: ...
```

**Solution:**
1. Verify `wade.jar` is in classpath (check `bin/classpath.bat`)
2. Ensure password is properly encrypted using dSeries encryption
3. Try plain text password temporarily for testing

### Issue 3: Windows Authentication Failed

**Error:**
```
This driver is not configured for integrated authentication
```

**Solution:**
1. Verify `sqljdbc_auth.dll` is in `<install_dir>/bin/`
2. Check DLL architecture matches Java (x64 vs x86)
3. Ensure Windows service account has database access
4. Verify `integratedSecurity=true` in JDBC URL

### Issue 4: DB2 Connection Failed

**Error:**
```
com.ibm.db2.jcc.DB2Driver not found
```

**Solution:**
1. Ensure DB2 JDBC driver (`db2jcc4.jar`) is in `<install_dir>/lib/`
2. Verify `bin/classpath.bat` includes the DB2 driver
3. Check DB2 license file (`db2jcc_license_cu.jar`) is also present

## Best Practices

### 1. Use dSeries Native Classpath

✅ **DO:**
```bash
# Let the tool use dSeries classpath.bat
dseries_healthcheck.bat C:\CA\ESPdSeriesWAServer_R12_4
```

❌ **DON'T:**
```bash
# Don't manually build classpath
java -cp "lib/*" DSeriesHealthCheck ...
```

### 2. Use dSeries Encryption

✅ **DO:**
```properties
# Use dSeries encrypted passwords
rdbms.password=4CzoT8OQjaDxtgMnFtRU2w==
```

❌ **DON'T:**
```properties
# Don't use plain text in production
rdbms.password=mypassword
```

### 3. Windows Authentication

✅ **DO:**
```properties
# Use Windows authentication when possible
jdbc.URL=jdbc:sqlserver://...;integratedSecurity=true
rdbms.userid=
rdbms.password=
```

**Benefits:**
- No password management
- Centralized AD authentication
- Better security audit trail

### 4. Database-Specific Tuning

**PostgreSQL:**
```properties
# Add connection pool settings
jdbc.URL=jdbc:postgresql://host:5432/dseries?currentSchema=public&ssl=true
```

**Oracle:**
```properties
# Add service name instead of SID
jdbc.URL=jdbc:oracle:thin:@host:1521/service_name
```

**SQL Server:**
```properties
# Add encryption and trust settings
jdbc.URL=jdbc:sqlserver://host:1433;databaseName=dseries;encrypt=true;trustServerCertificate=true
```

**DB2:**
```properties
# Add schema and security settings
jdbc.URL=jdbc:db2://host:50000/dseries:currentSchema=DSERIES;
```

## Version Compatibility

| dSeries Version | Health Check Version | Database Support |
|----------------|---------------------|------------------|
| 12.0 - 12.3 | v2.3.0+ | All databases |
| 12.4+ | v2.3.0+ | All databases |
| 25.0+ | v2.3.0+ | All databases |

## Security Considerations

### 1. Password Encryption

- ✅ Always use dSeries encryption for passwords
- ✅ Never commit plain text passwords to version control
- ✅ Restrict `db.properties` file permissions (`chmod 600`)

### 2. Windows Authentication

- ✅ Use Windows authentication when possible
- ✅ Ensure service accounts have minimal required permissions
- ✅ Audit database access regularly

### 3. JDBC Driver Security

- ✅ Keep JDBC drivers up to date for security patches
- ✅ Download drivers only from official vendor sources
- ✅ Verify JAR file checksums

### 4. Network Security

- ✅ Use SSL/TLS for database connections when possible
- ✅ Restrict database access to specific IP addresses
- ✅ Use firewalls to limit database port access

## Performance Optimization

### 1. Connection Pooling

For frequent health checks, consider connection pooling:

```properties
# PostgreSQL
jdbc.URL=jdbc:postgresql://host:5432/dseries?maxPoolSize=10

# Oracle
jdbc.URL=jdbc:oracle:thin:@host:1521:ORCL?oracle.jdbc.implicitStatementCacheSize=25
```

### 2. Query Timeout

Add timeout settings to prevent hanging:

```properties
# SQL Server
jdbc.URL=jdbc:sqlserver://host:1433;databaseName=dseries;loginTimeout=30;queryTimeout=60
```

### 3. Read-Only Access

Use read-only database user for health checks:

```sql
-- PostgreSQL
CREATE USER healthcheck_user WITH PASSWORD 'encrypted_password';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO healthcheck_user;

-- Oracle
CREATE USER healthcheck_user IDENTIFIED BY encrypted_password;
GRANT SELECT ANY TABLE TO healthcheck_user;

-- SQL Server
CREATE LOGIN healthcheck_user WITH PASSWORD = 'encrypted_password';
CREATE USER healthcheck_user FOR LOGIN healthcheck_user;
GRANT SELECT TO healthcheck_user;

-- DB2
CREATE USER healthcheck_user IDENTIFIED BY encrypted_password;
GRANT SELECT ON SCHEMA DSERIES TO USER healthcheck_user;
```

## Additional Resources

- [dSeries Best Practices](https://techdocs.broadcom.com/us/en/ca-enterprise-software/intelligent-automation/ca-workload-automation-de/12-3/installing/ca-workload-automation-de-deployment-best-practices.html)
- [PostgreSQL JDBC Documentation](https://jdbc.postgresql.org/documentation/)
- [Oracle JDBC Documentation](https://docs.oracle.com/en/database/oracle/oracle-database/21/jjdbc/)
- [SQL Server JDBC Documentation](https://learn.microsoft.com/en-us/sql/connect/jdbc/)
- [DB2 JDBC Documentation](https://www.ibm.com/docs/en/db2/11.5?topic=cdsudidsdjs-db2-jdbc-driver-versions)

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
   - JDBC driver version
