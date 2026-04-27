# MedSync Backend on Render

This backend is configured for deployment on **Render** with **Supabase PostgreSQL**.

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


