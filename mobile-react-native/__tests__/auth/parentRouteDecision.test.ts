import { parentRouteFor } from '../../src/features/auth/parentRouteDecision';

describe('parentRouteFor', () => {
  it('waits while session initialization is in progress', () => {
    expect(parentRouteFor('initializing')).toBeNull();
  });

  it('routes every settled auth state', () => {
    expect(parentRouteFor('unauthenticated')).toBe('/parent/auth');
    expect(parentRouteFor('authenticated')).toBe('/parent/cabinet');
    expect(parentRouteFor('recovery')).toBe('/parent/reset-password');
  });
});
