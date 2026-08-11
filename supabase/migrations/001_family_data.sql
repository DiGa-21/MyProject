create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.children (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references public.profiles(id) on delete cascade,
  user_id uuid unique references auth.users(id) on delete set null,
  display_name text not null,
  parent_label text,
  hero text not null default 'BOY' check (hero in ('BOY', 'GIRL')),
  updated_at timestamptz not null default now()
);

create table if not exists public.chores (
  id uuid primary key default gen_random_uuid(),
  child_id uuid not null references public.children(id) on delete cascade,
  actor text not null default 'CHILD' check (actor in ('CHILD', 'PARENT')),
  title text not null,
  category text not null default 'OTHER',
  reward integer not null default 0 check (reward >= 0),
  hint text not null default '',
  color_argb bigint not null default 4294967295,
  required boolean not null default true,
  updated_at timestamptz not null default now()
);

create table if not exists public.completions (
  id uuid primary key default gen_random_uuid(),
  chore_id uuid not null references public.chores(id) on delete cascade,
  child_id uuid not null references public.children(id) on delete cascade,
  completion_date date not null,
  status text not null default 'PENDING' check (status in ('PENDING', 'CONFIRMED', 'CANCELLED')),
  completed_by uuid references auth.users(id) on delete set null,
  updated_at timestamptz not null default now(),
  unique (chore_id, completion_date)
);

create table if not exists public.rewards (
  id uuid primary key default gen_random_uuid(),
  child_id uuid not null references public.children(id) on delete cascade,
  completion_id uuid not null unique references public.completions(id) on delete cascade,
  stars integer not null default 0 check (stars >= 0),
  fragments integer not null default 0 check (fragments >= 0),
  updated_at timestamptz not null default now()
);

create table if not exists public.invite_codes (
  id uuid primary key default gen_random_uuid(),
  family_id uuid not null references public.profiles(id) on delete cascade,
  child_id uuid not null unique references public.children(id) on delete cascade,
  code_hash text not null unique,
  expires_at timestamptz not null,
  consumed_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists children_family_idx on public.children(family_id);
create index if not exists chores_child_idx on public.chores(child_id);
create index if not exists completions_child_date_idx on public.completions(child_id, completion_date);

alter table public.profiles enable row level security;
alter table public.children enable row level security;
alter table public.chores enable row level security;
alter table public.completions enable row level security;
alter table public.rewards enable row level security;
alter table public.invite_codes enable row level security;

create or replace function public.is_family_parent(target_family uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$ select auth.uid() = target_family $$;

create or replace function public.is_child_user(target_child uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$ select exists (
  select 1 from public.children c where c.id = target_child and c.user_id = auth.uid()
) $$;

drop policy if exists profiles_owner on public.profiles;
create policy profiles_owner on public.profiles
  for all using (id = auth.uid()) with check (id = auth.uid());

drop policy if exists children_parent_read on public.children;
create policy children_parent_read on public.children
  for select using (public.is_family_parent(family_id));
drop policy if exists children_parent_write on public.children;
create policy children_parent_write on public.children
  for all using (public.is_family_parent(family_id)) with check (public.is_family_parent(family_id));
drop policy if exists children_child_read on public.children;
create policy children_child_read on public.children
  for select using (user_id = auth.uid());
drop policy if exists children_child_update on public.children;
create policy children_child_update on public.children
  for update using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists chores_family_read on public.chores;
create policy chores_family_read on public.chores
  for select using (
    exists (select 1 from public.children c where c.id = child_id and (c.family_id = auth.uid() or c.user_id = auth.uid()))
  );
drop policy if exists chores_parent_write on public.chores;
create policy chores_parent_write on public.chores
  for all using (exists (select 1 from public.children c where c.id = child_id and c.family_id = auth.uid()))
  with check (exists (select 1 from public.children c where c.id = child_id and c.family_id = auth.uid()));

drop policy if exists completions_family_read on public.completions;
create policy completions_family_read on public.completions
  for select using (
    exists (select 1 from public.children c where c.id = child_id and (c.family_id = auth.uid() or c.user_id = auth.uid()))
  );
drop policy if exists completions_child_insert on public.completions;
create policy completions_child_insert on public.completions
  for insert with check (public.is_child_user(child_id) and completed_by = auth.uid());
drop policy if exists completions_parent_update on public.completions;
create policy completions_parent_update on public.completions
  for update using (exists (select 1 from public.children c where c.id = child_id and c.family_id = auth.uid()))
  with check (exists (select 1 from public.children c where c.id = child_id and c.family_id = auth.uid()));
drop policy if exists completions_child_update on public.completions;
create policy completions_child_update on public.completions
  for update using (public.is_child_user(child_id))
  with check (public.is_child_user(child_id));

drop policy if exists rewards_family_read on public.rewards;
create policy rewards_family_read on public.rewards
  for select using (exists (select 1 from public.children c where c.id = child_id and (c.family_id = auth.uid() or c.user_id = auth.uid())));
drop policy if exists rewards_parent_write on public.rewards;
create policy rewards_parent_write on public.rewards
  for all using (exists (select 1 from public.children c where c.id = child_id and c.family_id = auth.uid()))
  with check (exists (select 1 from public.children c where c.id = child_id and c.family_id = auth.uid()));

drop policy if exists invite_parent_access on public.invite_codes;
create policy invite_parent_access on public.invite_codes
  for all using (public.is_family_parent(family_id)) with check (public.is_family_parent(family_id));

revoke select on public.children from authenticated;
grant select (id, family_id, user_id, display_name, hero, updated_at) on public.children to authenticated;

create or replace view public.parent_children
as select id, family_id, user_id, display_name, parent_label, hero, updated_at
from public.children
where family_id = auth.uid();

create or replace view public.child_profile
as select id, family_id, user_id, display_name, hero, updated_at
from public.children
where user_id = auth.uid();

grant select on public.parent_children to authenticated;
grant select on public.child_profile to authenticated;

create or replace function public.consume_invite_code(input_code text)
returns public.children
language plpgsql
security definer
set search_path = public
as $$
declare
  invite public.invite_codes;
  linked_child public.children;
begin
  if auth.uid() is null then raise exception 'authentication required'; end if;
  select * into invite
  from public.invite_codes
  where code_hash = encode(digest(lower(trim(input_code)), 'sha256'), 'hex')
    and consumed_at is null
    and expires_at > now()
  for update;
  if not found then raise exception 'invite code is invalid or expired'; end if;

  update public.children
  set user_id = auth.uid(), updated_at = now()
  where id = invite.child_id and user_id is null
  returning * into linked_child;
  if not found then raise exception 'invite code has already been used'; end if;

  update public.invite_codes set consumed_at = now() where id = invite.id;
  return linked_child;
end;
$$;

revoke all on function public.consume_invite_code(text) from public;
grant execute on function public.consume_invite_code(text) to authenticated;
