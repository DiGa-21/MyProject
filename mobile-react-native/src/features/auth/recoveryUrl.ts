export type RecoveryCredentials =
  | { kind: 'tokens'; accessToken: string; refreshToken: string }
  | { kind: 'code'; code: string };

export function parseRecoveryUrl(url: string): RecoveryCredentials | null {
  try {
    const parsed = new URL(url);
    if (parsed.protocol !== 'myway:' || parsed.hostname !== 'reset-password') {
      return null;
    }

    const query = parsed.searchParams;
    const hash = new URLSearchParams(parsed.hash.replace(/^#/, ''));
    const type = query.get('type') ?? hash.get('type');
    if (type !== 'recovery') {
      return null;
    }

    const code = query.get('code');
    if (code) {
      return { kind: 'code', code };
    }

    const accessToken = hash.get('access_token');
    const refreshToken = hash.get('refresh_token');
    if (accessToken && refreshToken) {
      return { kind: 'tokens', accessToken, refreshToken };
    }

    return null;
  } catch {
    return null;
  }
}
