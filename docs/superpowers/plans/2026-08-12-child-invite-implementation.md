# Child Invite Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a signed-in parent create a child profile and secure one-time six-digit code, then let the child link one device, choose Tom or Lily, and keep that device linked until the parent disconnects it.

**Architecture:** A Supabase anonymous Auth session identifies the child device without collecting email or the parent password. A second Supabase client uses the separate storage key `myway-child-session`, so parent and child sessions can coexist on one family device without signing each other out. Security-critical code generation, hashing, expiry, single-use consumption, invalid-attempt throttling, and unlinking live in security-definer PostgreSQL functions; Android calls typed RPCs through `FamilyGateway`. The existing Room profile becomes the local offline copy of the linked Supabase child and continues powering the current scaffold.

**Tech Stack:** PostgreSQL/Supabase RLS and RPC, supabase-kt 3.2.4 Auth/PostgREST, Kotlin serialization, Room, StateFlow/ViewModel, Jetpack Compose, JUnit 4 and Compose UI tests.

## Global Constraints

- A parent creates the initial child name and may keep a separate `parent_label`; `parent_label` is never returned to or displayed on the child device.
- The child, not the parent, chooses Tom or Lily after successful linking and may edit the visible name later.
- The code contains exactly 6 digits, expires after 15 minutes, is single-use, and is stored only as a SHA-256 hash.
- Creating a new code invalidates any earlier unconsumed code for the same child.
- Five consecutive invalid attempts lock that anonymous device session for 5 minutes; the UI shows seconds remaining.
- The child session persists locally through Supabase Auth storage. The parent account password is never accepted on the child screen.
- An anonymous Supabase user has the `authenticated` role, so every parent-only policy/function must explicitly reject JWTs whose `is_anonymous` claim is true.
- A child device may read and modify only its linked `children.user_id = auth.uid()` profile and associated child data.
- Parent disconnect removes `children.user_id`; the child returns to code entry after the next profile refresh or app launch.
- Do not log the plaintext code, anonymous user id, access token, or refresh token.

---

## File Map

- `supabase/migrations/002_child_invite_onboarding.sql`: secure code lifecycle, rate-limit state, parent RPCs, child consume RPC, and tightened RLS.
- `supabase/tests/child_invite_checks.sql`: SQL assertions for expiry, single use, invalidation, lockout, and private labels.
- `features/family/FamilyContract.kt`: app-owned DTOs and `FamilyGateway`.
- `data/remote/SupabaseRepository.kt`: typed implementations of parent and child RPC calls.
- `AppContainer.kt`: separate parent and child Supabase clients with non-overlapping persistent session keys.
- `features/family/InviteCodeViewModel.kt`: child link state and anonymous-session orchestration.
- `features/family/InviteCodeScreens.kt`: six-digit entry UI and lockout countdown.
- `features/family/ParentChildLinkViewModel.kt`: parent profile/code/disconnect actions.
- `features/family/ParentChildLinkCard.kt`: parent cabinet controls and code display.
- `data/AppRepository.kt` and `RoomAppRepository.kt`: replace the local offline child with the linked remote id/profile.
- `features/scaffold/ScaffoldScreen.kt`: child connection gate and parent children-tab integration.

### Task 1: Secure Invite-Code Database Functions

**Files:**
- Create: `supabase/migrations/002_child_invite_onboarding.sql`
- Create: `supabase/tests/child_invite_checks.sql`

**Interfaces:**
- Produces RPC: `create_child_profile(input_display_name text, input_parent_label text)`.
- Produces RPC: `create_child_invite(input_child_id uuid)`.
- Produces RPC: `consume_child_invite(input_code text)`.
- Produces RPC: `disconnect_child_device(input_child_id uuid)`.

- [ ] **Step 1: Write failing SQL behavior checks**

Create transaction-wrapped SQL that uses `set local request.jwt.claims` to impersonate one permanent parent and one anonymous child. Assert these exact outcomes:

