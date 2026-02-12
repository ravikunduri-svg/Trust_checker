# SSL/TLS Certificate and Security Guide - dSeries Health Check v2.4.0

## Overview

Version 2.4.0 introduces comprehensive SSL/TLS certificate support, connection validation, query timeouts, and enhanced security features based on dSeries native implementation patterns.

## Key Features

### 1. SSL/TLS Certificate Support

The tool now automatically handles SSL/TLS certificates for secure database connections:

- ✅ **TrustStore Configuration** - javax.net.ssl.trustStore
- ✅ **TrustStore Password** - javax.net.ssl.trustStorePassword (encrypted)
- ✅ **System Property Setup** - Automatic configuration for DB2 and Oracle
- ✅ **Password Decryption** - dSeries native encryption support
- ✅ **AIX+Oracle SSL** - Special handling for AIX with Oracle SSL

### 2. Connection Validation

- ✅ **isValid() Check** - 30-second timeout validation
- ✅ **Connection Timing** - Performance monitoring
- ✅ **Retry Logic** - 3 attempts with 2-second delay
- ✅ **Proper Error Handling** - Detailed troubleshooting messages

### 3. Query Timeout and Performance

- ✅ **Query Timeout** - 30-second timeout per query
- ✅ **Performance Monitoring** - Query execution time tracking
- ✅ **Slow Query Detection** - Warnings for queries > 1 second
- ✅ **SQLTimeoutException Handling** - Graceful timeout handling

### 4. Resource Cleanup

- ✅ **Finally Blocks** - Proper resource cleanup
- ✅ **ResultSet Cleanup** - Automatic closing
- ✅ **Statement Cleanup** - Automatic closing
- ✅ **Connection Cleanup** - Proper connection management

## Configuration

### SSL/TLS in db.properties

```properties
# PostgreSQL with SSL
jdbc.Driver=org.postgresql.Driver
jdbc.URL=jdbc:postgresql://hostname:5432/dseries?ssl=true
rdbms.userid=dseries_user
rdbms.password=4CzoT8OQjaDxtgMnFtRU2w==
javax.net.ssl.trustStore=/path/to/truststore.jks
javax.net.ssl.trustStorePassword=ENC(base64password)

# Oracle with SSL
jdbc.Driver=oracle.jdbc.driver.OracleDriver
jdbc.URL=jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=hostname)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=orcl)))
rdbms.userid=dseries_user
rdbms.password=4CzoT8OQjaDxtgMnFtRU2w==
javax.net.ssl.trustStore=/path/to/truststore.jks
javax.net.ssl.trustStorePassword=ENC(base64password)

# SQL Server with Encryption
jdbc.Driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
jdbc.URL=jdbc:sqlserver://hostname:1433;databaseName=dseries;encrypt=true;trustServerCertificate=false
rdbms.userid=dseries_user
rdbms.password=4CzoT8OQjaDxtgMnFtRU2w==
javax.net.ssl.trustStore=/path/to/truststore.jks
javax.net.ssl.trustStorePassword=ENC(base64password)

# DB2 with SSL
jdbc.Driver=com.ibm.db2.jcc.DB2Driver
jdbc.URL=jdbc:db2://hostname:50001/dseries:sslConnection=true;
rdbms.userid=dseries_user
rdbms.password=4CzoT8OQjaDxtgMnFtRU2w==
javax.net.ssl.trustStore=/path/to/truststore.jks
javax.net.ssl.trustStorePassword=ENC(base64password)
```

### AIX + Oracle SSL Special Configuration

For AIX servers connecting to Oracle with SSL:

```properties
jdbc.Driver=oracle.jdbc.driver.OracleDriver
jdbc.URL=jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=hostname)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=orcl)))
rdbms.userid=dseries_user
rdbms.password=4CzoT8OQjaDxtgMnFtRU2w==
javax.net.ssl.trustStore=/path/to/truststore.jks
javax.net.ssl.trustStorePassword=ENC(base64password)
oracle.net.ssl_client_authentication=false
oracle.net.ssl_version=1.2
```

## How It Works

