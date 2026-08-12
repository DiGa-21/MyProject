begin;
select plan(11);

-- Run after 001_family_data.sql and 002_child_invite_onboarding.sql in a local Supabase database.
-- The UUIDs below are replaced by local test users in CI/setup scripts.

select has_function('public', 'create_child_profile', array['text', 'text'], 'parent profile RPC exists');
select has_function('public', 'create_child_invite', array['uuid'], 'invite RPC exists');
select has_function('public', 'consume_child_invite', array['text'], 'consume RPC exists');
select has_function('public', 'disconnect_child_device', array['uuid'], 'disconnect RPC exists');
select has_function('public', 'update_child_identity', array['text', 'text'], 'child identity RPC exists');
select has_function('public', 'parent_child_progress', array['uuid', 'date'], 'parent progress RPC exists');
select has_function('public', 'set_child_completion_as_parent', array['uuid', 'text', 'date', 'boolean'], 'parent completion RPC exists');
select has_function('public', 'set_child_completion_as_child', array['uuid', 'text', 'date', 'boolean'], 'child completion RPC exists');
select has_table('public', 'invite_rate_limits', 'rate-limit table exists');
select col_is_null('public', 'invite_codes', 'consumed_at', 'unused invite has nullable consumed time');
select ok(
  not exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'child_profile' and column_name = 'parent_label'
  ),
  'child view never exposes private parent label'
);

select * from finish();
rollback;
