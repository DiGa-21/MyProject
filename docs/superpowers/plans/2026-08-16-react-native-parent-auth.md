# React Native Parent Authentication Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Подключить React Native-клиент к существующему Supabase-проекту и реализовать регистрацию, вход, сохранённую родительскую сессию, выход и восстановление пароля с временным защищённым кабинетом.

**Architecture:** `supabase-js` скрывается за приложенческим `AuthGateway`; `AuthProvider` управляет сессией и предоставляет действия UI. Expo Router оставляет стартовый выбор режима открытым, а родительский кабинет защищает проверкой состояния сессии. Валидация и преобразование ошибок остаются чистыми функциями и тестируются отдельно.

**Tech Stack:** Expo SDK 57.0.0, React Native 0.86.2, React 19.2.3, Expo Router 57, TypeScript 6, `@supabase/supabase-js`, React Native AsyncStorage, `react-native-url-polyfill`, Jest 29, React Native Testing Library 14.

## Global Constraints

- Работать только в ветке `codex/react-native-stage-2`, основанной на `codex/react-native-stage-1`.
- Не изменять и не удалять резервное Kotlin-приложение в `app`.
- Не изменять существующие Supabase migrations, RLS-политики, RPC или таблицы.
- Подтверждение email временно отключено; регистрация должна немедленно создавать сессию.
- Минимальная длина пароля — ровно 6 символов.
- Recovery redirect URL — `myway://reset-password`, не `localhost`.
- Не записывать пароль, access token, refresh token или recovery URL в журналы.
- Не добавлять `.env`, service role key, `node_modules`, сгенерированные `android`/`ios` или signing-файлы в Git.
- Все пользовательские тексты и ошибки — на русском языке.
- Каждый UI-элемент для нажатия — не менее 48 dp; экран прокручивается и учитывает клавиатуру и safe area.
- Использовать TDD: сначала красный тест, затем минимальная реализация, затем зелёный тест.

---

## File Structure

Новые или изменяемые файлы:

- `mobile-react-native/.env.example` — имена двух публичных Expo-переменных без значений.
- `mobile-react-native/package.json` и `package-lock.json` — Supabase и React Native storage/polyfill dependencies.
- `mobile-react-native/app.json` — схема `myway`.
- `mobile-react-native/assets/images/parent-auth-helper.png` — копия утверждённого портрета из Kotlin-ресурсов.
- `mobile-react-native/src/features/auth/config.ts` — чтение и проверка публичной конфигурации.
- `mobile-react-native/src/features/auth/types.ts` — типы сессии, состояния и gateway.
- `mobile-react-native/src/features/auth/validation.ts` — чистая валидация форм.
- `mobile-react-native/src/features/auth/errors.ts` — перевод SDK/сетевых ошибок.
- `mobile-react-native/src/features/auth/recoveryUrl.ts` — безопасный разбор recovery deep link.
- `mobile-react-native/src/features/auth/supabaseClient.ts` — ленивое создание клиента и foreground auto-refresh.
- `mobile-react-native/src/features/auth/SupabaseAuthGateway.ts` — единственное место вызовов Supabase Auth/PostgREST.
- `mobile-react-native/src/features/auth/AuthContext.tsx` — восстановление сессии, auth reducer/provider и действия.
- `mobile-react-native/src/screens/ParentAuthScreen.tsx` — утверждённая страница входа/регистрации.
- `mobile-react-native/src/screens/ParentCabinetScreen.tsx` — временный защищённый кабинет.
- `mobile-react-native/src/screens/ResetPasswordScreen.tsx` — ввод нового пароля.
- `mobile-react-native/src/screens/AuthLoadingScreen.tsx` — стабильный экран инициализации.
- `mobile-react-native/src/app/_layout.tsx` — `AuthProvider` вокруг маршрутов.
- `mobile-react-native/src/app/parent.tsx` — удалить, потому что маршрут становится папкой.
- `mobile-react-native/src/app/parent/index.tsx` — auth gate.
- `mobile-react-native/src/app/parent/auth.tsx` — контейнер страницы авторизации.
- `mobile-react-native/src/app/parent/cabinet.tsx` — защищённый контейнер кабинета.
- `mobile-react-native/src/app/parent/reset-password.tsx` — контейнер нового пароля.
- `mobile-react-native/src/app/reset-password.tsx` — alias для `myway://reset-password`.
- `mobile-react-native/__tests__/auth/*.test.ts?(x)` — unit и UI tests.
- `mobile-react-native/README.md`, `docs/setup.md`, `supabase/README.md` — локальная конфигурация и Dashboard-настройки.