```sql
select plan(9);
select is((select length(code) from create_child_invite(test_child_id)), 6, 'code has six digits');
select ok((select code ~ '^[0-9]{6}$' from create_child_invite(test_child_id)), 'code is numeric');
select is((select status from consume_child_invite(valid_code)), 'LINKED', 'valid code links once');
select is((select status from consume_child_invite(valid_code)), 'INVALID', 'consumed code cannot be reused');
select is((select status from consume_child_invite(expired_code)), 'INVALID', 'expired code is rejected');
select is((select status from consume_child_invite(old_code)), 'INVALID', 'new code invalidates old code');
select is((select status from consume_child_invite('000000')), 'RATE_LIMITED', 'fifth invalid attempt locks session');
select ok((select retry_after_seconds > 0 from consume_child_invite('000000')), 'lock response includes remaining seconds');
select is((select parent_label from child_profile), null, 'child API never exposes parent label');
select * from finish();
rollback;
```

- [ ] **Step 2: Run the checks against a Supabase development database**

Run: `supabase db reset`

Run: `supabase test db supabase/tests/child_invite_checks.sql`

Expected: FAIL because the four RPCs and rate-limit table do not exist.

- [ ] **Step 3: Add rate-limit storage and revoke direct access**

```sql
create table public.invite_rate_limits (
  actor_user_id uuid primary key references auth.users(id) on delete cascade,
  failed_attempts integer not null default 0 check (failed_attempts >= 0),
  locked_until timestamptz,
  updated_at timestamptz not null default now()
);

alter table public.invite_rate_limits enable row level security;
revoke all on public.invite_rate_limits from anon, authenticated;
revoke all on public.invite_codes from anon, authenticated;
```

- [ ] **Step 4: Implement permanent-parent authorization helper**

```sql
create or replace function public.is_permanent_parent()
returns boolean
language sql stable security invoker
as $$
  select auth.uid() is not null
     and coalesce((auth.jwt()->>'is_anonymous')::boolean, false) is false;
$$;
```

All parent functions start with `if not public.is_permanent_parent() then raise exception 'parent authentication required'; end if;` and verify `children.family_id = auth.uid()`.

- [ ] **Step 5: Implement profile creation and code generation**

`create_child_profile` trims the visible name, rejects blank names, inserts `family_id = auth.uid()`, `user_id = null`, `hero = 'BOY'`, and returns the complete parent view including `parent_label`.

`create_child_invite` must:

```sql
update public.invite_codes
set consumed_at = now()
where child_id = input_child_id and consumed_at is null;

generated_code := lpad((floor(random() * 900000) + 100000)::integer::text, 6, '0');

insert into public.invite_codes(family_id, child_id, code_hash, expires_at)
values (auth.uid(), input_child_id, encode(digest(generated_code, 'sha256'), 'hex'), now() + interval '15 minutes');
```

Return only `code text` and `expires_at timestamptz`; never store or return the code in any other table.

- [ ] **Step 6: Implement structured consume results without rollback-prone exceptions**

The function return type is:

```sql
table(status text, child_id uuid, display_name text, hero text, retry_after_seconds integer)
```

Processing order:

1. Require `auth.uid()` and `(auth.jwt()->>'is_anonymous')::boolean is true`.
2. Lock/create the caller’s `invite_rate_limits` row.
3. If `locked_until > now()`, return `RATE_LIMITED` with `ceil(extract(epoch from locked_until - now()))`.
4. Match `digest(trim(input_code), 'sha256')`, `consumed_at is null`, and `expires_at > now()`.
5. On no match, increment failures; at 5 set `locked_until = now() + interval '5 minutes'`, reset failures to 0, and return `INVALID` or `RATE_LIMITED`.
6. On match, atomically set `invite_codes.consumed_at = now()`, set `children.user_id = auth.uid()`, clear the rate row, and return `LINKED` plus the child id, visible name, and hero only.

