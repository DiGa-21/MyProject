import { Image, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ModeCard } from '../components/ModeCard';
import { colors, spacing } from '../theme/tokens';

type HomeScreenProps = {
  onOpenChild: () => void;
  onOpenParent: () => void;
};

export function HomeScreen({ onOpenChild, onOpenParent }: HomeScreenProps) {
  return (
    <SafeAreaView edges={['top', 'bottom']} style={styles.safeArea}>
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        contentInsetAdjustmentBehavior="automatic"
      >
        <View style={styles.content}>
          <Image
            accessibilityLabel="Логотип приложения Мой путь"
            resizeMode="contain"
            source={require('../../assets/images/app-logo.png')}
            style={styles.logo}
          />

          <View style={styles.headingGroup}>
            <Text accessibilityRole="header" style={styles.title}>Мои домашние дела</Text>
            <Text style={styles.subtitle}>Выбери, как хочешь войти</Text>
          </View>

          <View style={styles.cards}>
            <ModeCard
              accessibilityLabel="Открыть режим ребёнка"
              marker="✓"
              onPress={onOpenChild}
              subtitle="Дела, звёзды, помощник и награды"
              title="Режим ребёнка"
            />
            <ModeCard
              accessibilityLabel="Открыть режим родителя"
              marker="★"
              onPress={onOpenParent}
              subtitle="Настройка дел и подтверждение результатов"
              title="Режим родителя"
            />
          </View>

          <Text style={styles.footer}>React Native · 0.1</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { backgroundColor: colors.background, flex: 1 },
  scrollContent: { flexGrow: 1, justifyContent: 'center', padding: spacing.lg },
  content: {
    alignItems: 'center',
    alignSelf: 'center',
    maxWidth: 520,
    paddingBottom: spacing.md,
    width: '100%',
  },
  logo: { height: 136, width: 136 },
  headingGroup: { alignItems: 'center', marginTop: spacing.lg },
  title: {
    color: colors.text,
    fontSize: 32,
    fontWeight: '800',
    lineHeight: 40,
    textAlign: 'center',
  },
  subtitle: {
    color: colors.accent,
    fontSize: 18,
    fontWeight: '600',
    lineHeight: 26,
    marginTop: spacing.sm,
    textAlign: 'center',
  },
  cards: { gap: spacing.md, marginTop: spacing.xl, width: '100%' },
  footer: {
    color: colors.textMuted,
    fontSize: 14,
    lineHeight: 20,
    marginTop: spacing.xl,
    textAlign: 'center',
  },
});