---

### Task 1: Supabase Dependencies and Safe Configuration

**Files:**
- Modify: `mobile-react-native/package.json`
- Modify: `mobile-react-native/package-lock.json`
- Create: `mobile-react-native/.env.example`
- Create: `mobile-react-native/src/features/auth/config.ts`
- Test: `mobile-react-native/__tests__/auth/config.test.ts`

**Interfaces:**
- Produces: `type SupabasePublicConfig = { url: string; publishableKey: string }`.
- Produces: `readSupabasePublicConfig(env?: NodeJS.ProcessEnv): SupabasePublicConfig`.
- Later tasks consume the validated config and never read `process.env` directly.

- [ ] **Step 1: Install Expo-compatible dependencies**

Run from `mobile-react-native`:

```powershell
npx.cmd expo install @supabase/supabase-js @react-native-async-storage/async-storage react-native-url-polyfill
```

Expected: package files contain the three dependencies and the command exits `0`.

- [ ] **Step 2: Write failing configuration tests**

Create `__tests__/auth/config.test.ts`:

```ts
import { readSupabasePublicConfig } from '../../src/features/auth/config';

describe('readSupabasePublicConfig', () => {
  it('returns trimmed public values', () => {
    expect(readSupabasePublicConfig({
      EXPO_PUBLIC_SUPABASE_URL: ' https://example.supabase.co ',
      EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY: ' publishable-key ',
    })).toEqual({
      url: 'https://example.supabase.co',
      publishableKey: 'publishable-key',
    });
  });

  it('throws a readable error when configuration is missing', () => {
    expect(() => readSupabasePublicConfig({})).toThrow(
      'Не настроено подключение к Supabase',
    );
  });
});
```

- [ ] **Step 3: Run the test and verify RED**

Run:

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/config.test.ts
```

Expected: FAIL because `config.ts` does not exist.

- [ ] **Step 4: Implement the minimal configuration reader**

Create `src/features/auth/config.ts`:

```ts
export type SupabasePublicConfig = {
  url: string;
  publishableKey: string;
};

export function readSupabasePublicConfig(
  env: NodeJS.ProcessEnv = process.env,
): SupabasePublicConfig {
  const url = env.EXPO_PUBLIC_SUPABASE_URL?.trim() ?? '';
  const publishableKey = env.EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY?.trim() ?? '';
  if (!url || !publishableKey) {
    throw new Error('Не настроено подключение к Supabase');
  }
  return { url, publishableKey };
}
```

Create `mobile-react-native/.env.example`:

```ini
EXPO_PUBLIC_SUPABASE_URL=
EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY=
```

The actual ignored `.env` is generated locally from the already configured `local.properties` without printing values to the terminal.

- [ ] **Step 5: Run focused tests and typecheck**

Run:

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/config.test.ts
npm.cmd run typecheck
```

Expected: PASS, 2 tests and no TypeScript errors.

- [ ] **Step 6: Verify secrets are not staged**

Run:

```powershell
git status --short
git check-ignore mobile-react-native/.env
git grep -n -E "sb_(secret|service)|EXPO_PUBLIC_SUPABASE_(URL|PUBLISHABLE_KEY)=.+" -- . ":(exclude)mobile-react-native/.env.example"
```

Expected: `.env` is ignored and no server secret or populated environment file is tracked.

- [ ] **Step 7: Commit**

```powershell
git add mobile-react-native/package.json mobile-react-native/package-lock.json mobile-react-native/.env.example mobile-react-native/src/features/auth/config.ts mobile-react-native/__tests__/auth/config.test.ts
git commit -m "chore: configure Supabase for React Native"
```

---

### Task 2: Auth Types, Validation, and Russian Error Mapping