- [ ] **Step 7: Implement parent disconnect and child-safe view**

`disconnect_child_device` checks ownership and runs:

```sql
update public.children
set user_id = null, updated_at = now()
where id = input_child_id and family_id = auth.uid();
```

Replace `child_profile` so its columns are only `id`, `family_id`, `user_id`, `display_name`, `hero`, and `updated_at`; `parent_label` must be absent.

- [ ] **Step 8: Grant least-privilege RPC access and rerun SQL tests**

```sql
grant execute on function public.create_child_profile(text, text) to authenticated;
grant execute on function public.create_child_invite(uuid) to authenticated;
grant execute on function public.consume_child_invite(text) to authenticated;
grant execute on function public.disconnect_child_device(uuid) to authenticated;
```

Run: `supabase db reset`

Run: `supabase test db supabase/tests/child_invite_checks.sql`

Expected: all 9 checks PASS.

- [ ] **Step 9: Commit the database contract**

```powershell
git add supabase/migrations/002_child_invite_onboarding.sql supabase/tests/child_invite_checks.sql
git commit -m "feat: secure child invite codes"
```

### Task 2: Family Gateway and Typed RPC Responses

**Files:**
- Create: `app/src/main/java/com/myhomechores/app/features/family/FamilyContract.kt`
- Modify: `app/src/main/java/com/myhomechores/app/data/remote/SupabaseRepository.kt`
- Modify: `app/src/main/java/com/myhomechores/app/AppContainer.kt`
- Create: `app/src/test/java/com/myhomechores/app/features/family/InviteResultMappingTest.kt`

**Interfaces:**
- Produces: `FamilyGateway.ensureAnonymousChildSession()`.
- Produces: `createChildProfile`, `createInvite`, `consumeInvite`, `childProfile`, and `disconnectChildDevice`.
- Produces: `InviteConsumeResult.Linked`, `.Invalid`, and `.RateLimited`.

- [ ] **Step 1: Define serialized remote and app-owned result types**

```kotlin
@Serializable data class InviteCodeResponse(val code: String, val expires_at: String)
@Serializable data class InviteConsumeResponse(
    val status: String,
    val child_id: String? = null,
    val display_name: String? = null,
    val hero: String? = null,
    val retry_after_seconds: Int = 0,
)

sealed interface InviteConsumeResult {
    data class Linked(val child: RemoteChild) : InviteConsumeResult
    data object Invalid : InviteConsumeResult
    data class RateLimited(val retryAfterSeconds: Int) : InviteConsumeResult
}

interface FamilyGateway {
    suspend fun ensureAnonymousChildSession()
    suspend fun createChildProfile(displayName: String, parentLabel: String?): RemoteChild
    suspend fun createInvite(childId: String): InviteCodeResponse
    suspend fun consumeInvite(code: String): InviteConsumeResult
    suspend fun childProfile(): RemoteChild?
    suspend fun disconnectChildDevice(childId: String)
}
```

Remove `parent_label` from the child-safe serialized response type or decode parent and child views into separate DTOs.

- [ ] **Step 2: Write failing mapping tests**

```kotlin
@Test fun linked_response_maps_to_child() = assertTrue(
    mapInviteResponse(InviteConsumeResponse("LINKED", "child-1", "Саша", "GIRL")) is InviteConsumeResult.Linked
)

@Test fun rate_limit_keeps_retry_seconds() = assertEquals(
    InviteConsumeResult.RateLimited(240),
    mapInviteResponse(InviteConsumeResponse("RATE_LIMITED", retry_after_seconds = 240)),
)
```

- [ ] **Step 3: Run the mapper tests and verify failure**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.InviteResultMappingTest"`

Expected: FAIL because the types and mapper do not exist.

- [ ] **Step 4: Implement Supabase Auth and RPC calls**

First create the child client separately from the parent client:

