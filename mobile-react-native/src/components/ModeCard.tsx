import { Pressable, StyleSheet, Text, View } from 'react-native';

import { colors, radii, spacing } from '../theme/tokens';

type ModeCardProps = {
  title: string;
  subtitle: string;
  marker: string;
  accessibilityLabel: string;
  onPress: () => void;
};

export function ModeCard({ title, subtitle, marker, accessibilityLabel, onPress }: ModeCardProps) {
  return (
    <Pressable
      accessibilityLabel={accessibilityLabel}
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.card, pressed && styles.pressed]}
    >
      <View accessibilityElementsHidden style={styles.marker}>
        <Text style={styles.markerText}>{marker}</Text>
      </View>
      <View style={styles.copy}>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.subtitle}>{subtitle}</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    alignItems: 'center',
    backgroundColor: colors.surface,
    borderRadius: radii.card,
    flexDirection: 'row',
    gap: spacing.md,
    minHeight: 96,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
    width: '100%',
  },
  pressed: { opacity: 0.82 },
  marker: {
    alignItems: 'center',
    backgroundColor: colors.accentSoft,
    borderRadius: 28,
    height: 56,
    justifyContent: 'center',
    width: 56,
  },
  markerText: { color: colors.accent, fontSize: 28, fontWeight: '700' },
  copy: { flex: 1, gap: spacing.xs },
  title: { color: colors.text, fontSize: 20, fontWeight: '700', lineHeight: 26 },
  subtitle: { color: colors.textMuted, fontSize: 16, lineHeight: 23 },
});
