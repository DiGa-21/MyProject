# React Native Stage 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Создать рядом с резервным Kotlin-приложением отдельный Expo/React Native-клиент с адаптивным стартовым экраном «Мой путь» и рабочими переходами в режим ребёнка и режим родителя на Android.

**Architecture:** Новый клиент живёт в `mobile-react-native` и не зависит от Gradle-проекта в корне. Expo Router управляет тремя маршрутами, а экраны состоят из небольших презентационных компонентов с TypeScript-интерфейсами. На этом этапе нет Supabase и локальной базы; существующий логотип копируется как отдельный ресурс нового клиента.

**Tech Stack:** Expo SDK 57, React Native, TypeScript, Expo Router, React Native Safe Area Context, Jest Expo, React Native Testing Library, Android Studio/ADB.

## Global Constraints

- Существующие `app`, `supabase`, Kotlin- и Gradle-файлы нельзя удалять, переносить или переписывать.
- Новый клиент создаётся только в `mobile-react-native`.
- Целевая проверяемая платформа первого этапа — Android; код должен оставаться совместимым с будущей iOS-сборкой.
- Supabase, пользовательские данные и `.env` в первом этапе не подключаются.
- Локальные секреты, `node_modules`, `.expo`, сгенерированные `android`/`ios` и ключи подписи не попадают в Git.
- Текст интерфейса — русский.
- Контент не перекрывается верхней и нижней системными областями Android.
- React Native-приложение имеет отдельный Android package `com.myway.reactnative`, чтобы устанавливаться рядом с резервной Kotlin-версией.

---

## File Map

- `mobile-react-native/app/_layout.tsx` — корневая навигация и общая системная конфигурация.
- `mobile-react-native/app/index.tsx` — маршрут стартового экрана.
- `mobile-react-native/app/child.tsx` — временный маршрут режима ребёнка.
- `mobile-react-native/app/parent.tsx` — временный маршрут режима родителя.
- `mobile-react-native/src/screens/HomeScreen.tsx` — презентационный стартовый экран без зависимости от роутера.
- `mobile-react-native/src/screens/PlaceholderModeScreen.tsx` — общий вид двух временных экранов.
- `mobile-react-native/src/components/ModeCard.tsx` — доступная карточка выбора режима.
- `mobile-react-native/src/theme/tokens.ts` — цвета, интервалы, радиусы и размеры.
- `mobile-react-native/assets/images/app-logo.png` — копия существующего логотипа.
- `mobile-react-native/__tests__/HomeScreen.test.tsx` — отображение и действия стартового экрана.
- `mobile-react-native/__tests__/PlaceholderModeScreen.test.tsx` — отображение временного экрана и возврат.
- `mobile-react-native/jest.config.js` — Jest Expo.
- `mobile-react-native/.gitignore` — локальные файлы нового клиента.
- `.gitignore` — дополнительная защита корневого репозитория от React Native-артефактов.
- `README.md` — команды запуска обеих реализаций и объяснение резервной Kotlin-версии.

---

### Task 1: Подготовить Node.js и создать изолированный Expo-каркас

**Files:**
- Create: `mobile-react-native/**` через официальный `create-expo-app`
- Modify: `.gitignore`
- Modify: `mobile-react-native/.gitignore`
- Modify: `mobile-react-native/app.json`
- Modify: `mobile-react-native/package.json`
- Modify: `mobile-react-native/tsconfig.json`

**Interfaces:**
- Consumes: установленный Android SDK и эмулятор из существующей среды Android Studio.
- Produces: проект Expo SDK 57 с командами `npm run typecheck`, `npm test`, `npm run lint`, `npm run android`.

- [ ] **Step 1: Зафиксировать отсутствие Node.js и проверить Android SDK**

Run:

```powershell
node --version
npm --version
Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
```

Expected: первые две команды сейчас сообщают, что команда не найдена; последняя возвращает `True`.

- [ ] **Step 2: Получить разрешение пользователя и установить Node.js LTS**

Run after explicit approval:

```powershell
winget install --id OpenJS.NodeJS.LTS --exact
```

Закрыть и открыть новое окно PowerShell, затем проверить:

```powershell
node --version
npm --version
```

Expected: Node.js не ниже `22.13.0` (минимум Expo SDK 57); npm выводит номер версии.