```kotlin
val childSupabase = SupabaseClientProvider.create(
    BuildConfig.SUPABASE_URL,
    BuildConfig.SUPABASE_PUBLISHABLE_KEY,
    sessionKey = "myway-child-session",
)
val familyRepository: FamilyGateway = SupabaseRepository(childSupabase)
```

`SupabaseClientProvider.create` passes `SettingsSessionManager(key = sessionKey)` to Auth; this is the exact supabase-kt 3.2.4 API. Add a unit or integration assertion that signing the child client in anonymously does not change the parent client’s saved session.

```kotlin
override suspend fun ensureAnonymousChildSession() {
    val user = client.auth.currentUserOrNull()
    if (user == null) client.auth.signInAnonymously()
    else require(user.isAnonymous == true) { "На устройстве активен аккаунт родителя" }
}

override suspend fun createInvite(childId: String): InviteCodeResponse =
    client.postgrest.rpc("create_child_invite", buildJsonObject { put("input_child_id", childId) })
        .decodeSingle()

override suspend fun consumeInvite(code: String): InviteConsumeResult =
    mapInviteResponse(
        client.postgrest.rpc("consume_child_invite", buildJsonObject { put("input_code", code) })
            .decodeSingle(),
    )
```

Use corresponding RPC names and argument keys for profile creation and disconnect.

- [ ] **Step 5: Run mapper and auth unit tests**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.InviteResultMappingTest"`

Expected: PASS.

- [ ] **Step 6: Commit the family gateway**

```powershell
git add app/src/main/java/com/myhomechores/app/features/family/FamilyContract.kt app/src/main/java/com/myhomechores/app/data/remote/SupabaseRepository.kt app/src/main/java/com/myhomechores/app/AppContainer.kt app/src/test/java/com/myhomechores/app/features/family/InviteResultMappingTest.kt
git commit -m "feat: add typed family invite gateway"
```

### Task 3: Child Code Entry, Lockout, and Local Profile Import

**Files:**
- Replace: `app/src/main/java/com/myhomechores/app/features/family/InviteCodeViewModel.kt`
- Replace: `app/src/main/java/com/myhomechores/app/features/family/InviteCodeScreens.kt`
- Modify: `app/src/main/java/com/myhomechores/app/data/AppRepository.kt`
- Modify: `app/src/main/java/com/myhomechores/app/data/RoomAppRepository.kt`
- Modify: `app/src/main/java/com/myhomechores/app/data/local/Daos.kt`
- Create: `app/src/test/java/com/myhomechores/app/features/family/InviteCodeViewModelTest.kt`
- Create: `app/src/androidTest/java/com/myhomechores/app/features/family/InviteCodeScreenTest.kt`

**Interfaces:**
- Consumes: `FamilyGateway` and `InviteConsumeResult`.
- Produces: `AppRepository.replaceLinkedChild(profile: ChildProfile)`.
- Produces: `InviteCodeUiState` with code, loading, linked child, error, and retry seconds.

- [ ] **Step 1: Add a failing local replacement repository test**

Add an Android Room test that inserts `local-child`, calls `replaceLinkedChild(ChildProfile("remote-1", "Саша", null, GIRL))`, and asserts the DAO contains only `remote-1`. This prevents stale local data from driving the linked child screen.

- [ ] **Step 2: Add failing ViewModel tests**

```kotlin
@Test fun only_six_digits_are_kept() {
    viewModel.updateCode("12a34567")
    assertEquals("123456", viewModel.state.value.code)
}

@Test fun successful_code_imports_remote_child() = runTest {
    gateway.consumeResult = InviteConsumeResult.Linked(RemoteChild("remote-1", "family-1", display_name = "Саша", hero = "GIRL"))
    viewModel.updateCode("123456")
    viewModel.submit()
    advanceUntilIdle()
    assertEquals("remote-1", localRepository.lastReplacement?.id)
}

@Test fun rate_limit_disables_submit_and_shows_countdown() = runTest {
    gateway.consumeResult = InviteConsumeResult.RateLimited(300)
    viewModel.updateCode("000000")
    viewModel.submit()
    advanceUntilIdle()
    assertEquals(300, viewModel.state.value.retryAfterSeconds)
}
```