**Files:**
- Create: `mobile-react-native/src/features/auth/types.ts`
- Create: `mobile-react-native/src/features/auth/validation.ts`
- Create: `mobile-react-native/src/features/auth/errors.ts`
- Test: `mobile-react-native/__tests__/auth/validation.test.ts`
- Test: `mobile-react-native/__tests__/auth/errors.test.ts`

**Interfaces:**
- Produces: `ParentUser`, `AuthStatus`, `AuthFieldErrors`, `RegistrationInput`, `SignInInput`, `ValidationResult<T>`.
- Produces: `AuthGateway` with `restoreSession`, `subscribe`, `signUp`, `signIn`, `signOut`, `sendPasswordReset`, `consumeRecoveryUrl`, and `updatePassword`.
- Produces: `validateSignIn`, `validateRegistration`, `validateNewPassword`, `mapAuthError`.

- [ ] **Step 1: Write failing validation tests**

Create `__tests__/auth/validation.test.ts` with assertions for empty fields, malformed email, five-character password, mismatched passwords, trimmed/lowercase email, and valid Russian display name:

```ts
import {
  validateNewPassword,
  validateRegistration,
  validateSignIn,
} from '../../src/features/auth/validation';

describe('auth validation', () => {
  it('normalizes a valid sign-in', () => {
    expect(validateSignIn(' Diana@Example.COM ', '123456').value).toEqual({
      email: 'diana@example.com',
      password: '123456',
    });
  });

  it('requires six password characters', () => {
    expect(validateSignIn('diana@example.com', '12345').errors.password)
      .toBe('Минимум 6 символов');
  });

  it('validates registration name and matching passwords', () => {
    const result = validateRegistration('  Диана  ', 'diana@example.com', '123456', '654321');
    expect(result.errors.passwordRepeat).toBe('Пароли не совпадают');
    expect(validateRegistration('  Диана  ', 'diana@example.com', '123456', '123456').value)
      .toEqual({ displayName: 'Диана', email: 'diana@example.com', password: '123456' });
  });

  it('validates the repeated new password', () => {
    expect(validateNewPassword('123456', '123456').value).toBe('123456');
  });
});
```

- [ ] **Step 2: Write failing error-mapping tests**

Create `__tests__/auth/errors.test.ts`:

```ts
import { mapAuthError } from '../../src/features/auth/errors';

describe('mapAuthError', () => {
  it.each([
    ['Invalid login credentials', 'Неверная почта или пароль'],
    ['User already registered', 'Аккаунт с такой почтой уже существует'],
    ['Network request failed', 'Нет связи с интернетом. Попробуй ещё раз'],
    ['rate limit exceeded', 'Слишком много попыток. Попробуй немного позже'],
  ])('maps %s', (source, expected) => {
    expect(mapAuthError(new Error(source))).toBe(expected);
  });

  it('does not expose an unknown technical message', () => {
    expect(mapAuthError(new Error('internal detail'))).toBe(
      'Не удалось выполнить действие. Попробуй ещё раз',
    );
  });
});
```

- [ ] **Step 3: Run tests and verify RED**

Run:

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/validation.test.ts __tests__/auth/errors.test.ts
```

Expected: FAIL because the modules do not exist.

- [ ] **Step 4: Implement types and pure functions**

Create `types.ts` with the following exact public shapes:

```ts
export type ParentUser = { id: string; email: string; displayName: string };
export type AuthStatus = 'initializing' | 'unauthenticated' | 'authenticated' | 'recovery';
export type AuthFieldErrors = {
  displayName?: string;
  email?: string;
  password?: string;
  passwordRepeat?: string;
  general?: string;
};
export type RegistrationInput = { displayName: string; email: string; password: string };
export type SignInInput = { email: string; password: string };
export type ValidationResult<T> = { value?: T; errors: AuthFieldErrors };
export type AuthSnapshot = { status: AuthStatus; user: ParentUser | null };
export type AuthUnsubscribe = () => void;