- [ ] **Step 3: Создать Expo SDK 57 проект**

Run from repository root:

```powershell
npx create-expo-app@latest mobile-react-native --template default@sdk-57
```

Expected: создан `mobile-react-native/package.json`, зависимости установлены без ошибок.

- [ ] **Step 4: Удалить только демонстрационные маршруты шаблона и создать минимальные каталоги**

Перед удалением проверить точные пути:

```powershell
Resolve-Path -LiteralPath "C:\Android\MyWay\New project\mobile-react-native"
Get-ChildItem -LiteralPath "C:\Android\MyWay\New project\mobile-react-native\app" -Recurse -ErrorAction SilentlyContinue
Get-ChildItem -LiteralPath "C:\Android\MyWay\New project\mobile-react-native\src\app" -Recurse -ErrorAction SilentlyContinue
```

Удалять через патчи только демонстрационные маршруты внутри `mobile-react-native/app` или `mobile-react-native/src/app`. Не затрагивать корневую папку `app` Kotlin-проекта. После очистки использовать утверждённую структуру `mobile-react-native/app` для маршрутов; `mobile-react-native/src` оставить только для `components`, `screens` и `theme`. Создать также `__tests__` и `assets/images`.

- [ ] **Step 5: Настроить идентификаторы приложения**

В `mobile-react-native/app.json` сохранить параметры шаблона и установить:

```json
{
  "expo": {
    "name": "Мой путь RN",
    "slug": "my-way-react-native",
    "scheme": "mywayrn",
    "version": "0.1.0",
    "orientation": "portrait",
    "android": {
      "package": "com.myway.reactnative",
      "edgeToEdgeEnabled": true
    }
  }
}
```

Если шаблон содержит дополнительные необходимые ключи `plugins`, `experiments`, `icon` или `splash`, не удалять их; объединить конфигурацию.

- [ ] **Step 6: Добавить команды проверки**

В `mobile-react-native/package.json` добавить в `scripts`, сохранив команды шаблона:

```json
{
  "typecheck": "tsc --noEmit",
  "test": "jest",
  "test:ci": "jest --runInBand"
}
```

Установить тестовые зависимости версиями, совместимыми с SDK 57:

```powershell
Set-Location "C:\Android\MyWay\New project\mobile-react-native"
npx expo install jest-expo jest @types/jest @testing-library/react-native "--" --dev
```

В `mobile-react-native/tsconfig.json` сохранить настройки шаблона и добавить типы Jest:

```json
{
  "compilerOptions": {
    "types": ["jest"]
  }
}
```

- [ ] **Step 7: Защитить локальные файлы**

В `mobile-react-native/.gitignore` должны быть точные строки:

```gitignore
node_modules/
.expo/
.expo-shared/
dist/
web-build/
android/
ios/
.env
.env.*
!.env.example
*.jks
*.keystore
```

В корневой `.gitignore` добавить защиту на случай ошибочного удаления вложенного файла:

```gitignore
# React Native / Expo
**/node_modules/
**/.expo/
**/.expo-shared/
mobile-react-native/android/
mobile-react-native/ios/
```

- [ ] **Step 8: Проверить исходный каркас**

Run:

```powershell
Set-Location "C:\Android\MyWay\New project\mobile-react-native"
npm run typecheck
npx expo config --type public
```

Expected: обе команды завершаются с кодом `0`; конфигурация показывает package `com.myway.reactnative` и не содержит секретов.

- [ ] **Step 9: Сделать отдельный коммит каркаса**

```powershell
git add .gitignore mobile-react-native
git commit -m "chore: scaffold React Native client"
```

Перед коммитом проверить `git diff --cached --name-only`: там не должно быть `local.properties`, `.env`, `node_modules`, `android`, `ios` или файлов резервного Kotlin-клиента.

---

### Task 2: Добавить тему и доступные переиспользуемые компоненты

**Files:**
- Create: `mobile-react-native/src/theme/tokens.ts`
- Create: `mobile-react-native/src/components/ModeCard.tsx`
- Create: `mobile-react-native/src/screens/PlaceholderModeScreen.tsx`
- Create: `mobile-react-native/__tests__/PlaceholderModeScreen.test.tsx`
- Create: `mobile-react-native/jest.config.js`

