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

## Supabase connection note

If Render cannot reach the direct Supabase host, use the **Session Pooler** connection string from Supabase instead of the direct `db.<project-ref>.supabase.co:5432` URL.

Typical JDBC format:

```text
jdbc:postgresql://<pooler-host>:6543/postgres?sslmode=require&pgbouncer=true
```

## Render deploy settings

- **Build command:** `mvn clean package -DskipTests`
- **Start command:** `java -jar target/medsync-backend-1.0.0.jar`
- **Health check:** `/api/public/health`

## Local development

You can keep a local `.env` file for development, but do not commit secrets.


