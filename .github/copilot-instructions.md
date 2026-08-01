# Copilot Instructions for CTF Contest Platform

## Architecture Overview

This is a **Dockerized full-stack CTF competition platform** with three services:
- **Frontend**: Vue 3 + Vite + Tailwind CSS (Nginx on port 3615)
- **Backend**: Spring Boot 3.1.5 + MyBatis (port 8615)
- **Database**: MySQL 8.0 (port 3306)

**Key data flow**: Browser → Nginx (`/api/*`) → Spring Boot → MySQL

## Critical Development Patterns

### API Communication
- Frontend uses `/api` as baseURL (NOT `http://backend:8615`) - Nginx proxies to backend
- All API methods in `frontend/src/api/client.js` - keep naming consistent (`createX`, `updateX`, `deleteX`)
- JWT token stored in localStorage, auto-attached via Axios interceptor

### Database & Entities
- Schema defined in `backend/src/main/resources/schema.sql` - auto-runs on startup
- **MySQL 8.0 constraint**: No `CREATE INDEX IF NOT EXISTS` - use inline indexes in `CREATE TABLE`
- Password field uses `@JsonProperty(access = WRITE_ONLY)` - NOT `@JsonIgnore` (blocks deserialization)
- BCrypt for password hashing via `PasswordUtil.encodePassword()`

### Frontend Routing
- Uses **Hash routing** (`createWebHashHistory`) to prevent refresh navigation issues
- Route guards in `router/index.js`: check `isAuthenticated` and `isAdmin()` before navigation
- User object persisted in localStorage (not just token) for role checks after refresh

### Contest Time Management
- Stored in `contest_config` table (NOT yaml files) - changes take effect immediately
- Four time phases: `readyTime` → `startTime` → `endTime` → `resultsTime`
- `ContestTimeUtil.java` reads from database on each request

## Common Commands

```bash
# Start all services
docker compose up -d --build

# View logs
docker compose logs backend --tail=50
docker compose logs frontend --tail=50

# Database access
docker exec -it ctf-mysql mysql -uroot -proot ctf_db

# Rebuild single service
docker compose up -d --build backend

# Full reset (delete data)
docker compose down -v && docker compose up -d --build
```

## File Structure Patterns

| Path | Purpose |
|------|---------|
| `frontend/src/api/client.js` | Centralized API client with auth interceptor |
| `frontend/src/store/auth.js` | Auth state with localStorage persistence |
| `frontend/src/composables/useModal.js` | Custom modal system (avoid native alerts) |
| `backend/src/main/java/com/ctf/controller/` | REST endpoints |
| `backend/src/main/java/com/ctf/mapper/` | MyBatis mappers (annotations + XML in resources/mapper/) |
| `backend/src/main/resources/schema.sql` | Database initialization |

## Known Gotchas

1. **MySQL connection**: JDBC URL must include `allowPublicKeyRetrieval=true`
2. **CORS**: Handled via Nginx proxy, not Spring CORS config
3. **Admin panel modals**: Use `Modal.vue` + `useModal` composable, not native `alert()`/`confirm()`
4. **API response handling**: Always check `response.data.code === 200` before success message
5. **User creation**: Frontend sends `passwordHash` field (raw password), backend BCrypt-encodes it

## Default Credentials

- Admin: `root` / `admin123`
- Test user: `2024001` / `123456`