**Interfaces:**
- Consumes: React Native `Pressable`, `Text`, `View`; `SafeAreaView` из `react-native-safe-area-context`.
- Produces: `ModeCardProps` и `PlaceholderModeScreenProps`, используемые маршрутами в Task 3.

- [ ] **Step 1: Настроить Jest Expo**

Create `mobile-react-native/jest.config.js`:

```javascript
module.exports = {
  preset: 'jest-expo',
  testMatch: ['**/__tests__/**/*.test.ts?(x)'],
  clearMocks: true,
};
```

- [ ] **Step 2: Написать падающий тест временного экрана**

Create `mobile-react-native/__tests__/PlaceholderModeScreen.test.tsx`:

```tsx
import { fireEvent, render } from '@testing-library/react-native';
import { PlaceholderModeScreen } from '../src/screens/PlaceholderModeScreen';

describe('PlaceholderModeScreen', () => {
  it('shows child copy and returns back', () => {
    const onBack = jest.fn();
    const screen = render(
      <PlaceholderModeScreen
        title="Режим ребёнка"
        message="Подключение по коду появится на следующем этапе"
        onBack={onBack}
      />,
    );

    screen.getByText('Режим ребёнка');
    screen.getByText('Подключение по коду появится на следующем этапе');
    fireEvent.press(screen.getByRole('button', { name: 'Назад' }));
    expect(onBack).toHaveBeenCalledTimes(1);
  });
});
```

- [ ] **Step 3: Запустить тест и подтвердить ожидаемое падение**

Run:

```powershell
npm run test:ci -- PlaceholderModeScreen.test.tsx
```

Expected: FAIL, потому что `PlaceholderModeScreen` ещё не существует.

- [ ] **Step 4: Создать дизайн-токены**

Create `mobile-react-native/src/theme/tokens.ts`:

```ts
export const colors = {
  background: '#FFF8EF',
  surface: '#F3ECFA',
  accent: '#188DA8',
  accentSoft: '#D9F4F5',
  text: '#211E23',
  textMuted: '#665F68',
  border: '#D8CED9',
  white: '#FFFFFF',
} as const;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
} as const;

export const radii = {
  button: 22,
  card: 28,
  image: 32,
} as const;
```

- [ ] **Step 5: Реализовать `ModeCard`**

Create `mobile-react-native/src/components/ModeCard.tsx` with this public interface:

```tsx
export type ModeCardProps = {
  title: string;
  subtitle: string;
  marker: string;
  accessibilityLabel: string;
  onPress: () => void;
};
```

Implementation requirements:

- root is `Pressable` with `accessibilityRole="button"`;
- `accessibilityLabel` is passed unchanged;
- minimum height is `96`;
- pressed state lowers opacity to `0.82`;
- marker sits in a circular `56×56` soft-accent surface;
- title and subtitle wrap rather than clip.

- [ ] **Step 6: Реализовать `PlaceholderModeScreen`**

Create `mobile-react-native/src/screens/PlaceholderModeScreen.tsx` with:

```tsx
export type PlaceholderModeScreenProps = {
  title: string;
  message: string;
  onBack: () => void;
};
```

Use `SafeAreaView` as the root, center the copy in available space, and render a `Pressable` named `Назад` at the top. Do not import Expo Router inside this presentation component.

- [ ] **Step 7: Запустить тест и TypeScript**

Run:

```powershell
npm run test:ci -- PlaceholderModeScreen.test.tsx
npm run typecheck
```

Expected: PASS and exit code `0`.

- [ ] **Step 8: Commit**

```powershell
git add mobile-react-native/src mobile-react-native/__tests__/PlaceholderModeScreen.test.tsx mobile-react-native/jest.config.js
git commit -m "feat: add React Native design primitives"
```

---

### Task 3: Реализовать стартовый экран и маршруты

**Files:**
- Create: `mobile-react-native/assets/images/app-logo.png`
- Create: `mobile-react-native/src/screens/HomeScreen.tsx`
- Create: `mobile-react-native/__tests__/HomeScreen.test.tsx`
- Modify: `mobile-react-native/app/_layout.tsx`
- Modify: `mobile-react-native/app/index.tsx`
- Create: `mobile-react-native/app/child.tsx`
- Create: `mobile-react-native/app/parent.tsx`

