# Local demo authentication

The `local-demo` Spring profile is an explicit, local-only QA mechanism. It is
not active by default and is never enabled by production configuration.

Before starting the backend, set all four values in the current process:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local-demo"
$env:DEMO_STUDENT_EMAIL = "student@example.test"
$env:DEMO_STUDENT_PASSWORD = "<local-only password>"
$env:DEMO_ADMIN_EMAIL = "admin@example.test"
$env:DEMO_ADMIN_PASSWORD = "<local-only password>"
```

The startup seeder uses the application `PasswordHasher`, creates a `CUSTOMER`
and an `ADMIN` only when they do not already exist, and is idempotent. Existing
accounts with the wrong role stop startup instead of being elevated or
overwritten. Missing credentials also stop the local-demo startup. No password
is stored in tracked configuration, and the public registration endpoint never
creates an administrator.

Do not enable `local-demo` in production or reuse these identities outside the
isolated local QA database.