- [ ] **Step 3: Run the targeted tests and verify failure**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.InviteCodeViewModelTest"`

Expected: FAIL because the new state and replacement API are missing.

- [ ] **Step 4: Implement atomic local child replacement**

Add DAO `deleteAllChildren()` and repository method:

```kotlin
override suspend fun replaceLinkedChild(profile: ChildProfile) {
    database.withTransaction {
        database.childDao().deleteAll()
        database.childDao().upsert(
            ChildEntity(profile.id, profile.displayName, null, profile.hero, System.currentTimeMillis()),
        )
    }
}
```

Do not enqueue a child UPSERT from the child device during this import; the server already owns the linked profile.

- [ ] **Step 5: Implement code submission and countdown state**

`submit()` first validates exactly 6 digits, calls `ensureAnonymousChildSession()`, then `consumeInvite(code)`. For `Linked`, map `BOY` to `HeroId.BOY`, any approved `GIRL` value to `HeroId.GIRL`, import locally, and expose the linked child. For `Invalid`, show “Код неверный или уже не действует”. For `RateLimited`, start a one-second coroutine countdown and keep submit disabled until zero.

- [ ] **Step 6: Implement the child code screen**

Use one centered numeric `OutlinedTextField` with `KeyboardType.NumberPassword`, maximum 6 characters, large letter spacing, and button “Подключиться”. Show “Попробуй снова через M:SS” during lockout. Do not provide an email or password field. The back control returns to mode selection.

- [ ] **Step 7: Run unit, Room, and Compose tests**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.InviteCodeViewModelTest"`

Run: `./gradlew.bat connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.myhomechores.app.data.local.RoomRepositoryTest,com.myhomechores.app.features.family.InviteCodeScreenTest`

Expected: PASS.

- [ ] **Step 8: Commit child linking UI**

```powershell
git add app/src/main/java/com/myhomechores/app/features/family/InviteCodeViewModel.kt app/src/main/java/com/myhomechores/app/features/family/InviteCodeScreens.kt app/src/main/java/com/myhomechores/app/data app/src/test/java/com/myhomechores/app/features/family/InviteCodeViewModelTest.kt app/src/androidTest/java/com/myhomechores/app/features/family/InviteCodeScreenTest.kt
git commit -m "feat: link child with one-time code"
```

### Task 4: Parent Child Profile, Code Display, and Disconnect

**Files:**
- Create: `app/src/main/java/com/myhomechores/app/features/family/ParentChildLinkViewModel.kt`
- Create: `app/src/main/java/com/myhomechores/app/features/family/ParentChildLinkCard.kt`
- Modify: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt:1530-1575`
- Create: `app/src/test/java/com/myhomechores/app/features/family/ParentChildLinkViewModelTest.kt`
- Create: `app/src/androidTest/java/com/myhomechores/app/features/family/ParentChildLinkCardTest.kt`

**Interfaces:**
- Consumes: `FamilyGateway.createChildProfile`, `createInvite`, and `disconnectChildDevice`.
- Produces: `ParentChildLinkUiState` and `ParentChildLinkCard` for the existing parent children tab.

- [ ] **Step 1: Write failing parent-link ViewModel tests**

```kotlin
@Test fun parent_creates_name_and_private_label() = runTest {
    viewModel.createChild("Саша", "Саша, школа")
    advanceUntilIdle()
    assertEquals("Саша", gateway.createdDisplayName)
    assertEquals("Саша, школа", gateway.createdParentLabel)
}

@Test fun new_code_replaces_old_displayed_code() = runTest {
    gateway.invites.addAll(listOf(InviteCodeResponse("111111", firstExpiry), InviteCodeResponse("222222", secondExpiry)))
    viewModel.createInvite("child-1"); advanceUntilIdle()
    viewModel.createInvite("child-1"); advanceUntilIdle()
    assertEquals("222222", viewModel.state.value.invite?.code)
}