export interface AuthGateway {
  restoreSession(): Promise<ParentUser | null>;
  subscribe(listener: (snapshot: AuthSnapshot) => void): AuthUnsubscribe;
  signUp(input: RegistrationInput): Promise<ParentUser>;
  signIn(input: SignInInput): Promise<ParentUser>;
  signOut(): Promise<void>;
  sendPasswordReset(email: string, redirectTo: string): Promise<void>;
  consumeRecoveryUrl(url: string): Promise<boolean>;
  updatePassword(password: string): Promise<ParentUser>;
}
```

Port the already approved validation semantics from Kotlin into `validation.ts`, including `trim`, lowercase email, the regex `^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$`, and all exact Russian messages from the tests.

Implement `mapAuthError` by matching lowercase error messages; return only the approved generic fallback for unknown errors.

- [ ] **Step 5: Run focused tests and typecheck**

Run:

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/validation.test.ts __tests__/auth/errors.test.ts
npm.cmd run typecheck
```

Expected: all focused tests PASS.

- [ ] **Step 6: Commit**

```powershell
git add mobile-react-native/src/features/auth/types.ts mobile-react-native/src/features/auth/validation.ts mobile-react-native/src/features/auth/errors.ts mobile-react-native/__tests__/auth/validation.test.ts mobile-react-native/__tests__/auth/errors.test.ts
git commit -m "feat: add React Native auth contracts"
```

---

### Task 3: Supabase Client, Gateway, and Recovery URL Parser

**Files:**
- Create: `mobile-react-native/src/features/auth/recoveryUrl.ts`
- Create: `mobile-react-native/src/features/auth/supabaseClient.ts`
- Create: `mobile-react-native/src/features/auth/SupabaseAuthGateway.ts`
- Test: `mobile-react-native/__tests__/auth/recoveryUrl.test.ts`
- Test: `mobile-react-native/__tests__/auth/SupabaseAuthGateway.test.ts`

**Interfaces:**
- Consumes: `SupabasePublicConfig`, `AuthGateway`, `ParentUser`.
- Produces: `parseRecoveryUrl(url): RecoveryCredentials | null`.
- Produces: `createSupabaseClient(config): SupabaseClient` and `registerAuthAutoRefresh(client): () => void`.
- Produces: `class SupabaseAuthGateway implements AuthGateway`.

- [ ] **Step 1: Write failing recovery parser tests**

```ts
import { parseRecoveryUrl } from '../../src/features/auth/recoveryUrl';

describe('parseRecoveryUrl', () => {
  it('parses recovery tokens from a hash', () => {
    expect(parseRecoveryUrl('myway://reset-password#access_token=a&refresh_token=r&type=recovery'))
      .toEqual({ kind: 'tokens', accessToken: 'a', refreshToken: 'r' });
  });

  it('parses a PKCE code', () => {
    expect(parseRecoveryUrl('myway://reset-password?code=abc&type=recovery'))
      .toEqual({ kind: 'code', code: 'abc' });
  });

  it('ignores unrelated links', () => {
    expect(parseRecoveryUrl('myway://parent')).toBeNull();
  });
});
```

- [ ] **Step 2: Write a failing gateway test with a structural fake Supabase client**

The fake records `signUp`, `signInWithPassword`, `resetPasswordForEmail`, `setSession`, `exchangeCodeForSession`, `updateUser` and `signOut`. Assert that:

```ts
await gateway.signUp({ displayName: 'Диана', email: 'diana@example.com', password: '123456' });
expect(fake.auth.signUp).toHaveBeenCalledWith({
  email: 'diana@example.com',
  password: '123456',
  options: { data: { display_name: 'Диана' } },
});
```

Also assert the redirect URL passed to reset, recovery token consumption, profile-name fallback from `user_metadata.display_name`, and unsubscribe behavior.

- [ ] **Step 3: Run focused tests and verify RED**

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/recoveryUrl.test.ts __tests__/auth/SupabaseAuthGateway.test.ts
```

Expected: FAIL because parser and gateway do not exist.

- [ ] **Step 4: Implement client creation and foreground refresh**

`supabaseClient.ts` must:

```ts
import 'react-native-url-polyfill/auto';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { createClient, processLock } from '@supabase/supabase-js';
import { AppState, Platform } from 'react-native';