**Interfaces:**
- Consumes: `ModeCard`, `PlaceholderModeScreen`, design tokens, `router.push`, `router.back`.
- Produces: рабочие маршруты `/`, `/child`, `/parent` и презентационный `HomeScreenProps`.

- [ ] **Step 1: Скопировать утверждённый логотип**

Source:

```text
app/src/main/res/drawable-nodpi/app_logo.png
```

Destination:

```text
mobile-react-native/assets/images/app-logo.png
```

Проверить, что исходник и копия имеют размер `1024×1024`, а исходный файл не изменился.

- [ ] **Step 2: Написать падающие тесты стартового экрана**

Create `mobile-react-native/__tests__/HomeScreen.test.tsx`:

```tsx
import { fireEvent, render } from '@testing-library/react-native';
import { HomeScreen } from '../src/screens/HomeScreen';

describe('HomeScreen', () => {
  it('shows the product and both modes', () => {
    const screen = render(
      <HomeScreen onOpenChild={() => {}} onOpenParent={() => {}} />,
    );

    screen.getByText('Мои домашние дела');
    screen.getByRole('button', { name: 'Открыть режим ребёнка' });
    screen.getByRole('button', { name: 'Открыть режим родителя' });
    screen.getByText('React Native · 0.1');
  });

  it('reports the selected mode', () => {
    const onOpenChild = jest.fn();
    const onOpenParent = jest.fn();
    const screen = render(
      <HomeScreen onOpenChild={onOpenChild} onOpenParent={onOpenParent} />,
    );

    fireEvent.press(screen.getByRole('button', { name: 'Открыть режим ребёнка' }));
    fireEvent.press(screen.getByRole('button', { name: 'Открыть режим родителя' }));

    expect(onOpenChild).toHaveBeenCalledTimes(1);
    expect(onOpenParent).toHaveBeenCalledTimes(1);
  });
});
```

- [ ] **Step 3: Запустить тест и подтвердить падение**

```powershell
npm run test:ci -- HomeScreen.test.tsx
```

Expected: FAIL because `HomeScreen` does not exist.

- [ ] **Step 4: Реализовать презентационный стартовый экран**

Create `mobile-react-native/src/screens/HomeScreen.tsx` with this interface:

```tsx
export type HomeScreenProps = {
  onOpenChild: () => void;
  onOpenParent: () => void;
};
```

Required hierarchy:

```tsx
<SafeAreaView>
  <ScrollView contentContainerStyle={{ flexGrow: 1 }}>
    <View>{/* centered 136×136 app logo */}</View>
    <Text>Мои домашние дела</Text>
    <ModeCard
      title="Режим ребёнка"
      subtitle="Дела, звёзды, помощник и награды"
      marker="✓"
      accessibilityLabel="Открыть режим ребёнка"
      onPress={onOpenChild}
    />
    <ModeCard
      title="Режим родителя"
      subtitle="Настройка дел и подтверждение результатов"
      marker="★"
      accessibilityLabel="Открыть режим родителя"
      onPress={onOpenParent}
    />
    <Text>React Native · 0.1</Text>
  </ScrollView>
</SafeAreaView>
```

Use `resizeMode="contain"`, `maxWidth: 520`, centered horizontal margins, and bottom padding not less than `spacing.lg`.

- [ ] **Step 5: Подключить Expo Router**

`mobile-react-native/app/index.tsx`:

```tsx
import { router } from 'expo-router';
import { HomeScreen } from '../src/screens/HomeScreen';

export default function HomeRoute() {
  return (
    <HomeScreen
      onOpenChild={() => router.push('/child')}
      onOpenParent={() => router.push('/parent')}
    />
  );
}
```

`mobile-react-native/app/child.tsx`:

```tsx
import { router } from 'expo-router';
import { PlaceholderModeScreen } from '../src/screens/PlaceholderModeScreen';

export default function ChildRoute() {
  return (
    <PlaceholderModeScreen
      title="Режим ребёнка"
      message="Подключение по коду появится на следующем этапе"
      onBack={() => router.back()}
    />
  );
}
```

`mobile-react-native/app/parent.tsx` uses the same component with title `Режим родителя` and message `Регистрация и вход появятся на следующем этапе`.

`mobile-react-native/app/_layout.tsx`:

```tsx
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <StatusBar style="dark" />
      <Stack screenOptions={{ headerShown: false }} />
    </SafeAreaProvider>
  );
}
```

