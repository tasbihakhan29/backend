# MedSync Database Connection Troubleshooting Guide

## Error: Hibernate JDBC Connection Failed

If you're seeing the error:
```
org.hibernate.resource.transaction.backend.jdbc.internal.JdbcIsolationDelegate.delegateWork
```

This means Hibernate cannot connect to your database during startup. Follow these steps:

---

## Quick Diagnosis Checklist

### 1. **Verify `.env` File Exists and is Correct**
- Ensure `.env` file exists in the project root: `E:\medsync\backend\.env`
- Check the file contains all required variables:
  ```
  DB_URL=jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres?sslmode=require
  DB_USERNAME=postgres.zsefkovkesztufpnqaheDB_PASSWORD=Y3,qfMx$Z5TEEJRJWT_SECRET=MedSyncCitySecretKey2024VeryLongSecretKeyForJWTSigning
  ```

### 2. **Test Database Accessibility**

#### Using psql (if installed):
```powershell
# Install PostgreSQL client if needed
psql -h aws-1-ap-southeast-2.pooler.supabase.com -p 6543 -U "postgres.zsefkovkesztufpnqahe" -d postgres
# When prompted, enter password: Y3,qfMx$Z5TEEJR
```

#### Using Java (if psql not available):
- Create a simple test file `TestConnection.java`:
```java
import java.sql.Connection;
import java.sql.DriverManager;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres?sslmode=require";
        String user = "postgres.zsefkovkesztufpnqaheString password = "Y3,qfMx$Z5TEEJR";
        
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✓ Connection successful!");
            conn.close();
        } catch (Exception e) {
            System.err.println("✗ Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

---

## Common Issues and Solutions

### Issue 1: **Wrong Credentials**
**Symptom:** `PSQLException: FATAL: invalid_authorization_specification`

**Solution:**
1. Verify Supabase credentials:
   - Go to https://app.supabase.com → Your Project → Settings → Database
   - Check the connection string matches your `.env` file
   - Verify username and password are correct
2. Update `.env` with correct credentials
3. Restart the application

### Issue 2: **Database Not Reachable**
**Symptom:** `timeout expired` or `Network is unreachable`

**Solution:**
1. Check internet connection
2. Verify Supabase region availability:
   - Your DB is in `ap-southeast-2` (AWS Sydney)
   - Check if Supabase is accessible from your IP
3. Try using VPN if blocked by firewall
4. Temporarily disable `sslmode=require` in `.env` for testing:
   ```
   DB_URL=jdbc:postgresql://aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres?sslmode=disable
   ```

### Issue 3: **SSL/TLS Certificate Issues**
**Symptom:** `SSL error` or `CERTIFICATE_VERIFY_FAILED`

**Solution:**
1. Ensure `sslmode=require` is set (it already is)
2. The exception is normal for Supabase - it handles SSL automatically
3. If still failing, try adding to JVM arguments:
   ```
   -Duser.country=US -Duser.language=en
   ```

### Issue 4: **Password Contains Special Characters**
**Symptom:** Connection fails with special characters like `$`, `,`

**Solution:**
The password is properly being read from `.env`. If issues persist:
1. Check if `.env` is using proper encoding (UTF-8)
2. Verify no quotes are around the password in `.env`
3. Ensure no trailing spaces after password value

### Issue 5: **Connection Pool Exhausted**
**Symptom:** `Cannot get a connection` after first failed attempt

**Solution:**
1. Restart the application completely
2. Check database pool configuration in `application.properties`
3. Increase connection timeout:
   ```
   spring.datasource.hikari.connection-timeout=30000
   ```

---

## Advanced Debugging

### Enable SQL Logging:
Add to `application.properties`:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Check Connection Details in Logs:
Look for these debug messages when starting the app:
```
✓ Database connection pool initialized successfully
✓ Database connection test successful
```

If you see `✗` messages, the diagnostics will tell you exactly what failed.

---

## Environment Variables Not Loading?

If `.env` variables aren't being loaded:

1. Check if `spring-dotenv` dependency is in `pom.xml`:
   ```xml
   <dependency>
       <groupId>me.paulschwarz</groupId>
       <artifactId>spring-dotenv</artifactId>
       <version>3.0.0</version>
   </dependency>
   ```

2. Ensure `.env` file is in the project root (not in `src/resources`)

3. Set explicit property sources in `application.properties`:
   ```properties
   spring.config.import=optional:file:.env[.properties]
   ```

---

## Still Not Working?

1. **Check Supabase Dashboard:**
   - Visit https://app.supabase.com
   - Verify your project is active
   - Check database logs for connection attempts
   - Look for any active connections or blocked attempts

2. **Verify Network:**
   ```powershell
   Test-NetConnection -ComputerName aws-1-ap-southeast-2.pooler.supabase.com -Port 6543
   ```

3. **Check Application Logs:**
   - Look at full exception stack trace
   - Search for "Connection refused" or "timeout"
   - Check for any SSL warnings

4. **Try Local PostgreSQL (for testing):**
   - Install PostgreSQL locally
   - Update `.env` to use localhost
   - Verify app works with local DB
   - Then switch back to Supabase

---

## Configuration Changes Made

The latest configuration includes:
- Increased connection timeout to 30 seconds
- Added connection validation query
- Improved Hikari pool settings
- Better SSL handling for Supabase
- Automatic connection testing on startup

Try restarting the application and check for the `✓ Database connection test successful` message.

