# Admin Account Provisioning & Role Management Specification

## Security Design

In Project Apricorn, public registration (`POST /api/v1/auth/register`) strictly assigns the `ROLE_USER` role to all newly registered accounts to prevent unauthorized privilege escalation.

No public endpoint or registration parameter can assign `ROLE_ADMIN`.

## Development Admin Provisioning

For local development or administrative setup, upgrade a user account to `ROLE_ADMIN` using direct database access:

### 1. Register a Standard User Account
Register a user account via API or Swagger UI:
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "admin_user",
  "email": "admin@apricorn.com",
  "password": "SecurePassword123!"
}
```

### 2. Promote Account Role in PostgreSQL
Execute the following SQL command against your PostgreSQL instance:
```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'admin_user';
```

### 3. Re-authenticate
Obtain a fresh JWT by logging in (`POST /api/v1/auth/login`). The issued JWT will now contain the `ROLE_ADMIN` authority required for administrative management APIs.
