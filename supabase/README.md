# Supabase setup

1. Create a Supabase project and apply `migrations/001_family_data.sql` in the SQL editor.
2. In the Android app, configure only the project URL and publishable key through local Gradle properties or an untracked environment file. Never commit a service-role key.
3. Create a parent account, insert a child row and a short-lived `invite_codes` row from the parent session, then call `consume_invite_code` once from the child session.
4. Run `tests/rls_checks.sql` using separate parent and child sessions. The child receives `display_name` and `hero`; `parent_label` is exposed only by `parent_children`.

The Android client remains offline-first: Room is the source of truth and the sync worker will retry idempotent writes after connectivity returns.
