export type SupabasePublicConfig = {
  url: string;
  publishableKey: string;
};

type PublicEnvironment = Record<string, string | undefined>;

export function readSupabasePublicConfig(
  env: PublicEnvironment = process.env,
): SupabasePublicConfig {
  const url = env.EXPO_PUBLIC_SUPABASE_URL?.trim() ?? '';
  const publishableKey =
    env.EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY?.trim() ?? '';

  if (!url || !publishableKey) {
    throw new Error('Не настроено подключение к Supabase');
  }

  return { url, publishableKey };
}
