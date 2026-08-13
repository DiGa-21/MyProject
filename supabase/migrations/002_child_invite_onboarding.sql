create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if coalesce(new.is_anonymous, false) is false then
    insert into public.profiles(id, display_name)
    values (new.id, coalesce(new.raw_user_meta_data->>'display_name', ''))
    on conflict (id) do update set display_name = excluded.display_name, updated_at = now();
  end if;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

create table if not exists public.invite_rate_limits (
  rate_key text primary key,
  failed_attempts integer not null default 0 check (failed_attempts >= 0),
  locked_until timestamptz,
  updated_at timestamptz not null default now()
);

alter table public.chores add column if not exists client_key text;
create unique index if not exists chores_child_client_key_idx
  on public.chores(child_id, client_key) where client_key is not null;

alter table public.invite_rate_limits enable row level security;
revoke all on public.invite_rate_limits from anon, authenticated;
revoke all on public.invite_codes from anon, authenticated;
revoke insert, update, delete on public.children from anon, authenticated;
drop policy if exists children_child_update on public.children;

create or replace function public.is_permanent_parent()
returns boolean
language sql
stable
security invoker
as $$
  select auth.uid() is not null
     and coalesce((auth.jwt()->>'is_anonymous')::boolean, false) is false;
$$;

create or replace function public.is_family_parent(target_family uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.is_permanent_parent() and auth.uid() = target_family;
$$;

drop function if exists public.consume_invite_code(text);

create or replace function public.create_child_profile(
  input_display_name text,
  input_parent_label text default null
)
returns table(
  id uuid,
  family_id uuid,
  user_id uuid,
  display_name text,
  parent_label text,
  hero text,
  updated_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
declare
  clean_name text := trim(input_display_name);
begin
  if not public.is_permanent_parent() then
    raise exception 'parent authentication required';
  end if;
  if clean_name = '' or length(clean_name) > 30 then
    raise exception 'invalid child name';
  end if;

  return query
  insert into public.children(family_id, display_name, parent_label, hero)
  values (auth.uid(), clean_name, nullif(trim(input_parent_label), ''), 'BOY')
  returning children.id, children.family_id, children.user_id, children.display_name,
            children.parent_label, children.hero, children.updated_at;
end;
$$;

create or replace function public.create_child_invite(input_child_id uuid)
returns table(code text, expires_at timestamptz)
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  generated_code text;
  expiry timestamptz := now() + interval '15 minutes';
  random_bytes bytea;
  attempt integer := 0;
begin
  if not public.is_permanent_parent() then
    raise exception 'parent authentication required';
  end if;
  if not exists (
    select 1 from public.children
    where children.id = input_child_id and children.family_id = auth.uid()
  ) then
    raise exception 'child not found';
  end if;

  loop
    attempt := attempt + 1;
    random_bytes := gen_random_bytes(4);
    generated_code := lpad((
      ((get_byte(random_bytes, 0)::bigint * 16777216 + get_byte(random_bytes, 1)::bigint * 65536 +
        get_byte(random_bytes, 2)::bigint * 256 + get_byte(random_bytes, 3)::bigint) % 900000) + 100000
    )::text, 6, '0');
    begin
      insert into public.invite_codes(family_id, child_id, code_hash, expires_at, consumed_at, created_at)
      values (
        auth.uid(), input_child_id,
        encode(digest(generated_code, 'sha256'), 'hex'),
        expiry, null, now()
      )
      on conflict (child_id) do update
        set family_id = excluded.family_id,
            code_hash = excluded.code_hash,
            expires_at = excluded.expires_at,
            consumed_at = null,
            created_at = now();
      exit;
    exception when unique_violation then
      if attempt >= 8 then raise; end if;
    end;
  end loop;

  return query select generated_code, expiry;
end;
$$;

create or replace function public.consume_child_invite(input_code text)
returns table(
  status text,
  child_id uuid,
  family_id uuid,
  display_name text,
  hero text,
  retry_after_seconds integer
)
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  caller uuid := auth.uid();
  request_headers jsonb := coalesce(nullif(current_setting('request.headers', true), '')::jsonb, '{}'::jsonb);
  limiter_key text;
  rate public.invite_rate_limits;
  invite public.invite_codes;
  linked public.children;
  failures integer;
begin
  if caller is null or coalesce((auth.jwt()->>'is_anonymous')::boolean, false) is false then
    raise exception 'anonymous child authentication required';
  end if;

  limiter_key := encode(digest(
    coalesce(
      nullif(request_headers->>'cf-connecting-ip', ''),
      nullif(request_headers->>'fly-client-ip', ''),
      nullif(request_headers->>'x-real-ip', ''),
      caller::text
    ),
    'sha256'
  ), 'hex');

  insert into public.invite_rate_limits(rate_key)
  values (limiter_key)
  on conflict (rate_key) do nothing;

  select * into rate from public.invite_rate_limits
  where rate_key = limiter_key for update;

  if rate.locked_until is not null and rate.locked_until > now() then
    return query select 'RATE_LIMITED', null::uuid, null::uuid, null::text, null::text,
      greatest(1, ceil(extract(epoch from rate.locked_until - now()))::integer);
    return;
  end if;

  select * into invite from public.invite_codes
  where code_hash = encode(digest(trim(input_code), 'sha256'), 'hex')
    and consumed_at is null and expires_at > now()
  for update;

  if not found then
    failures := case when rate.locked_until is not null and rate.locked_until <= now()
      then 1 else rate.failed_attempts + 1 end;
    if failures >= 5 then
      update public.invite_rate_limits
      set failed_attempts = 0, locked_until = now() + interval '5 minutes', updated_at = now()
      where rate_key = limiter_key;
      return query select 'RATE_LIMITED', null::uuid, null::uuid, null::text, null::text, 300;
    else
      update public.invite_rate_limits
      set failed_attempts = failures, locked_until = null, updated_at = now()
      where rate_key = limiter_key;
      return query select 'INVALID', null::uuid, null::uuid, null::text, null::text, 0;
    end if;
    return;
  end if;

  update public.children
  set user_id = caller, updated_at = now()
  where children.id = invite.child_id and children.user_id is null
  returning * into linked;
  if not found then
    return query select 'INVALID', null::uuid, null::uuid, null::text, null::text, 0;
    return;
  end if;

  update public.invite_codes set consumed_at = now() where id = invite.id;
  return query select 'LINKED', linked.id, linked.family_id, linked.display_name, linked.hero, 0;
end;
$$;

create or replace function public.disconnect_child_device(input_child_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_permanent_parent() then
    raise exception 'parent authentication required';
  end if;
  update public.children set user_id = null, updated_at = now()
  where id = input_child_id and family_id = auth.uid();
  if not found then raise exception 'child not found'; end if;
end;
$$;

create or replace function public.update_child_identity(input_display_name text, input_hero text)
returns table(
  id uuid,
  family_id uuid,
  user_id uuid,
  display_name text,
  hero text,
  updated_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
declare
  clean_name text := trim(input_display_name);
begin
  if auth.uid() is null or coalesce((auth.jwt()->>'is_anonymous')::boolean, false) is false then
    raise exception 'anonymous child authentication required';
  end if;
  if clean_name = '' or length(clean_name) > 30 or input_hero not in ('BOY', 'GIRL') then
    raise exception 'invalid child identity';
  end if;
  return query
  update public.children
  set display_name = clean_name, hero = input_hero, updated_at = now()
  where children.user_id = auth.uid()
  returning children.id, children.family_id, children.user_id, children.display_name,
            children.hero, children.updated_at;
end;
$$;

create or replace function public.ensure_default_child_chores(input_child_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (
    select 1 from public.children c
    where c.id = input_child_id and (c.family_id = auth.uid() or c.user_id = auth.uid())
  ) then raise exception 'child access required'; end if;

  insert into public.chores(child_id, client_key, title, category, reward, hint, color_argb, required)
  values
    (input_child_id, 'teeth', 'Почистить зубы', 'Здоровье', 2, 'Утром и вечером', 4292478442, true),
    (input_child_id, 'bed', 'Заправить кровать', 'Дом', 2, 'Начать день с порядка', 4292660223, true),
    (input_child_id, 'homework', 'Сделать уроки', 'Учёба', 5, 'Проверить задания в дневнике', 4293455615, true),
    (input_child_id, 'reading', 'Почитать 15 минут', 'Учёба', 3, 'Выбери любую интересную книгу', 4293455615, false),
    (input_child_id, 'walk', 'Погулять', 'Здоровье', 4, 'Минимум 30 минут на свежем воздухе', 4294961340, false),
    (input_child_id, 'table', 'Помочь накрыть на стол', 'Дом', 3, 'Небольшая помощь семье', 4292660223, false)
  on conflict (child_id, client_key) where client_key is not null do nothing;
end;
$$;

create or replace function public.parent_child_progress(input_child_id uuid, input_date date)
returns table(
  client_key text, title text, category text, reward integer, hint text, required boolean,
  completion_id uuid, status text
)
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_permanent_parent() or not exists (
    select 1 from public.children c where c.id = input_child_id and c.family_id = auth.uid()
  ) then raise exception 'parent access required'; end if;
  perform public.ensure_default_child_chores(input_child_id);
  return query
  select ch.client_key, ch.title, ch.category, ch.reward, ch.hint, ch.required,
         co.id, co.status
  from public.chores ch
  left join public.completions co
    on co.chore_id = ch.id and co.completion_date = input_date
  where ch.child_id = input_child_id and ch.actor = 'CHILD'
  order by ch.required desc, ch.updated_at, ch.client_key;
end;
$$;

create or replace function public.set_child_completion_as_parent(
  input_child_id uuid,
  input_chore_key text,
  input_date date,
  input_completed boolean
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare target_chore uuid;
begin
  if not public.is_permanent_parent() or not exists (
    select 1 from public.children c where c.id = input_child_id and c.family_id = auth.uid()
  ) then raise exception 'parent access required'; end if;
  perform public.ensure_default_child_chores(input_child_id);
  select id into target_chore from public.chores
    where child_id = input_child_id and client_key = input_chore_key;
  if target_chore is null then raise exception 'chore not found'; end if;

  insert into public.completions(chore_id, child_id, completion_date, status, completed_by, updated_at)
  values (target_chore, input_child_id, input_date,
          case when input_completed then 'PENDING' else 'CANCELLED' end,
          auth.uid(), now())
  on conflict (chore_id, completion_date) do update
    set status = excluded.status, completed_by = excluded.completed_by, updated_at = now();
end;
$$;

create or replace function public.set_child_completion_as_child(
  input_child_id uuid,
  input_chore_key text,
  input_date date,
  input_completed boolean
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare linked_child uuid; target_chore uuid;
begin
  if auth.uid() is null or coalesce((auth.jwt()->>'is_anonymous')::boolean, false) is false then
    raise exception 'anonymous child authentication required';
  end if;
  select id into linked_child from public.children
    where id = input_child_id and user_id = auth.uid();
  if linked_child is null then raise exception 'linked child required'; end if;
  perform public.ensure_default_child_chores(linked_child);
  select id into target_chore from public.chores
    where child_id = linked_child and client_key = input_chore_key;
  if target_chore is null then raise exception 'chore not found'; end if;

  insert into public.completions(chore_id, child_id, completion_date, status, completed_by, updated_at)
  values (target_chore, linked_child, input_date,
          case when input_completed then 'PENDING' else 'CANCELLED' end,
          auth.uid(), now())
  on conflict (chore_id, completion_date) do update
    set status = excluded.status, completed_by = excluded.completed_by, updated_at = now();
end;
$$;

drop view if exists public.child_profile;
create view public.child_profile with (security_invoker = true) as
select id, family_id, user_id, display_name, hero, updated_at
from public.children
where user_id = auth.uid();

grant select on public.child_profile to authenticated;
revoke all on function public.create_child_profile(text, text) from public;
revoke all on function public.create_child_invite(uuid) from public;
revoke all on function public.consume_child_invite(text) from public;
revoke all on function public.disconnect_child_device(uuid) from public;
revoke all on function public.update_child_identity(text, text) from public;
revoke all on function public.ensure_default_child_chores(uuid) from public;
revoke all on function public.parent_child_progress(uuid, date) from public;
revoke all on function public.set_child_completion_as_parent(uuid, text, date, boolean) from public;
revoke all on function public.set_child_completion_as_child(uuid, text, date, boolean) from public;
grant execute on function public.create_child_profile(text, text) to authenticated;
grant execute on function public.create_child_invite(uuid) to authenticated;
grant execute on function public.consume_child_invite(text) to authenticated;
grant execute on function public.disconnect_child_device(uuid) to authenticated;
grant execute on function public.update_child_identity(text, text) to authenticated;
grant execute on function public.ensure_default_child_chores(uuid) to authenticated;
grant execute on function public.parent_child_progress(uuid, date) to authenticated;
grant execute on function public.set_child_completion_as_parent(uuid, text, date, boolean) to authenticated;
grant execute on function public.set_child_completion_as_child(uuid, text, date, boolean) to authenticated;
