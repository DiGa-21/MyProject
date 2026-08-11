# Supabase setup

The Android app uses Room first and Supabase only for authenticated sync. Apply
`supabase/migrations/001_family_data.sql` before enabling a remote environment.

Keep credentials outside Git. Put these lines in `%USERPROFILE%\.gradle\gradle.properties`:

```properties
supabaseUrl=https://YOUR_PROJECT.supabase.co
supabasePublishableKey=YOUR_PUBLISHABLE_KEY
```

Only the publishable key is allowed in the Android client. Never put a
service-role key in Gradle properties, source code, or an APK. Run the checks
in `supabase/tests/rls_checks.sql` with separate parent and child sessions.

The sync worker requires a network connection, retries with exponential
backoff, and keeps local changes in Room when the service is unavailable.