@Test fun disconnect_clears_link_status() = runTest {
    viewModel.disconnect("child-1")
    advanceUntilIdle()
    assertFalse(viewModel.state.value.deviceConnected)
}
```

- [ ] **Step 2: Run tests and verify failure**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.ParentChildLinkViewModelTest"`

Expected: FAIL because the ViewModel is missing.

- [ ] **Step 3: Implement parent ViewModel validation and actions**

Trim the visible name and private label. Reject a blank visible name locally. Keep the plaintext code only in in-memory UI state; clear it when its 15-minute expiry is reached, on sign-out, and when leaving the parent process. Never save the code to Room, preferences, logs, analytics, or saved instance state.

- [ ] **Step 4: Implement the parent card in the children tab**

When no remote child exists, show fields “Имя ребёнка” and “Пометка только для родителя” plus “Создать профиль”. For an existing child show visible name, private label, connection status, “Подключить устройство ребёнка”, and “Отключить устройство”. After code generation, show the six digits in a large accessible card with “Действует 15 минут” and a countdown; do not display the code after expiry.

- [ ] **Step 5: Add a destructive-action confirmation**

Before disconnect, show an `AlertDialog` explaining: “На устройстве ребёнка снова потребуется одноразовый код”. Confirm calls `disconnectChildDevice`; cancel makes no request.

- [ ] **Step 6: Run parent-link unit and Compose tests**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.ParentChildLinkViewModelTest"`

Run: `./gradlew.bat connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.myhomechores.app.features.family.ParentChildLinkCardTest`

Expected: PASS.

- [ ] **Step 7: Commit parent linking controls**

```powershell
git add app/src/main/java/com/myhomechores/app/features/family/ParentChildLinkViewModel.kt app/src/main/java/com/myhomechores/app/features/family/ParentChildLinkCard.kt app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt app/src/test/java/com/myhomechores/app/features/family/ParentChildLinkViewModelTest.kt app/src/androidTest/java/com/myhomechores/app/features/family/ParentChildLinkCardTest.kt
git commit -m "feat: manage child device connection"
```

### Task 5: Child Gate, Dragon Choice, and Persistent Re-entry

**Files:**
- Modify: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldViewModel.kt`
- Modify: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt:128-169`
- Modify: `app/src/main/java/com/myhomechores/app/features/scaffold/ScaffoldScreen.kt:226-360`
- Create: `app/src/test/java/com/myhomechores/app/features/family/ChildConnectionViewModelTest.kt`
- Modify: `app/src/androidTest/java/com/myhomechores/app/ScaffoldScreenTest.kt`

**Interfaces:**
- Consumes: persistent anonymous session, `FamilyGateway.childProfile`, and imported Room profile.
- Produces: child-role gate states `CHECKING`, `NEEDS_CODE`, `NEEDS_HERO`, and `READY`.

- [ ] **Step 1: Write failing child-gate tests**

```kotlin
@Test fun linked_saved_session_skips_code() = runTest {
    gateway.child = RemoteChild("remote-1", "family-1", display_name = "Саша", hero = "BOY")
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(ChildConnectionStage.NEEDS_HERO, viewModel.state.value.stage)
}

@Test fun disconnected_child_returns_to_code() = runTest {
    gateway.child = null
    viewModel.refresh()
    advanceUntilIdle()
    assertEquals(ChildConnectionStage.NEEDS_CODE, viewModel.state.value.stage)
}
```

- [ ] **Step 2: Run the tests and verify failure**

Run: `./gradlew.bat testDevDebugUnitTest --tests "*.ChildConnectionViewModelTest"`

Expected: FAIL because the child gate is missing.

- [ ] **Step 3: Remove automatic creation of `local-child`**