### SSL/TLS Certificate Loading

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Load db.properties                                          │
│     - Read javax.net.ssl.trustStore                             │
│     - Read javax.net.ssl.trustStorePassword (encrypted)         │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. Decrypt TrustStore Password                                 │
│     - Use dSeries native encryption (Scrambler)                 │
│     - Fallback to Base64 if needed                              │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. Set System Properties                                       │
│     - System.setProperty("javax.net.ssl.trustStore", ...)       │
│     - System.setProperty("javax.net.ssl.trustStorePassword",...) │
│     - Required for DB2 and some Oracle configurations           │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. Add to Connection Properties                                │
│     - connectionProps.setProperty("javax.net.ssl.trustStore",...) │
│     - connectionProps.setProperty("javax.net.ssl.trustStorePassword",...) │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. Establish Connection                                        │
│     - DriverManager.getConnection(jdbcUrl, connectionProps)     │
│     - SSL/TLS handshake automatic                               │
└─────────────────────────────────────────────────────────────────┘
```

### Connection Validation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Attempt Connection (with retry)                             │
│     - Max 3 attempts                                            │
│     - 2-second delay between attempts                           │
│     - Track connection time                                     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. Validate Connection                                         │
│     - connection.isValid(30)                                    │
│     - 30-second timeout                                         │
│     - Returns true if connection is valid                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. Report Connection Time                                      │
│     - Display milliseconds taken                                │
│     - Warn if > 5 seconds                                       │
└─────────────────────────────────────────────────────────────────┘
```

### Query Execution with Timeout

```
┌─────────────────────────────────────────────────────────────────┐
│  1. Create Statement                                            │
│     - stmt = connection.createStatement()                       │
│     - stmt.setQueryTimeout(30)                                  │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. Execute Query (with timing)                                 │
│     - Start timer                                               │
│     - rs = stmt.executeQuery(query)                             │
│     - End timer                                                 │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. Handle Results                                              │
│     - Process result set                                        │
│     - Warn if query took > 1 second                             │
│     - SQLTimeoutException if > 30 seconds                       │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. Cleanup (finally block)                                     │
│     - rs.close()                                                │
│     - stmt.close()                                              │
│     - Guaranteed execution                                      │
└─────────────────────────────────────────────────────────────────┘
```

## Security Best Practices

### 1. Certificate Management

✅ **DO:**
- Use proper SSL/TLS certificates from trusted CA
- Store certificates in secure keystore/truststore
- Encrypt truststore passwords using dSeries encryption
- Restrict truststore file permissions (`chmod 600`)

❌ **DON'T:**
- Use self-signed certificates in production
- Store plain text truststore passwords
- Share truststore files across environments
- Commit truststore files to version control

### 2. Password Encryption

✅ **DO:**
```properties
# Use dSeries encrypted password
javax.net.ssl.trustStorePassword=4CzoT8OQjaDxtgMnFtRU2w==

# Or Base64 encoded
javax.net.ssl.trustStorePassword=ENC(base64string)
```

❌ **DON'T:**
```properties
# Never use plain text
javax.net.ssl.trustStorePassword=mypassword
```

### 3. Connection Security