export function createSupabaseClient(config: SupabasePublicConfig) {
  return createClient(config.url, config.publishableKey, {
    auth: {
      ...(Platform.OS !== 'web' ? { storage: AsyncStorage } : {}),
      autoRefreshToken: true,
      persistSession: true,
      detectSessionInUrl: false,
      lock: processLock,
    },
  });
}
```

`registerAuthAutoRefresh` registers exactly one `AppState` listener per provider instance, starts refresh on `active`, stops on background, and returns cleanup.

- [ ] **Step 5: Implement recovery parser and gateway**

`parseRecoveryUrl` must accept both implicit tokens and a PKCE `code`, require `type=recovery`, decode values with `URLSearchParams`, and return `null` for unrelated or malformed links.

`SupabaseAuthGateway` must:

- use `signUp({ options: { data: { display_name } } })`;
- reject a registration result without a session with the readable configuration message about disabled email confirmation;
- use `signInWithPassword` for login;
- use `resetPasswordForEmail(email, { redirectTo })`;
- use `setSession` for token links or `exchangeCodeForSession` for code links;
- use `updateUser({ password })` for a new password;
- load `profiles.display_name` by authenticated user id and fall back to `user.user_metadata.display_name`;
- never log auth payloads;
- throw SDK errors upward for `mapAuthError` in the provider;
- map Supabase `Session` to the app-owned `ParentUser`.

- [ ] **Step 6: Run focused tests, typecheck, and lint**

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/recoveryUrl.test.ts __tests__/auth/SupabaseAuthGateway.test.ts
npm.cmd run typecheck
npm.cmd run lint
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add mobile-react-native/src/features/auth/recoveryUrl.ts mobile-react-native/src/features/auth/supabaseClient.ts mobile-react-native/src/features/auth/SupabaseAuthGateway.ts mobile-react-native/__tests__/auth/recoveryUrl.test.ts mobile-react-native/__tests__/auth/SupabaseAuthGateway.test.ts
git commit -m "feat: add Supabase auth gateway"
```

---

### Task 4: Auth Provider and Session Lifecycle

**Files:**
- Create: `mobile-react-native/src/features/auth/AuthContext.tsx`
- Test: `mobile-react-native/__tests__/auth/AuthContext.test.tsx`

**Interfaces:**
- Consumes: `AuthGateway`, validation-ready input types, `mapAuthError`.
- Produces: `AuthProvider({ children, gateway? })`.
- Produces: `useAuth(): AuthContextValue`.

`AuthContextValue` exact shape:

```ts
type AuthContextValue = {
  status: AuthStatus;
  user: ParentUser | null;
  submitting: boolean;
  error: string | null;
  clearError(): void;
  signUp(input: RegistrationInput): Promise<boolean>;
  signIn(input: SignInInput): Promise<boolean>;
  signOut(): Promise<void>;
  sendPasswordReset(email: string): Promise<boolean>;
  updatePassword(password: string): Promise<boolean>;
};
```

- [ ] **Step 1: Write failing provider tests**

Use a fake gateway and a small probe component. Cover:

- `initializing` until `restoreSession` resolves;
- `unauthenticated` for no restored user;
- `authenticated` for a restored user;
- successful registration/login;
- Russian error on failed request;
- logout;
- recovery URL event and cleanup;
- reset email uses exactly `myway://reset-password`.

Example assertion:

```tsx
const screen = await render(
  <AuthProvider gateway={fakeGateway}>
    <AuthProbe />
  </AuthProvider>,
);
expect(screen.getByText('initializing')).toBeTruthy();
await waitFor(() => expect(screen.getByText('unauthenticated')).toBeTruthy());
```

