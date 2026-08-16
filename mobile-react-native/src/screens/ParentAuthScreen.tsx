import { useState } from 'react';
import {
  ActivityIndicator,
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAuth } from '../features/auth/AuthContext';
import type { AuthFieldErrors } from '../features/auth/types';
import {
  validateRegistration,
  validateSignIn,
} from '../features/auth/validation';
import { colors, radii, spacing } from '../theme/tokens';

type AuthMode = 'signIn' | 'registration' | 'reset';

type ParentAuthScreenProps = {
  onBack: () => void;
};

type AuthFieldProps = {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  error?: string;
  password?: boolean;
  autoComplete?: 'email' | 'name' | 'password' | 'new-password';
};

function AuthField({
  label,
  value,
  onChangeText,
  error,
  password = false,
  autoComplete,
}: AuthFieldProps) {
  const [visible, setVisible] = useState(false);
  return (
    <View style={styles.fieldGroup}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <View style={[styles.inputRow, error ? styles.inputRowError : undefined]}>
        <TextInput
          accessibilityLabel={label}
          autoCapitalize={label === 'Имя' ? 'words' : 'none'}
          autoComplete={autoComplete}
          autoCorrect={false}
          keyboardType={label === 'Электронная почта' ? 'email-address' : 'default'}
          onChangeText={onChangeText}
          secureTextEntry={password && !visible}
          style={styles.input}
          value={value}
        />
        {password ? (
          <Pressable
            accessibilityLabel={visible ? 'Скрыть пароль' : 'Показать пароль'}
            accessibilityRole="button"
            hitSlop={4}
            onPress={() => setVisible((current) => !current)}
            style={({ pressed }) => [
              styles.visibilityButton,
              pressed ? styles.pressed : undefined,
            ]}>
            <Text style={styles.visibilityText}>{visible ? 'Скрыть' : 'Показать'}</Text>
          </Pressable>
        ) : null}
      </View>
      {error ? (
        <Text accessibilityLiveRegion="polite" style={styles.fieldError}>
          {error}
        </Text>
      ) : null}
    </View>
  );
}

