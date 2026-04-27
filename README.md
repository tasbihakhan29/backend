# MedSync Backend on Render

This backend is configured for deployment on **Render** with **Supabase PostgreSQL**.

## 🚀 Quick Links

**Having connection issues?** Start here:
- 📖 **[QUICK_START.MD](QUICK_START.MD)** - Get running fast
- 🔧 **[SOLUTION_SUMMARY.MD](SOLUTION_SUMMARY.MD)** - Complete solution overview
- 🆘 **[DATABASE_TROUBLESHOOTING.MD](DATABASE_TROUBLESHOOTING.MD)** - Detailed troubleshooting
- ⚙️ **[IMPLEMENTATION_SUMMARY.MD](IMPLEMENTATION_SUMMARY.MD)** - Technical details

**Diagnostic Tools:**
```powershell
# Windows PowerShell
.\diagnose.ps1

# Windows Batch
.\diagnose.bat
```

## Required environment variables

Set these in Render:

- `DB_URL` — Supabase **Session Pooler** or direct PostgreSQL JDBC URL
- `DB_USERNAME` — usually `postgres`
- `DB_PASSWORD` — your Supabase database password
- `JWT_SECRET` — a long random secret for JWT signing
- `APP_CORS_ALLOWED_ORIGINS` — comma-separated frontend origins, e.g. `https://your-frontend.onrender.com`
- `APP_SEED_DEFAULT_ADMIN` — keep `false` on Render
- `DB_POOL_SIZE` — optional, keep `1` on Render with Supabase pooler
- `DB_MIN_IDLE` — optional, keep `0` on Render with Supabase pooler

## Supabase connection note

For Render, prefer the **Transaction Pooler** connection string from Supabase.

If you must use the **Session Pooler**, keep the Hikari pool very small (for example `DB_POOL_SIZE=1`) or you can hit Supabase's client limit.

Typical JDBC format:

```text
jdbc:postgresql://<pooler-host>:6543/postgres?sslmode=require&pgbouncer=true
```

Example Render values:

```text
DB_POOL_SIZE=1
DB_MIN_IDLE=0
```

## Render deploy settings

- **Build command:** `mvn clean package -DskipTests`
- **Start command:** `java -jar target/medsync-backend-1.0.0.jar`
- **Health check:** `/api/public/health`

## Local development

You can keep a local `.env` file for development, but do not commit secrets.

### Testing Connection Locally

**Option 1: Use PowerShell diagnostic script**
```powershell
cd E:\medsync\backend
.\diagnose.ps1
```

**Option 2: Start with Maven**
```powershell
mvn spring-boot:run
```

**Option 3: Build and run**
```powershell
mvn clean package -DskipTests
java -jar target/medsync-backend-1.0.0.jar
```

### What to look for

✅ **Success indicators:**
```
✓ Database connection pool initialized successfully
✓ Database connection test successful
Started MedSyncApplication in X seconds
```

❌ **Failure indicators:**
```
✗ Failed to initialize database connection pool
Error: [specific database error]
```

## Recent Improvements (v1.1)

- ✅ Enhanced database connection handling
- ✅ Automatic connection testing on startup
- ✅ Better error messages for diagnostics
- ✅ Optimized HikariCP pool settings for Supabase
- ✅ PowerShell and Batch diagnostic tools
- ✅ Comprehensive troubleshooting guides

## Architecture

```
Spring Boot 3.2.0
├── Spring Web
├── Spring Data JPA (Hibernate 6.3.1)
├── Spring Security
├── PostgreSQL Driver
├── JWT (jjwt 0.11.5)
└── Lombok 1.18.30
```

## Database Schema

Auto-created tables:
- `users` - User accounts and roles
- `hospitals` - Hospital institutions
- `medicines` - Medicine inventory
- `medicine_batches` - Batch tracking
- `requests` - Medicine requests
- `audit_logs` - Activity logging

## API Endpoints

- `GET /api/public/health` - Health check
- `POST /api/auth/register` - Register hospital
- `POST /api/auth/login` - Login
- `POST /api/medicines` - Create medicine [Admin]
- `GET /api/medicines` - List medicines
- `POST /api/requests` - Create request
- `GET /api/requests` - List requests

## Docs

See `docs/` folder for API documentation and setup guides.
