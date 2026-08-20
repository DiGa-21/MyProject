import { useState } from 'react';
import {
  ActivityIndicator,
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

import type { AuthFieldErrors } from '../features/auth/types';
import { validateNewPassword } from '../features/auth/validation';
import { colors, radii, spacing } from '../theme/tokens';

function PasswordInput({
  label,
  value,
  onChangeText,
  error,
}: {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  error?: string;
}) {
  const [visible, setVisible] = useState(false);
  return (
    <View>
      <Text style={styles.label}>{label}</Text>
      <View style={[styles.inputRow, error ? styles.inputError : undefined]}>
        <TextInput
          accessibilityLabel={label}
          autoCapitalize="none"
          autoComplete="new-password"
          onChangeText={onChangeText}
          secureTextEntry={!visible}
          style={styles.input}
          value={value}
        />
        <Pressable
          accessibilityLabel={visible ? 'Скрыть пароль' : 'Показать пароль'}
          accessibilityRole="button"
          onPress={() => setVisible((current) => !current)}
          style={({ pressed }) => [
            styles.visibilityButton,
            pressed ? styles.pressed : undefined,
          ]}>
          <Text style={styles.visibilityText}>{visible ? 'Скрыть' : 'Показать'}</Text>
        </Pressable>
      </View>
      {error ? <Text style={styles.fieldError}>{error}</Text> : null}
    </View>
  );
}

export function ResetPasswordScreen({
  onSubmit,
  submitting,
  error,
}: {
  onSubmit: (password: string) => Promise<boolean>;
  submitting: boolean;
  error: string | null;
}) {
  const [password, setPassword] = useState('');
  const [passwordRepeat, setPasswordRepeat] = useState('');
  const [errors, setErrors] = useState<AuthFieldErrors>({});

  const submit = async () => {
    const result = validateNewPassword(password, passwordRepeat);
    setErrors(result.errors);
    if (result.value) {
      await onSubmit(result.value);
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
            <Text accessibilityRole="header" style={styles.title}>
              Новый пароль
            </Text>
            <Text style={styles.description}>
              Придумай новый пароль длиной не менее 6 символов
            </Text>
            <View style={styles.form}>
              <PasswordInput
                error={errors.password}
                label="Новый пароль"
                onChangeText={setPassword}
                value={password}
              />
              <PasswordInput
                error={errors.passwordRepeat}
                label="Повтори новый пароль"
                onChangeText={setPasswordRepeat}
                value={passwordRepeat}
              />
              {error ? (
                <Text accessibilityLiveRegion="polite" style={styles.requestError}>
                  {error}
                </Text>
              ) : null}
              <Pressable
                accessibilityLabel="Сохранить новый пароль"
                accessibilityRole="button"
                accessibilityState={{ disabled: submitting }}
                disabled={submitting}
                onPress={() => void submit()}
                style={({ pressed }) => [
                  styles.submitButton,
                  submitting ? styles.disabled : undefined,
                  pressed ? styles.pressed : undefined,
                ]}>
                {submitting ? (
                  <ActivityIndicator color={colors.white} />
                ) : (
                  <Text style={styles.submitText}>Сохранить новый пароль</Text>
                )}
              </Pressable>
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
    justifyContent: 'center',
    padding: spacing.lg,
  },
  content: { alignSelf: 'center', maxWidth: 520, width: '100%' },
  title: {
    color: colors.text,
    fontSize: 30,
    fontWeight: '800',
    lineHeight: 38,
    textAlign: 'center',
  },
  description: {
    color: colors.textMuted,
    fontSize: 16,
    lineHeight: 24,
    marginTop: spacing.sm,
    textAlign: 'center',
  },
  form: { gap: spacing.md, marginTop: spacing.xl },
  label: {
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
  inputError: { borderColor: colors.error },
  input: {
    color: colors.text,
    flex: 1,
    fontSize: 16,
    minHeight: 52,
    paddingHorizontal: spacing.md,
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
  requestError: { color: colors.error, fontSize: 14, lineHeight: 21 },
  submitButton: {
    alignItems: 'center',
    backgroundColor: colors.accent,
    borderRadius: radii.button,
    height: 52,
    justifyContent: 'center',
  },
  submitText: { color: colors.white, fontSize: 16, fontWeight: '800' },
  disabled: { opacity: 0.48 },
  pressed: { opacity: 0.72 },
});
