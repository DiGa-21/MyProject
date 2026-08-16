import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, radii, spacing } from '../theme/tokens';

type PlaceholderModeScreenProps = {
  title: string;
  message: string;
  onBack: () => void;
};

export function PlaceholderModeScreen({ title, message, onBack }: PlaceholderModeScreenProps) {
  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
      <View style={styles.container}>
        <Pressable
          accessibilityLabel="Назад"
          accessibilityRole="button"
          hitSlop={8}
          onPress={onBack}
          style={({ pressed }) => [styles.backButton, pressed && styles.pressed]}
        >
          <Text style={styles.backText}>Назад</Text>
        </Pressable>

        <View style={styles.content}>
          <Text accessibilityRole="header" style={styles.title}>{title}</Text>
          <Text style={styles.message}>{message}</Text>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { backgroundColor: colors.background, flex: 1 },
  container: { flex: 1, paddingHorizontal: spacing.lg, paddingVertical: spacing.md },
  backButton: {
    alignItems: 'center',
    alignSelf: 'flex-start',
    borderColor: colors.border,
    borderRadius: radii.button,
    borderWidth: 1,
    justifyContent: 'center',
    minHeight: 48,
    paddingHorizontal: spacing.lg,
  },
  pressed: { opacity: 0.82 },
  backText: { color: colors.text, fontSize: 16, fontWeight: '600' },
  content: {
    alignItems: 'center',
    flex: 1,
    justifyContent: 'center',
    paddingBottom: 64,
  },
  title: {
    color: colors.text,
    fontSize: 30,
    fontWeight: '700',
    lineHeight: 38,
    textAlign: 'center',
  },
  message: {
    color: colors.textMuted,
    fontSize: 17,
    lineHeight: 26,
    marginTop: spacing.md,
    maxWidth: 420,
    textAlign: 'center',
  },
});