✅ **DO:**
- Enable SSL/TLS for all database connections
- Use certificate validation (don't trust all certificates)
- Configure proper cipher suites
- Use TLS 1.2 or higher

❌ **DON'T:**
- Disable SSL verification (`trustServerCertificate=true`)
- Use weak cipher suites
- Allow TLS 1.0 or 1.1

## Troubleshooting

### Issue 1: SSL Handshake Failed

**Error:**
```
javax.net.ssl.SSLHandshakeException: PKIX path building failed
```

**Solution:**
1. Verify truststore path is correct
2. Check certificate is imported in truststore:
   ```bash
   keytool -list -v -keystore /path/to/truststore.jks
   ```
3. Import server certificate if missing:
   ```bash
   keytool -import -alias dbserver -file server.crt -keystore truststore.jks
   ```

### Issue 2: TrustStore Password Decryption Failed

**Error:**
```
⚠️  TrustStore password decryption failed
```

**Solution:**
1. Verify password is encrypted using dSeries encryption
2. Check encryption library (wade.jar) is in classpath
3. Test decryption manually:
   ```bash
   cd <install_dir>/bin
   ./encrypt_password.sh "password"
   ```

### Issue 3: Connection Validation Timeout

**Error:**
```
⚠️  Connection validation failed or timed out
```

**Solution:**
1. Check database is running and accessible
2. Verify network connectivity
3. Check firewall rules
4. Increase validation timeout if needed (currently 30 seconds)

### Issue 4: Query Timeout

**Error:**
```
⚠️  Query timeout (> 30 seconds): Check Name
Database may be slow or query is inefficient
```

**Solution:**
1. Optimize the query
2. Check database performance
3. Run database maintenance (VACUUM, ANALYZE, etc.)
4. Check for table locks
5. Review query execution plan

## Performance Optimization

### 1. Connection Pooling

For frequent health checks, consider connection pooling:

```properties
# Add to db.properties (if supported by driver)
database.minconnection=3
database.maxconnections.in.pool=10
database.timeout=30
```

### 2. Query Optimization

Monitor slow queries and optimize:

```
[DB-001] ESP_APPLICATION Table Size
  ✅ ESP_APPLICATION Table Size: 50000 (OK)
  ⚠️  Query took 1250 ms (slow)
```

**Actions:**
- Add indexes on frequently queried columns
- Update table statistics
- Consider partitioning large tables

### 3. SSL/TLS Performance

SSL/TLS adds overhead. Optimize by:

- Using hardware SSL acceleration if available
- Enabling SSL session caching
- Using appropriate cipher suites (AES-GCM preferred)

## Database-Specific SSL Configuration

### PostgreSQL SSL

```properties
jdbc.URL=jdbc:postgresql://hostname:5432/dseries?ssl=true&sslmode=verify-full
javax.net.ssl.trustStore=/path/to/truststore.jks
javax.net.ssl.trustStorePassword=ENC(password)
```

**SSL Modes:**
- `disable` - No SSL
- `allow` - Try SSL, fallback to non-SSL
- `prefer` - Try SSL first, fallback to non-SSL
- `require` - Require SSL, no certificate validation
- `verify-ca` - Require SSL, validate CA
- `verify-full` - Require SSL, validate CA and hostname

### Oracle SSL

```properties
jdbc.URL=jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCPS)(HOST=hostname)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=orcl)))
javax.net.ssl.trustStore=/path/to/truststore.jks
javax.net.ssl.trustStorePassword=ENC(password)
oracle.net.ssl_client_authentication=false
oracle.net.ssl_version=1.2
```

### SQL Server SSL

```properties
jdbc.URL=jdbc:sqlserver://hostname:1433;databaseName=dseries;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net
javax.net.ssl.trustStore=/path/to/truststore.jks
javax.net.ssl.trustStorePassword=ENC(password)
```

### DB2 SSL

```properties
jdbc.URL=jdbc:db2://hostname:50001/dseries:sslConnection=true;sslTrustStoreLocation=/path/to/truststore.jks;sslTrustStorePassword=password;
```

**Note:** DB2 requires system properties to be set, which the tool does automatically.

## Monitoring and Alerts

### Connection Performance

The tool reports connection performance:

```
[DB-CONNECT] Connecting to Database...
  ✅ Database connection established successfully
    Connection time: 250 ms
  Validating connection...
  ✅ Connection is valid (validated in 30 seconds)
```

**Thresholds:**
- < 500 ms: Excellent
- 500-2000 ms: Good
- 2000-5000 ms: Acceptable
- > 5000 ms: Slow (investigate)

### Query Performance

The tool reports query performance:

```
[DB-001] ESP_APPLICATION Table Size
  ✅ ESP_APPLICATION Table Size: 50000 (OK)
  ⚠️  Query took 1250 ms (slow)
```

**Thresholds:**
- < 100 ms: Excellent
- 100-500 ms: Good
- 500-1000 ms: Acceptable
- > 1000 ms: Slow (warning)
- > 30000 ms: Timeout (error)

## Additional Resources

- [PostgreSQL SSL Documentation](https://www.postgresql.org/docs/current/ssl-tcp.html)
- [Oracle SSL Documentation](https://docs.oracle.com/en/database/oracle/oracle-database/21/dbseg/configuring-secure-sockets-layer-authentication.html)
- [SQL Server Encryption](https://learn.microsoft.com/en-us/sql/relational-databases/security/encryption/sql-server-encryption)
- [DB2 SSL Configuration](https://www.ibm.com/docs/en/db2/11.5?topic=db2-configuring-ssl-support-in-driver)
- [Java Keytool Documentation](https://docs.oracle.com/en/java/javase/11/tools/keytool.html)

## Support

For issues or questions:
1. Check this guide first
2. Review error messages carefully
3. Verify certificate configuration
4. Check dSeries logs: `<install_dir>/log/`
5. Contact Broadcom Support with:
   - Error messages
   - `db.properties` (with passwords redacted)
   - Certificate details (keytool -list output)
   - Health check output
