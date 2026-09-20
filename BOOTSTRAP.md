# First platform administrator

Normal application startup does not create an administrator or assign a default password. Existing accounts are not reset or modified by this change.

For a new installation with no MASTER_ADMIN accounts, provide these Spring properties through deployment configuration outside version control:

| Property | Value |
|---|---|
| `app.bootstrap.master-admin.enabled` | `true` only for the initial bootstrap |
| `app.bootstrap.master-admin.phone` | Explicit phone number to use at password login |
| `app.bootstrap.master-admin.name` | Explicit administrator name |
| `app.bootstrap.master-admin.password` | Explicit password: at least 12 non-padding characters, at most 72 UTF-8 bytes |

Start a single backend instance for bootstrap. The account is created transactionally with a BCrypt password hash and platform MASTER_ADMIN role. It does not need an organization membership to access platform administration.

Disable bootstrap and remove its password from deployment configuration after the first successful start. If any MASTER_ADMIN already exists (including an inactive one), bootstrap performs no account changes. It also refuses to promote an existing ordinary account with the supplied phone. Missing identity/password configuration fails startup when bootstrap is enabled and no administrator exists.

New shop-admin and staff accounts require explicitly supplied passwords with the same length limits. Linking an existing account keeps its current password. Provisioning never supplies a fallback password or resets an existing account's credentials.

Accounts previously created with default credentials still require an explicit credential-reset process; this change does not silently alter existing users or access the live database. Demo SQL seeding and the other issues in the project analysis are separate pending fixes.