- [ ] **Step 2: Run the provider test and verify RED**

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/AuthContext.test.tsx
```

Expected: FAIL because `AuthContext.tsx` does not exist.

- [ ] **Step 3: Implement provider initialization and actions**

Implementation requirements:

- one reducer/state object owns `status`, `user`, `submitting`, and `error`;
- `restoreSession()` runs once on mount;
- the gateway subscription and AppState/deep-link subscriptions are cleaned up on unmount;
- stale async results do not update an unmounted provider;
- each action sets `submitting`, clears the previous error, then either updates state or stores `mapAuthError(error)`;
- `sendPasswordReset` returns `true` after success but never reveals whether the email exists;
- recovery link changes status to `recovery` only after the gateway establishes a recovery session;
- configuration failure produces `Не настроено подключение к Supabase` instead of crashing the app.

The production default gateway is created lazily inside the provider from `readSupabasePublicConfig`, `createSupabaseClient`, and `SupabaseAuthGateway`. Tests always inject a fake.

- [ ] **Step 4: Run focused tests and typecheck**

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/AuthContext.test.tsx
npm.cmd run typecheck
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add mobile-react-native/src/features/auth/AuthContext.tsx mobile-react-native/__tests__/auth/AuthContext.test.tsx
git commit -m "feat: manage persistent parent sessions"
```

---

### Task 5: Parent Authentication UI

**Files:**
- Copy: `app/src/main/res/drawable-nodpi/parent_auth_helper.png` → `mobile-react-native/assets/images/parent-auth-helper.png`
- Create: `mobile-react-native/src/screens/ParentAuthScreen.tsx`
- Test: `mobile-react-native/__tests__/auth/ParentAuthScreen.test.tsx`
- Modify: `mobile-react-native/src/theme/tokens.ts`

**Interfaces:**
- Consumes: `useAuth`, `validateSignIn`, `validateRegistration`.
- Produces: `ParentAuthScreen({ onBack })`.

- [ ] **Step 1: Copy the approved binary asset without modifying the original**

```powershell
Copy-Item -LiteralPath "..\app\src\main\res\drawable-nodpi\parent_auth_helper.png" -Destination ".\assets\images\parent-auth-helper.png"
```

Verify both files exist and have the same SHA-256 hash.

- [ ] **Step 2: Write failing UI tests**

Tests must render inside a fake `AuthProvider` and verify:

- title/subtitle/helper accessibility label;
- default sign-in fields;
- switch to registration and all four fields;
- password visibility buttons;
- local errors appear without gateway calls;
- successful valid input calls the gateway once;
- forgot-password UI sends email and displays the neutral success message;
- submit button disables while `submitting`;
- back calls `onBack`.

Example:

```tsx
await fireEvent.press(screen.getByRole('button', { name: 'Регистрация' }));
screen.getByLabelText('Имя');
screen.getByLabelText('Электронная почта');
screen.getByLabelText('Пароль');
screen.getByLabelText('Повтори пароль');
```