- [ ] **Step 6: Запустить тесты и проверки типов**

```powershell
npm run test:ci
npm run typecheck
npm run lint
```

Expected: all commands exit `0`.

- [ ] **Step 7: Commit**

```powershell
git add mobile-react-native/app mobile-react-native/src/screens mobile-react-native/assets/images mobile-react-native/__tests__/HomeScreen.test.tsx
git commit -m "feat: add React Native mode selection"
```

---

### Task 4: Проверить Android-сборку и сохранность Kotlin-приложения

**Files:**
- Modify: `README.md`
- Generated but ignored: `mobile-react-native/android/**`
- Evidence only, not committed: emulator screenshot and build logs.

**Interfaces:**
- Consumes: Expo routes from Task 3, existing Android SDK and Pixel 6 API 34 emulator.
- Produces: verified Android launch, navigation evidence, and documented repeatable commands.

- [ ] **Step 1: Проверить JavaScript bundle до нативной сборки**

```powershell
Set-Location "C:\Android\MyWay\New project\mobile-react-native"
npx expo export --platform android --output-dir dist
```

Expected: Android bundle exported without Metro or asset errors; `dist` remains ignored.

- [ ] **Step 2: Запустить известный рабочий эмулятор**

В Android Studio выбрать `Pixel_6_API_34`. Проверить:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

Expected: one `emulator-...` row with status `device`, not `offline`.

- [ ] **Step 3: Собрать и установить React Native-клиент**

```powershell
Set-Location "C:\Android\MyWay\New project\mobile-react-native"
npx expo run:android
```

Expected: Gradle build succeeds, package `com.myway.reactnative` installs and the start screen opens.

- [ ] **Step 4: Выполнить ручную проверку**

Checklist:

1. Логотип не обрезан.
2. Заголовок и обе карточки полностью видны.
3. Нижний текст не перекрывается системной панелью.
4. Режим ребёнка открывается и возвращается назад.
5. Режим родителя открывается и возвращается назад.
6. Системная кнопка Android «Назад» работает.
7. Быстрые повторные нажатия не приводят к падению.

- [ ] **Step 5: Проверить, что резервный Kotlin-клиент не сломан**

Run from repository root:

```powershell
.\gradlew.bat :app:assembleDevDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Добавить инструкцию запуска в README**

Add a section with exact commands:

````markdown
## React Native-клиент

Новая кроссплатформенная версия находится в `mobile-react-native`.

```powershell
cd "C:\Android\MyWay\New project\mobile-react-native"
npm install
npx expo run:android
```

Резервная Kotlin-версия остаётся в папке `app` и собирается командой:

```powershell
.\gradlew.bat :app:assembleDevDebug
```
````

- [ ] **Step 7: Выполнить финальную автоматическую проверку**

```powershell
Set-Location "C:\Android\MyWay\New project\mobile-react-native"
npm run typecheck
npm run test:ci
npm run lint
npx expo export --platform android --output-dir dist
Set-Location "C:\Android\MyWay\New project"
.\gradlew.bat :app:assembleDevDebug
git status --short
```

Expected: all checks pass. `git status --short` shows only intended source/docs changes and the three pre-existing diagnostic files; it does not show secrets, dependencies, `mobile-react-native/android` or `mobile-react-native/ios`.

- [ ] **Step 8: Commit documentation and Android polish**

```powershell
git add README.md mobile-react-native
git commit -m "test: verify React Native Android scaffold"
```

Перед коммитом inspect staged names and verify that generated native folders and secrets are absent.

---

## Final Acceptance Checklist

- [ ] `mobile-react-native` is an independent Expo SDK 57 application.
- [ ] Kotlin app files remain unchanged and its `assembleDevDebug` succeeds.
- [ ] Home screen matches the approved «Мой путь» structure.
- [ ] Child and parent buttons navigate to the correct placeholders.
- [ ] In-app and Android system back navigation work.
- [ ] Safe areas prevent status/navigation bar overlap.
- [ ] TypeScript, Jest, lint, Expo Android export and native Android build pass.
- [ ] No `.env`, Supabase keys, `local.properties`, `node_modules`, generated native projects or signing keys are staged.
- [ ] React Native and Kotlin applications can coexist on the same Android device.
