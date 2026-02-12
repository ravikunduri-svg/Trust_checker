# dSeries Health Check - Database Configuration Guide

**Version:** 2.1.0  
**Date:** February 12, 2026  
**Status:** Production Ready

---

## 🎯 Overview

The Enhanced dSeries Health Check Tool now uses the **existing dSeries database configuration** (`conf/db.properties`) by default, eliminating the need for duplicate configuration and leveraging existing security measures.

---

## 🏗️ Design Decision: Use Existing db.properties

### ✅ Why This Approach is Better

| Aspect | Using Existing db.properties | Separate Config File |
|--------|----------------------------|---------------------|
| **Configuration** | ✅ Single source of truth | ❌ Duplicate config |
| **Maintenance** | ✅ One file to manage | ❌ Two files to sync |
| **Security** | ✅ Leverages existing encryption | ❌ Need separate encryption |
| **Consistency** | ✅ Always matches dSeries | ❌ Can drift out of sync |
| **Simplicity** | ✅ No extra setup | ❌ Additional file to create |
| **Integration** | ✅ Fully aligned with dSeries | ❌ Separate from dSeries |

**Decision:** ✅ **Use existing `installDir/conf/db.properties`**

---

## 📁 How It Works

### Automatic Configuration Loading

```
┌─────────────────────────────────────────────────────────────┐
│  Command Line                                               │
│  java DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4    │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Health Check Tool                                          │
│  installDir = C:\CA\ESPdSeriesWAServer_R12_4               │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Automatically Reads                                        │
│  C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties         │
│                                                             │
│  Content:                                                   │
│    jdbc.URL=jdbc:postgresql://localhost:5432/dseries       │
│    rdbms.userid=dseries_user                               │
│    rdbms.password=ENC(base64encryptedpassword)             │
│    rdbms.driver=org.postgresql.Driver                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Password Decryption                                        │
│  • Detects encryption format                               │
│  • Decrypts password                                        │
│  • Uses for database connection                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│  Database Connection                                        │
│  • Connect using decrypted credentials                      │
│  • Execute health check queries                            │
│  • Generate report                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔐 Password Encryption Support

### Supported Formats

The tool supports multiple password formats:

#### 1. Plain Text (Not Recommended for Production)

```properties
rdbms.password=mypassword
```

**Status:** ⚠️ Works but not secure  
**Use Case:** Development/testing only

#### 2. Base64 Encrypted (Common)

```properties
rdbms.password=ENC(bXlwYXNzd29yZA==)
```

**Status:** ✅ Supported with built-in decryption  
**Format:** `ENC(base64_encoded_password)`  
**Use Case:** Standard dSeries encryption

#### 3. Advanced Encryption (Enterprise)

```properties
rdbms.password={ENCRYPTED}alonghexstring...
```

**Status:** ℹ️ Requires dSeries encryption library  
**Format:** `{ENCRYPTED}hex_string`  
**Use Case:** Enterprise installations with advanced security

---

## 🔧 Password Encryption/Decryption

### How Decryption Works

```java
/**
 * Decrypt password flow
 */
 
1. Read password from db.properties
   encryptedPassword = "ENC(bXlwYXNzd29yZA==)"

2. Detect encryption format
   if (encryptedPassword.startsWith("ENC("))
      → Base64 format detected

3. Extract encrypted value
   encrypted = "bXlwYXNzd29yZA=="

4. Decode Base64
   decodedBytes = Base64.decode(encrypted)
   password = new String(decodedBytes)
   → "mypassword"

5. Use for connection
   connection = DriverManager.getConnection(url, user, password)
```

### Encryption Methods

#### Method 1: Base64 Encoding (Built-in)

```bash
# Encode password
echo -n "mypassword" | base64
# Result: bXlwYXNzd29yZA==

# Add to db.properties
rdbms.password=ENC(bXlwYXNzd29yZA==)
```

#### Method 2: Using dSeries Encryption Tool

```bash
# If dSeries provides encryption utility
cd $INSTALL_DIR/bin
./encryptPassword.sh mypassword