- [ ] **Step 3: Run UI tests and verify RED**

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/ParentAuthScreen.test.tsx
```

Expected: FAIL because screen does not exist.

- [ ] **Step 4: Implement the approved responsive screen**

Use only React Native primitives already in the project: `KeyboardAvoidingView`, `ScrollView`, `TextInput`, `Pressable`, `Image`, `ActivityIndicator`, `View`, `Text`, and safe-area context.

Required copies:

- `Кабинет родителя`
- `Войди, чтобы управлять делами семьи`
- `Вход`, `Регистрация`
- `Электронная почта`, `Пароль`, `Имя`, `Повтори пароль`
- `Войти`, `Создать аккаунт`, `Забыли пароль?`
- neutral reset result from the spec.

The helper image is positioned in a fixed portrait area; the teal border is a separate rounded `View` behind the body so it never draws across the ears. Use `resizeMode="contain"`. Do not enlarge the surrounding card beyond the approved layout.

The forgot-password form may replace the fields inside the same card; it must include a clear return action to sign-in and preserve the rest of the page.

- [ ] **Step 5: Run focused tests, typecheck, and lint**

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/ParentAuthScreen.test.tsx
npm.cmd run typecheck
npm.cmd run lint
```

Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add mobile-react-native/assets/images/parent-auth-helper.png mobile-react-native/src/screens/ParentAuthScreen.tsx mobile-react-native/src/theme/tokens.ts mobile-react-native/__tests__/auth/ParentAuthScreen.test.tsx
git commit -m "feat: add parent authentication screen"
```

---

### Task 6: Protected Parent Routes, Cabinet, and Password Reset

**Files:**
- Modify: `mobile-react-native/app.json`
- Modify: `mobile-react-native/src/app/_layout.tsx`
- Delete: `mobile-react-native/src/app/parent.tsx`
- Create: `mobile-react-native/src/app/parent/index.tsx`
- Create: `mobile-react-native/src/app/parent/auth.tsx`
- Create: `mobile-react-native/src/app/parent/cabinet.tsx`
- Create: `mobile-react-native/src/app/parent/reset-password.tsx`
- Create: `mobile-react-native/src/app/reset-password.tsx`
- Create: `mobile-react-native/src/screens/AuthLoadingScreen.tsx`
- Create: `mobile-react-native/src/screens/ParentCabinetScreen.tsx`
- Create: `mobile-react-native/src/screens/ResetPasswordScreen.tsx`
- Test: `mobile-react-native/__tests__/auth/ParentCabinetScreen.test.tsx`
- Test: `mobile-react-native/__tests__/auth/ResetPasswordScreen.test.tsx`
- Test: `mobile-react-native/__tests__/auth/parentRouteDecision.test.ts`

**Interfaces:**
- Consumes: `useAuth` and `validateNewPassword`.
- Produces: `parentRouteFor(status): '/parent/auth' | '/parent/cabinet' | '/parent/reset-password' | null` as a pure testable decision.
- Produces: `ParentCabinetScreen({ user, onSignOut })`.
- Produces: `ResetPasswordScreen({ onSubmit, submitting, error })`.

- [ ] **Step 1: Write failing route-decision and screen tests**

Route decisions:

```ts
expect(parentRouteFor('initializing')).toBeNull();
expect(parentRouteFor('unauthenticated')).toBe('/parent/auth');
expect(parentRouteFor('authenticated')).toBe('/parent/cabinet');
expect(parentRouteFor('recovery')).toBe('/parent/reset-password');
```

Cabinet test verifies `Мой кабинет`, display name, email, `Ты вошла в аккаунт`, and one `Выйти` call.

Reset test verifies two secure fields, six-character validation, mismatch validation, disabled loading button, successful callback, and Russian error copy.

- [ ] **Step 2: Run tests and verify RED**

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth/parentRouteDecision.test.ts __tests__/auth/ParentCabinetScreen.test.tsx __tests__/auth/ResetPasswordScreen.test.tsx
```

Expected: FAIL because routes/screens do not exist.

- [ ] **Step 3: Implement route decision and presentation screens**

`AuthLoadingScreen` uses the app background and centered `ActivityIndicator` plus `Проверяем вход…`.

`ParentCabinetScreen` renders only the temporary content approved in the spec. `ResetPasswordScreen` uses the same form/keyboard/safe-area conventions as `ParentAuthScreen`.

- [ ] **Step 4: Replace the placeholder parent route with folder routes**

`src/app/parent/index.tsx`:

```tsx
const { status } = useAuth();
const target = parentRouteFor(status);
if (!target) return <AuthLoadingScreen />;
return <Redirect href={target} />;
```

`parent/auth.tsx` redirects authenticated users to `/parent/cabinet`. `parent/cabinet.tsx` redirects all non-authenticated users to `/parent` and calls `signOut` from context. `parent/reset-password.tsx` redirects away unless the provider is in recovery or authenticated recovery state.

`reset-password.tsx` re-exports or redirects to the parent reset route so `myway://reset-password` resolves to a real Expo Router route.

Wrap the root `Stack` in `AuthProvider` in `_layout.tsx`. Keep the public `/` and `/child` routes unchanged.

- [ ] **Step 5: Configure the app scheme**

Change only:

```json
"scheme": "myway"
```

Do not change the Android application id in this stage.

- [ ] **Step 6: Run all auth tests, typecheck, and lint**

```powershell
npm.cmd run test:ci -- --runTestsByPath __tests__/auth
npm.cmd run typecheck
npm.cmd run lint
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add mobile-react-native/app.json mobile-react-native/src/app mobile-react-native/src/screens mobile-react-native/__tests__/auth
git commit -m "feat: protect the React Native parent cabinet"
```

---

### Task 7: Documentation, Full Verification, Emulator, and Phone

**Files:**
- Modify: `mobile-react-native/README.md`
- Modify: `docs/setup.md`
- Modify: `supabase/README.md`
- Create: `docs/superpowers/plans/2026-08-16-react-native-parent-auth-verification.md`

