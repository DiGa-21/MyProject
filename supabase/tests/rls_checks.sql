-- Run each block in the Supabase SQL editor while authenticated as the noted role.
-- These checks are intentionally explicit so a parent/child test session can verify the policy boundary.

-- Parent session: this must return the private parent_label column for the parent's family.
select id, display_name, parent_label from public.parent_children;

-- Child session: this must return no parent_label column and only the linked child.
select id, display_name, hero from public.child_profile;

-- Child session: this must affect only the linked child and must reject a different child id.
select public.consume_invite_code('REPLACE_WITH_ONE_TIME_CODE');

-- Any session: the unique key must reject a duplicate completion for one chore/date.
select chore_id, completion_date, count(*)
from public.completions
group by chore_id, completion_date
having count(*) > 1;

-- Parent session: the following should return zero rows for a child outside the parent's family.
select c.id
from public.children c
where c.family_id <> auth.uid();
