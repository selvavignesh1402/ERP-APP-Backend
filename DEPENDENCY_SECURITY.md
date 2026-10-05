# Dependency security

The dependency-security workflow uses OSV to scan the Maven manifest and resolve transitive runtime dependencies on pushes and pull requests. Findings fail the job and remain visible in workflow results. Dependabot separately proposes Maven and GitHub Actions updates. These workflows must be committed and pushed before hosted results exist.

This does not certify the existing Spring Boot, JJWT, or Firebase versions as free of vulnerabilities. Version upgrades must be tested against authentication, tenant checks, database migrations, and the application APIs. The user's excluded OTP behavior and JWT signing-key changes remain separate from dependency maintenance.

The [OSV Maven documentation](https://google.github.io/osv-scanner/supported-languages-and-lockfiles/) describes runtime dependency resolution and the limitation that test dependencies are not included in its computed Maven graph. The [workflow documentation](https://google.github.io/osv-scanner/github-action/) describes report artifacts and failure behavior.
