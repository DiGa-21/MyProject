import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import type { ParentUser } from '../features/auth/types';
import { colors, radii, spacing } from '../theme/tokens';

export function ParentCabinetScreen({
  user,
  onSignOut,
}: {
  user: ParentUser;
  onSignOut: () => void;
}) {
  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.content}>
          <Text accessibilityRole="header" style={styles.title}>
            Мой кабинет
          </Text>
          <View style={styles.card}>
            <Text style={styles.greeting}>Ты вошла в аккаунт</Text>
            <Text style={styles.name}>{user.displayName}</Text>
            <Text style={styles.email}>{user.email}</Text>
            <Text style={styles.note}>
              Полный кабинет с делами семьи появится на следующем этапе.
            </Text>
          </View>
          <Pressable
            accessibilityRole="button"
            onPress={onSignOut}
            style={({ pressed }) => [
              styles.signOutButton,
              pressed ? styles.pressed : undefined,
            ]}>
            <Text style={styles.signOutText}>Выйти</Text>
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
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
  card: {
    backgroundColor: colors.surface,
    borderRadius: radii.card,
    marginTop: spacing.lg,
    padding: spacing.lg,
  },
  greeting: {
    color: colors.accent,
    fontSize: 16,
    fontWeight: '700',
    lineHeight: 24,
  },
  name: {
    color: colors.text,
    fontSize: 26,
    fontWeight: '800',
    lineHeight: 34,
    marginTop: spacing.sm,
  },
  email: {
    color: colors.textMuted,
    fontSize: 16,
    lineHeight: 24,
    marginTop: spacing.xs,
  },
  note: {
    color: colors.textMuted,
    fontSize: 15,
    lineHeight: 23,
    marginTop: spacing.lg,
  },
  signOutButton: {
    alignItems: 'center',
    borderColor: colors.accent,
    borderRadius: radii.button,
    borderWidth: 2,
    height: 52,
    justifyContent: 'center',
    marginTop: spacing.lg,
  },
  signOutText: { color: colors.accent, fontSize: 16, fontWeight: '800' },
  pressed: { opacity: 0.72 },
});
