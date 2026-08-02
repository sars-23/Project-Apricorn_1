# Security architecture

## Authentication flow

Passwords are accepted only by the registration and login endpoints. Registration hashes the password with BCrypt before it reaches PostgreSQL. Login delegates credential verification to Spring Security's `AuthenticationManager`, backed by a custom `UserDetailsService` that loads users by email.

```text
Client
  | POST /api/v1/auth/login
  v
AuthController -> AuthService -> AuthenticationManager
                                  |-> UserDetailsService -> PostgreSQL
                                  |-> BCrypt PasswordEncoder
  v
JwtService -> Bearer JWT returned to client
```

JWTs use the email as the minimal subject and have no password, password hash, or personally sensitive claims. The signing key is supplied through `JWT_SECRET`; it is never stored in source control. `JWT_EXPIRATION` controls expiry in milliseconds.

## Authenticated requests

```text
Client (Authorization: Bearer JWT)
  v
SecurityFilterChain -> JwtAuthenticationFilter -> JWT validation
  v                                          -> UserDetailsService
SecurityContext -> Controller
```

Requests without a token continue through the chain but protected routes receive `401 Unauthorized`. Invalid or expired tokens never populate the security context. `USER` and `ADMIN` become `ROLE_USER` and `ROLE_ADMIN` authorities. The admin test endpoint requires `ROLE_ADMIN`; authenticated users without it receive `403 Forbidden`.

## Operational requirements

- Set `JWT_SECRET` to a random value of at least 32 characters.
- Set `JWT_EXPIRATION` in milliseconds; the development default is 3,600,000 ms.
- Use HTTPS in deployed environments.
- Never log passwords, BCrypt hashes, full JWTs, or signing secrets.
- Database schema changes are versioned in Flyway migrations under `backend/src/main/resources/db/migration`.