# Or use Java directly
java -cp $INSTALL_DIR/lib/* com.ca.wa.de.security.Encryption mypassword
```

#### Method 3: PowerShell (Windows)

```powershell
# Encode password to Base64
$password = "mypassword"
$bytes = [System.Text.Encoding]::UTF8.GetBytes($password)
$encoded = [Convert]::ToBase64String($bytes)
Write-Host "ENC($encoded)"
# Result: ENC(bXlwYXNzd29yZA==)
```

---

## 📝 Configuration Examples

### Example 1: PostgreSQL with Plain Text (Development)

```properties
# C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties
jdbc.URL=jdbc:postgresql://localhost:5432/dseries
rdbms.userid=dseries_user
rdbms.password=mypassword
rdbms.driver=org.postgresql.Driver
rdbms.schema=public
```

**Usage:**
```bash
java DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4
```

---

### Example 2: PostgreSQL with Encrypted Password (Production)

```properties
# C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties
jdbc.URL=jdbc:postgresql://dbserver:5432/dseries_prod
rdbms.userid=dseries_user
rdbms.password=ENC(bXlwYXNzd29yZA==)
rdbms.driver=org.postgresql.Driver
rdbms.schema=public
```

**Usage:**
```bash
java DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4
```

**Note:** Tool automatically detects and decrypts password

---

### Example 3: Oracle with Encrypted Password

```properties
# C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties
jdbc.URL=jdbc:oracle:thin:@dbserver:1521:orcl
rdbms.userid=dseries_user
rdbms.password=ENC(ZHNlcmllc19wYXNzd29yZA==)
rdbms.driver=oracle.jdbc.driver.OracleDriver
rdbms.schema=DSERIES
```

**Usage:**
```bash
java -cp ojdbc8.jar;. DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4
```

---

### Example 4: SQL Server with Encrypted Password

```properties
# C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties
jdbc.URL=jdbc:sqlserver://dbserver:1433;databaseName=dseries
rdbms.userid=dseries_user
rdbms.password=ENC(U1FMU2VydmVyUGFzc3dvcmQ=)
rdbms.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
rdbms.schema=dbo
```

**Usage:**
```bash
java -cp mssql-jdbc-9.4.0.jar;. DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4
```

---

## 🔒 Security Best Practices

### 1. Encrypt Passwords in Production

```properties
# ❌ BAD - Plain text password
rdbms.password=MySecretPassword123

# ✅ GOOD - Encrypted password
rdbms.password=ENC(TXlTZWNyZXRQYXNzd29yZDEyMw==)
```

### 2. Restrict File Permissions

```bash
# Linux/Unix
chmod 600 /opt/CA/WA_DE/conf/db.properties
chown dseries:dseries /opt/CA/WA_DE/conf/db.properties

# Windows (PowerShell as Administrator)
icacls "C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties" /inheritance:r
icacls "C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties" /grant:r "SYSTEM:(R)"
icacls "C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties" /grant:r "Administrators:(R)"
```

### 3. Use Read-Only Database Account

```sql
-- PostgreSQL
CREATE USER dseries_readonly WITH PASSWORD 'encrypted_password';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO dseries_readonly;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO dseries_readonly;

-- Oracle
CREATE USER dseries_readonly IDENTIFIED BY encrypted_password;
GRANT SELECT ANY TABLE TO dseries_readonly;

-- SQL Server
CREATE LOGIN dseries_readonly WITH PASSWORD = 'encrypted_password';
CREATE USER dseries_readonly FOR LOGIN dseries_readonly;
EXEC sp_addrolemember 'db_datareader', 'dseries_readonly';
```

### 4. Enable SSL/TLS for Database Connections

```properties
# PostgreSQL with SSL
jdbc.URL=jdbc:postgresql://dbserver:5432/dseries?ssl=true&sslmode=require

# Oracle with SSL
jdbc.URL=jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=dbserver)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=orcl)))

# SQL Server with Encryption
jdbc.URL=jdbc:sqlserver://dbserver:1433;databaseName=dseries;encrypt=true;trustServerCertificate=false
```

---

## 🚀 Usage

### Basic Usage (Recommended)

```bash
# Uses installDir/conf/db.properties automatically
java DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4
```

**What happens:**
1. Reads `C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties`
2. Detects password encryption format
3. Decrypts password automatically
4. Connects to database
5. Runs all health checks

### With Custom SQL Queries

```bash
# Uses installDir/conf/db.properties + custom queries
java DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4 custom_queries.sql
```

### With External db.properties (Testing Only)

```bash
# For testing with a different database configuration
java DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4 /path/to/test_db.properties
```

### With JDBC Driver

```bash
# PostgreSQL
java -cp postgresql-42.5.0.jar;. DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4

# Oracle
java -cp ojdbc8.jar;. DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4

# SQL Server
java -cp mssql-jdbc-9.4.0.jar;. DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4
```

---

## 🔍 Troubleshooting

### Issue 1: Password Decryption Failed

**Symptom:**
```
⚠️  Password appears encrypted but could not decrypt
Note: Advanced encryption requires dSeries encryption library
```

**Solutions:**

1. **Check encryption format:**
   ```bash
   # Ensure password is in correct format
   # Valid: ENC(base64string)
   # Valid: {ENCRYPTED}hexstring
   ```

2. **For advanced encryption:**
   ```bash
   # Add dSeries encryption library to classpath
   java -cp $INSTALL_DIR/lib/*:. DSeriesHealthCheck $INSTALL_DIR
   ```

3. **Test with plain text (temporarily):**
   ```properties
   # For testing, use plain text
   rdbms.password=test_password
   # Then re-encrypt for production
   ```

---

### Issue 2: Database Connection Failed

**Symptom:**
```
⚠️  Could not connect to database: Access denied
```

**Solutions:**

1. **Verify credentials:**
   ```bash
   # Test database connection directly
   psql -h localhost -p 5432 -U dseries_user -d dseries
   ```

2. **Check password decryption:**
   ```bash
   # Temporarily use plain text to verify connection works
   # Then investigate encryption/decryption
   ```

3. **Verify network access:**
   ```bash
   # Test port connectivity
   telnet localhost 5432
   # Or
   Test-NetConnection -ComputerName localhost -Port 5432
   ```

---

### Issue 3: JDBC Driver Not Found

**Symptom:**
```
⚠️  JDBC driver not found: org.postgresql.Driver
```

**Solutions:**

1. **Add JDBC driver to classpath:**
   ```bash
   java -cp postgresql-42.5.0.jar;. DSeriesHealthCheck $INSTALL_DIR
   ```

2. **Download JDBC driver:**
   - PostgreSQL: https://jdbc.postgresql.org/download/
   - Oracle: https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html
   - SQL Server: https://docs.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server

---

## 📊 Comparison: Old vs New Approach

### Old Approach (Separate Config)

```
Health Check Tool
├── config/
│   ├── db.properties (separate config)
│   └── health_check_queries.sql
│
dSeries Installation
├── conf/
│   └── db.properties (dSeries config)

Issues:
❌ Two db.properties files to maintain
❌ Can get out of sync
❌ Different encryption mechanisms
❌ Extra configuration overhead
```

### New Approach (Use Existing Config) ✅

```
Health Check Tool
├── config/
│   └── health_check_queries.sql (only SQL queries)
│
dSeries Installation
├── conf/
│   └── db.properties (single source of truth)

Benefits:
✅ Single db.properties file
✅ Always in sync with dSeries
✅ Uses dSeries encryption
✅ No duplicate configuration
✅ Simpler to manage
```

---

## 📚 Advanced Topics

### Custom Encryption Integration

If your organization uses custom encryption:

```java
// Add custom decryption in DSeriesHealthCheck.java
private static String decryptPassword(String encryptedPassword) {
    if (encryptedPassword.startsWith("CUSTOM(")) {
        // Your custom decryption logic here
        return CustomEncryption.decrypt(encryptedPassword);
    }
    // ... existing logic
}
```

### Environment-Specific Configuration

For different environments:

```bash
# Development
java DSeriesHealthCheck C:\CA\dSeries_Dev

# Uses: C:\CA\dSeries_Dev\conf\db.properties
# Points to: jdbc:postgresql://dev-db:5432/dseries_dev

# Production
java DSeriesHealthCheck C:\CA\dSeries_Prod

# Uses: C:\CA\dSeries_Prod\conf\db.properties
# Points to: jdbc:postgresql://prod-db:5432/dseries_prod
```

---

## ✅ Summary

### Key Improvements

✅ **Single Configuration** - Uses existing dSeries db.properties  
✅ **Automatic Password Decryption** - Supports multiple encryption formats  
✅ **No Duplicate Config** - Eliminates maintenance overhead  
✅ **Consistent with dSeries** - Aligned with dSeries security  
✅ **Simpler Usage** - No extra configuration needed  
✅ **Better Security** - Leverages existing encryption  

### Usage Summary

```bash
# Simple - just provide installation directory
java DSeriesHealthCheck C:\CA\ESPdSeriesWAServer_R12_4

# Tool automatically:
# 1. Reads C:\CA\ESPdSeriesWAServer_R12_4\conf\db.properties
# 2. Decrypts password if encrypted
# 3. Connects to database
# 4. Runs health checks
```

### Security Summary

✅ Supports plain text passwords (development)  
✅ Supports Base64 encrypted passwords (production)  
✅ Supports advanced encryption (with library)  
✅ Automatically masks passwords in output  
✅ Uses read-only database access  

---

**Version:** 2.1.0  
**Last Updated:** February 12, 2026  
**Copyright © 2026 Broadcom. All Rights Reserved.**