**Interfaces:**
- Consumes: completed tasks 1–6.
- Produces: repeatable local setup and a verification record without credentials.

- [ ] **Step 1: Document exact local environment configuration**

Document these keys without values:

```ini
EXPO_PUBLIC_SUPABASE_URL=
EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY=
```

Explain that the publishable key is intended for clients but access to data is controlled by Supabase Auth and RLS; service role key must never be used in the app.

- [ ] **Step 2: Document one-time Supabase Dashboard settings**

Add exact steps:

1. Authentication → Providers → Email → disable mandatory email confirmation for the current family build.
2. Authentication → URL Configuration → Redirect URLs → add `myway://reset-password`.
3. Password minimum length → `6`.

- [ ] **Step 3: Run the full automated verification**

```powershell
npm.cmd run test:ci
npm.cmd run typecheck
npm.cmd run lint
npx.cmd expo export --platform android --output-dir dist-auth-check
```

Expected: all commands exit `0`; report exact test counts.

- [ ] **Step 4: Verify the reserve Kotlin application**

From the repository root:

```powershell
.\gradlew.bat :app:assembleDevDebug
```

Expected: `BUILD SUCCESSFUL` and no Kotlin source diff.

- [ ] **Step 5: Build the React Native Android APK**

Use the ASCII worktree and its Gradle cache:

```powershell
$env:GRADLE_USER_HOME = 'C:\Android\MyWayRNWorktree\.gradle-user'
Set-Location mobile-react-native\android
.\gradlew.bat app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`; APK at `mobile-react-native/android/app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 6: Configure Supabase and verify on emulator**

Without printing values, populate ignored `mobile-react-native/.env` from the already configured local Supabase URL/publishable key. Rebuild/reinstall if environment values changed.

Verify and record:

- registration opens the cabinet immediately;
- app restart restores the cabinet;
- logout returns to sign-in;
- invalid password shows Russian copy;
- reset email shows neutral copy;
- recovery link opens the reset screen rather than `localhost`;
- new password permits login.

- [ ] **Step 7: Verify on a physical Android phone when connected**

Install with ADB using the generated APK. Repeat registration/login persistence, logout, and recovery deep-link checks. Record device and result without email, token, URL query, key, or other private data.

- [ ] **Step 8: Security and repository audit**

```powershell
git status --short
git diff --check
git grep -n -E "sb_(secret|service)|EXPO_PUBLIC_SUPABASE_(URL|PUBLISHABLE_KEY)=.+" -- . ":(exclude)mobile-react-native/.env.example"
git diff codex/react-native-stage-1 -- app supabase/migrations
```

Expected: no secrets, no `.env`, no generated native folders, and no Kotlin or database migration changes.

- [ ] **Step 9: Write verification record and commit**

The record includes commands, exit codes, test counts, APK path/hash, emulator result, phone result or a clear note that the phone was unavailable, and the required Dashboard settings. It must not contain account email or credentials.

```powershell
git add mobile-react-native/README.md docs/setup.md supabase/README.md docs/superpowers/plans/2026-08-16-react-native-parent-auth-verification.md
git commit -m "docs: verify React Native parent authentication"
```

---

## Final Review Checklist

- [ ] Every approved requirement in `docs/superpowers/specs/2026-08-16-react-native-parent-auth-design.md` maps to a task above.
- [ ] Registration creates an immediate Supabase session when email confirmation is disabled.
- [ ] Name is persisted as `display_name` and existing `profiles` trigger remains unchanged.
- [ ] Session restoration does not flash the sign-in form.
- [ ] Cabinet cannot be reached without a valid session.
- [ ] Recovery deep link supports both implicit-token and PKCE-code forms.
- [ ] Recovery redirect is `myway://reset-password` everywhere.
- [ ] No passwords, tokens, recovery links, user emails, keys, or populated `.env` values appear in logs, docs, tests, or Git history.
- [ ] All existing stage-one tests continue to pass.
- [ ] Kotlin backup build continues to pass.
- [ ] React Native Android APK builds and the main auth scenarios work on an emulator.
- [ ] Any physical-phone limitation is reported explicitly rather than inferred.
