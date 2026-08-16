import { readSupabasePublicConfig } from '../../src/features/auth/config';

describe('readSupabasePublicConfig', () => {
  it('returns trimmed public values', () => {
    expect(
      readSupabasePublicConfig({
        EXPO_PUBLIC_SUPABASE_URL: ' https://example.supabase.co ',
        EXPO_PUBLIC_SUPABASE_PUBLISHABLE_KEY: ' publishable-key ',
      }),
    ).toEqual({
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
