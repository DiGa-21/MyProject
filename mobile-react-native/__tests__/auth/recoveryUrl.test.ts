import { parseRecoveryUrl } from '../../src/features/auth/recoveryUrl';

describe('parseRecoveryUrl', () => {
  it('parses recovery tokens from a hash', () => {
    expect(
      parseRecoveryUrl(
        'myway://reset-password#access_token=a%20b&refresh_token=r&type=recovery',
      ),
    ).toEqual({ kind: 'tokens', accessToken: 'a b', refreshToken: 'r' });
  });

  it('parses a PKCE code', () => {
    expect(
      parseRecoveryUrl('myway://reset-password?code=abc%20123&type=recovery'),
    ).toEqual({ kind: 'code', code: 'abc 123' });
  });

  it.each([
    'myway://parent',
    'myway://reset-password?code=abc&type=signup',
    'myway://reset-password#access_token=a&type=recovery',
    'not a url',
  ])('ignores unrelated or malformed link %s', (url) => {
    expect(parseRecoveryUrl(url)).toBeNull();
  });
});
