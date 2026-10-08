# Firebase Admin setup

Firebase is optional and disabled by default. Password login and the existing backend OTP implementation are unchanged. Enabling Firebase does not configure SMS delivery, phone-auth providers or app verification; those remain separate Firebase project settings.

To enable Firebase token verification locally, put a service-account JSON file outside the repository. In the PowerShell terminal that starts the backend, set:

```powershell
$env:FIREBASE_ENABLED = 'true'
$env:FIREBASE_CREDENTIALS_PATH = 'C:\private\erp-firebase-service-account.json'
$env:FIREBASE_PROJECT_ID = 'your-firebase-project-id'
```

Replace the example path and project ID with your own. Start the backend from that terminal. Spring Boot reads these environment variables; it does not automatically read a frontend `.env` file. An explicitly enabled but invalid configuration stops startup with a configuration error.

Alternatively, omit FIREBASE_CREDENTIALS_PATH and use Application Default Credentials: set GOOGLE_APPLICATION_CREDENTIALS to the external file, or use the workload's attached Google identity. FIREBASE_PROJECT_ID can be omitted when the credentials identify the project. No service-account file is loaded from the application classpath or packaged intentionally in the application. Do not copy private credentials into src/main/resources, source control or any EXPO_PUBLIC variable.

The frontend's EXPO_PUBLIC_FIREBASE_API_KEY must belong to the same Firebase project. It is a public client configuration value, not an Admin credential. See the frontend SETUP.md for installation and environment configuration. Live Firebase access requires credentials supplied by the deployment owner; automated tests do not contact Google.
