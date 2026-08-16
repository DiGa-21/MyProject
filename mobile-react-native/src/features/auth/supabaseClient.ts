import 'react-native-url-polyfill/auto';

import AsyncStorage from '@react-native-async-storage/async-storage';
import { createClient, processLock, type SupabaseClient } from '@supabase/supabase-js';
import { AppState, Platform } from 'react-native';

import type { SupabasePublicConfig } from './config';

export function createSupabaseClient(
  config: SupabasePublicConfig,
): SupabaseClient {
  return createClient(config.url, config.publishableKey, {
    auth: {
      ...(Platform.OS !== 'web' ? { storage: AsyncStorage } : {}),
      autoRefreshToken: true,
      persistSession: true,
      detectSessionInUrl: false,
      lock: processLock,
    },
  });
}

export function registerAuthAutoRefresh(client: SupabaseClient): () => void {
  const updateRefresh = (state: string) => {
    if (state === 'active') {
      client.auth.startAutoRefresh();
    } else {
      client.auth.stopAutoRefresh();
    }
  };

  updateRefresh(AppState.currentState);
  const subscription = AppState.addEventListener('change', updateRefresh);

  return () => {
    subscription.remove();
    client.auth.stopAutoRefresh();
  };
}