Delete the initialization branch in `ScaffoldViewModel` that silently creates `local-child`. A child profile may now be created only by parent RPC and imported after successful code consumption. Keep existing completion refresh and editable-name logic for a non-null Room child.

- [ ] **Step 4: Implement the child-role gate**

When child role is selected:

1. Wait for Supabase session initialization.
2. If there is no session, show code entry; its ViewModel creates the anonymous session before consume.
3. If there is an anonymous session, call `childProfile()`.
4. Null profile means `NEEDS_CODE`; linked profile with no locally confirmed hero choice means `NEEDS_HERO`; otherwise `READY`.
5. Refresh `childProfile()` on app resume so parent disconnect is observed.

- [ ] **Step 5: Preserve child ownership of dragon selection**

After the code links, show the existing Tom/Lily choice screen. Selecting a dragon updates both Room and a child-safe server RPC `update_child_identity(input_display_name text, input_hero text)` that only permits `children.user_id = auth.uid()` and never accepts `parent_label`. The child may edit the visible name before continuing; the parent’s private label remains unchanged.

- [ ] **Step 6: Update scaffold tests**

Assert that selecting child mode with an unlinked session shows “Код подключения”, a linked child shows dragon selection once, and a ready linked profile shows the existing room. Assert parent mode behavior remains guarded by the parent-auth plan.

- [ ] **Step 7: Run all unit and connected tests**

Run: `./gradlew.bat testDevDebugUnitTest connectedDevDebugAndroidTest`

Expected: PASS.

- [ ] **Step 8: Commit the child gate**

```powershell
git add app/src/main/java/com/myhomechores/app/features/scaffold app/src/main/java/com/myhomechores/app/features/family app/src/test/java/com/myhomechores/app/features/family/ChildConnectionViewModelTest.kt app/src/androidTest/java/com/myhomechores/app/ScaffoldScreenTest.kt supabase/migrations/002_child_invite_onboarding.sql
git commit -m "feat: persist linked child onboarding"
```

### Task 6: Supabase Settings, Full Build, and Two-Device Verification

**Files:**
- Modify: `supabase/README.md`
- Modify: `docs/setup.md`

**Interfaces:**
- Consumes: completed parent auth plan and Tasks 1–5 of this plan.
- Produces: exact external configuration and verification checklist.

- [ ] **Step 1: Document required Supabase settings**

Add:

```text
Authentication > Providers > Anonymous Sign-Ins: enabled
Authentication > Rate Limits > Anonymous sign-ins: retain an explicit safe hourly limit
Database migrations: apply 001_family_data.sql then 002_child_invite_onboarding.sql
```

Document that anonymous users use the authenticated database role and are restricted by explicit JWT `is_anonymous` checks. Recommend CAPTCHA/Turnstile before public release and periodic cleanup of old unlinked anonymous accounts.

- [ ] **Step 2: Run the full local verification**

Run: `./gradlew.bat testDevDebugUnitTest lintDevDebug assembleDevDebug`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run instrumentation verification**

Run: `./gradlew.bat connectedDevDebugAndroidTest`

Expected: all Compose and Room tests PASS.

- [ ] **Step 4: Perform the two-device checklist**

1. Parent signs in, creates child name plus private label, and receives a six-digit code.
2. Child enters the code without knowing the parent password, chooses Tom or Lily, optionally edits the visible name, and enters the room.
3. Reusing the same code fails; a code older than 15 minutes fails; issuing a new code invalidates the previous code.
4. Five bad attempts show a five-minute countdown and block submit.
5. Closing and reopening the child app restores the linked profile without another code.
6. The child never sees the private parent label or other family profiles.
7. Parent disconnects the device; after child app resume/relaunch the code screen returns.
8. Existing task completion and undo actions still work in both parent and child modes.

- [ ] **Step 5: Commit documentation and final verification state**

```powershell
git add supabase/README.md docs/setup.md
git commit -m "docs: add child invite setup and checks"
```