export function ParentAuthScreen({ onBack }: ParentAuthScreenProps) {
  const auth = useAuth();
  const [mode, setMode] = useState<AuthMode>('signIn');
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordRepeat, setPasswordRepeat] = useState('');
  const [errors, setErrors] = useState<AuthFieldErrors>({});
  const [resetSent, setResetSent] = useState(false);
  const primaryActionLabel =
    mode === 'signIn'
      ? 'Войти'
      : mode === 'registration'
        ? 'Создать аккаунт'
        : 'Отправить письмо';

  const changeMode = (next: AuthMode) => {
    auth.clearError();
    setErrors({});
    setResetSent(false);
    setMode(next);
  };

  const submit = async () => {
    auth.clearError();
    setResetSent(false);
    if (mode === 'signIn') {
      const result = validateSignIn(email, password);
      setErrors(result.errors);
      if (result.value) {
        await auth.signIn(result.value);
      }
      return;
    }

    if (mode === 'registration') {
      const result = validateRegistration(
        displayName,
        email,
        password,
        passwordRepeat,
      );
      setErrors(result.errors);
      if (result.value) {
        await auth.signUp(result.value);
      }
      return;
    }

    const result = validateSignIn(email, '123456');
    setErrors({ email: result.errors.email });
    if (!result.errors.email) {
      const sent = await auth.sendPasswordReset(email.trim().toLowerCase());
      setResetSent(sent);
    }
  };

  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.flex}>
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled">
          <View style={styles.content}>
            <Pressable
              accessibilityLabel="Назад"
              accessibilityRole="button"
              onPress={onBack}
              style={({ pressed }) => [
                styles.backButton,
                pressed ? styles.pressed : undefined,
              ]}>
              <Text style={styles.backText}>Назад</Text>
            </Pressable>

            <View style={styles.helperArea}>
              <View style={styles.helperFrame} />
              <Image
                accessibilityLabel="Помощник родителя"
                resizeMode="contain"
                source={require('../../assets/images/parent-auth-helper.png')}
                style={styles.helperImage}
              />
            </View>

            <Text accessibilityRole="header" style={styles.title}>
              Кабинет родителя
            </Text>
            <Text style={styles.subtitle}>Войди, чтобы управлять делами семьи</Text>

            {mode !== 'reset' ? (
              <View style={styles.tabs}>
                <Pressable
                  accessibilityRole="button"
                  accessibilityState={{ selected: mode === 'signIn' }}
                  onPress={() => changeMode('signIn')}
                  style={({ pressed }) => [
                    styles.tab,
                    mode === 'signIn' ? styles.tabSelected : undefined,
                    pressed ? styles.pressed : undefined,
                  ]}>
                  <Text
                    style={mode === 'signIn' ? styles.tabTextSelected : styles.tabText}>
                    Вход
                  </Text>
                </Pressable>
                <Pressable
                  accessibilityRole="button"
                  accessibilityState={{ selected: mode === 'registration' }}
                  onPress={() => changeMode('registration')}
                  style={({ pressed }) => [
                    styles.tab,
                    mode === 'registration' ? styles.tabSelected : undefined,
                    pressed ? styles.pressed : undefined,
                  ]}>
                  <Text
                    style={
                      mode === 'registration' ? styles.tabTextSelected : styles.tabText
                    }>
                    Регистрация
                  </Text>
                </Pressable>
              </View>
            ) : (
              <View style={styles.resetHeading}>
                <Text style={styles.resetTitle}>Восстановление пароля</Text>
                <Text style={styles.resetDescription}>
                  Укажи почту, которую использовала при регистрации
                </Text>
              </View>
            )}

            <View style={styles.form}>
              {mode === 'registration' ? (
                <AuthField
                  autoComplete="name"
                  error={errors.displayName}
                  label="Имя"
                  onChangeText={setDisplayName}
                  value={displayName}
                />
              ) : null}
              <AuthField
                autoComplete="email"
                error={errors.email}
                label="Электронная почта"
                onChangeText={setEmail}
                value={email}
              />
              {mode !== 'reset' ? (
                <AuthField
                  autoComplete={mode === 'registration' ? 'new-password' : 'password'}
                  error={errors.password}
                  label="Пароль"
                  onChangeText={setPassword}
                  password
                  value={password}
                />
              ) : null}
              {mode === 'registration' ? (
                <AuthField
                  autoComplete="new-password"
                  error={errors.passwordRepeat}
                  label="Повтори пароль"
                  onChangeText={setPasswordRepeat}
                  password
                  value={passwordRepeat}
                />
              ) : null}

              {auth.error ? (
                <Text accessibilityLiveRegion="polite" style={styles.generalError}>
                  {auth.error}
                </Text>
              ) : null}
              {resetSent ? (
                <Text accessibilityLiveRegion="polite" style={styles.successMessage}>
                  Если аккаунт с такой почтой существует, мы отправили письмо
                </Text>
              ) : null}

              <Pressable
                accessibilityLabel={primaryActionLabel}
                accessibilityRole="button"
                accessibilityState={{ disabled: auth.submitting }}
                disabled={auth.submitting}
                onPress={() => void submit()}
                style={({ pressed }) => [
                  styles.primaryButton,
                  auth.submitting ? styles.disabled : undefined,
                  pressed ? styles.pressed : undefined,
                ]}>
                {auth.submitting ? (
                  <ActivityIndicator color={colors.white} />
                ) : (
                  <Text style={styles.primaryButtonText}>{primaryActionLabel}</Text>
                )}
              </Pressable>

              {mode === 'signIn' ? (
                <Pressable
                  accessibilityRole="button"
                  onPress={() => changeMode('reset')}
                  style={({ pressed }) => [
                    styles.linkButton,
                    pressed ? styles.pressed : undefined,
                  ]}>
                  <Text style={styles.linkText}>Забыли пароль?</Text>
                </Pressable>
              ) : null}
              {mode === 'reset' ? (
                <Pressable
                  accessibilityRole="button"
                  onPress={() => changeMode('signIn')}
                  style={({ pressed }) => [
                    styles.linkButton,
                    pressed ? styles.pressed : undefined,
                  ]}>
                  <Text style={styles.linkText}>Вернуться ко входу</Text>
                </Pressable>
              ) : null}
            </View>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  safeArea: { backgroundColor: colors.background, flex: 1 },
  scrollContent: {
    flexGrow: 1,
    paddingBottom: spacing.lg,
    paddingHorizontal: spacing.md,
  },
  content: {
    alignSelf: 'center',
    maxWidth: 520,
    paddingTop: spacing.sm,
    width: '100%',
  },
  backButton: {
    alignItems: 'center',
    alignSelf: 'flex-start',
    borderColor: colors.border,
    borderRadius: radii.button,
    borderWidth: 1,
    height: 48,
    justifyContent: 'center',
    paddingHorizontal: spacing.md,
  },
  backText: { color: colors.text, fontSize: 16, fontWeight: '600' },
  helperArea: {
    alignSelf: 'center',
    height: 188,
    marginTop: spacing.sm,
    position: 'relative',
    width: 188,
  },
  helperFrame: {
    backgroundColor: colors.white,
    borderColor: '#17BCAF',
    borderRadius: 22,
    borderWidth: 3,
    bottom: 0,
    height: 124,
    left: 20,
    position: 'absolute',
    width: 148,
  },
  helperImage: {
    bottom: -1,
    height: 188,
    left: 0,
    position: 'absolute',
    width: 188,
  },
  title: {
    color: colors.text,
    fontSize: 28,
    fontWeight: '800',
    lineHeight: 36,
    marginTop: spacing.md,
    textAlign: 'center',
  },
  subtitle: {
    color: colors.textMuted,
    fontSize: 16,
    lineHeight: 24,
    textAlign: 'center',
  },
  tabs: {
    backgroundColor: colors.lavender,
    borderRadius: radii.button,
    flexDirection: 'row',
    marginTop: spacing.md,
    padding: 4,
  },
  tab: {
    alignItems: 'center',
    borderRadius: 18,
    flex: 1,
    height: 48,
    justifyContent: 'center',
  },
  tabSelected: { backgroundColor: colors.accent },
  tabText: { color: colors.accent, fontSize: 15, fontWeight: '700' },
  tabTextSelected: { color: colors.white, fontSize: 15, fontWeight: '700' },
  form: { gap: spacing.sm, marginTop: spacing.md },
  fieldGroup: { width: '100%' },
  fieldLabel: {
    color: colors.textMuted,
    fontSize: 13,
    lineHeight: 18,
    marginBottom: spacing.xs,
    marginLeft: spacing.sm,
  },
  inputRow: {
    alignItems: 'center',
    backgroundColor: colors.inputBackground,
    borderColor: colors.border,
    borderRadius: 8,
    borderWidth: 1.5,
    flexDirection: 'row',
    minHeight: 54,
  },
  inputRowError: { borderColor: colors.error },
  input: {
    color: colors.text,
    flex: 1,
    fontSize: 16,
    minHeight: 52,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  visibilityButton: {
    alignItems: 'center',
    height: 48,
    justifyContent: 'center',
    minWidth: 78,
    paddingHorizontal: spacing.sm,
  },
  visibilityText: { color: colors.accent, fontSize: 13, fontWeight: '700' },
  fieldError: {
    color: colors.error,
    fontSize: 13,
    lineHeight: 18,
    marginLeft: spacing.sm,
    marginTop: spacing.xs,
  },
  generalError: {
    color: colors.error,
    fontSize: 14,
    lineHeight: 20,
    paddingHorizontal: spacing.sm,
  },
  successMessage: {
    backgroundColor: colors.accentSoft,
    borderRadius: 12,
    color: colors.text,
    fontSize: 14,
    lineHeight: 21,
    padding: spacing.md,
  },
  primaryButton: {
    alignItems: 'center',
    backgroundColor: colors.accent,
    borderRadius: radii.button,
    height: 52,
    justifyContent: 'center',
    marginTop: spacing.xs,
    paddingHorizontal: spacing.md,
  },
  primaryButtonText: { color: colors.white, fontSize: 16, fontWeight: '800' },
  linkButton: {
    alignItems: 'center',
    alignSelf: 'center',
    justifyContent: 'center',
    minHeight: 48,
    paddingHorizontal: spacing.md,
  },
  linkText: { color: colors.accent, fontSize: 14, fontWeight: '700' },
  resetHeading: { marginTop: spacing.md },
  resetTitle: {
    color: colors.text,
    fontSize: 20,
    fontWeight: '800',
    lineHeight: 28,
    textAlign: 'center',
  },
  resetDescription: {
    color: colors.textMuted,
    fontSize: 14,
    lineHeight: 21,
    marginTop: spacing.xs,
    textAlign: 'center',
  },
  disabled: { opacity: 0.48 },
  pressed: { opacity: 0.72 },
});
