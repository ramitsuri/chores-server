-- Dummy data for local testing. Dates are relative to "now" so filters like dueDateWithinDays stay meaningful.
-- Every member's login key is: test-key
--
-- ID scheme (easy to type in curl):
--   members      aaaaaaaa-0000-0000-0000-00000000000N
--   houses       bbbbbbbb-0000-0000-0000-00000000000N
--   tasks        cccccccc-0000-0000-0000-0000000000NN
--   assignments  dddddddd-0000-0000-0000-0000000000NN
--   member↔house eeeeeeee-0000-0000-0000-0000000000NN

-- LocalDateTime as stored by LocalDateTimeConverter (ISO_LOCAL_DATE_TIME, UTC)
create function pg_temp.ldt(offset_interval interval) returns varchar language sql as $$
    select to_char(date_trunc('hour', now() at time zone 'utc') + offset_interval, 'YYYY-MM-DD"T"HH24:MI:SS')
$$;
-- Instant as stored by InstantConverter (ISO_INSTANT)
create function pg_temp.inst(offset_interval interval) returns varchar language sql as $$
    select to_char((now() at time zone 'utc') + offset_interval, 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
$$;

begin;

-- bcrypt hash of "test-key"
\set key '$2a$10$KJx2joxdvAx2ysPe0cMbUe433dikw0hGxtgQajmSPUbiB13lX8X6W'

-- access: 1 = READ_HOUSE_WRITE_OWN, 2 = READ_HOUSE_WRITE_HOUSE, 3 = READ_ALL_WRITE_ALL
insert into members (id, name, "createdDate", key, access) values
    ('aaaaaaaa-0000-0000-0000-000000000001', 'Admin', pg_temp.inst('-90 days'), :'key', 3),
    ('aaaaaaaa-0000-0000-0000-000000000002', 'Alice', pg_temp.inst('-90 days'), :'key', 2),
    ('aaaaaaaa-0000-0000-0000-000000000003', 'Bob',   pg_temp.inst('-60 days'), :'key', 1),
    ('aaaaaaaa-0000-0000-0000-000000000004', 'Carol', pg_temp.inst('-30 days'), :'key', 1);

-- activeStatus: 1 = ACTIVE, 2 = INACTIVE, 3 = PAUSED
insert into houses (id, name, "memberId", "createdDate", "activeStatus") values
    ('bbbbbbbb-0000-0000-0000-000000000001', 'Home',   'aaaaaaaa-0000-0000-0000-000000000001', pg_temp.inst('-90 days'), 1),
    ('bbbbbbbb-0000-0000-0000-000000000002', 'Cabin',  'aaaaaaaa-0000-0000-0000-000000000002', pg_temp.inst('-60 days'), 3),
    ('bbbbbbbb-0000-0000-0000-000000000003', 'Office', 'aaaaaaaa-0000-0000-0000-000000000004', pg_temp.inst('-30 days'), 1);

-- Admin: all houses. Alice & Bob: Home + Cabin. Carol: Office only (useful for cross-house access checks)
insert into memberassignments (id, "memberId", "houseId") values
    ('eeeeeeee-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000001'),
    ('eeeeeeee-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000002'),
    ('eeeeeeee-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000001', 'bbbbbbbb-0000-0000-0000-000000000003'),
    ('eeeeeeee-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000001'),
    ('eeeeeeee-0000-0000-0000-000000000005', 'aaaaaaaa-0000-0000-0000-000000000002', 'bbbbbbbb-0000-0000-0000-000000000002'),
    ('eeeeeeee-0000-0000-0000-000000000006', 'aaaaaaaa-0000-0000-0000-000000000003', 'bbbbbbbb-0000-0000-0000-000000000001'),
    ('eeeeeeee-0000-0000-0000-000000000007', 'aaaaaaaa-0000-0000-0000-000000000003', 'bbbbbbbb-0000-0000-0000-000000000002'),
    ('eeeeeeee-0000-0000-0000-000000000008', 'aaaaaaaa-0000-0000-0000-000000000004', 'bbbbbbbb-0000-0000-0000-000000000003');

-- repeatUnit: 0 NONE, 1 DAY, 2 WEEK, 3 MONTH, 4 HOUR, 5 YEAR, 6 ON_COMPLETE
insert into tasks (id, name, description, "dueDate", "repeatValue", "repeatUnit", "repeatEndDate",
                   "houseId", "memberId", "rotateMember", "createdDate", "activeStatus") values
    ('cccccccc-0000-0000-0000-000000000001', 'Dishes',       'Load and run dishwasher', pg_temp.ldt('-10 days'), 1, 1, null,
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002', true,  pg_temp.inst('-30 days'), 1),
    ('cccccccc-0000-0000-0000-000000000002', 'Laundry',      'Wash, dry, fold',         pg_temp.ldt('-7 days'),  1, 2, null,
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000003', false, pg_temp.inst('-30 days'), 1),
    ('cccccccc-0000-0000-0000-000000000003', 'Vacuum',       'All rooms',               pg_temp.ldt('-20 days'), 1, 3, null,
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002', false, pg_temp.inst('-30 days'), 1),
    ('cccccccc-0000-0000-0000-000000000004', 'Take out trash', 'Bins out on the curb',  pg_temp.ldt('-3 days'),  1, 2, null,
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', true,  pg_temp.inst('-30 days'), 1),
    ('cccccccc-0000-0000-0000-000000000005', 'Water plants', 'When soil is dry',        pg_temp.ldt('-2 days'),  1, 6, null,
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000003', false, pg_temp.inst('-30 days'), 1),
    ('cccccccc-0000-0000-0000-000000000006', 'Clean gutters', 'One-off, inactive task', pg_temp.ldt('-40 days'), 0, 0, null,
     'bbbbbbbb-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000001', false, pg_temp.inst('-60 days'), 2),
    ('cccccccc-0000-0000-0000-000000000007', 'Check heating', 'Cabin (paused house)',   pg_temp.ldt('-14 days'), 1, 2, null,
     'bbbbbbbb-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000002', false, pg_temp.inst('-30 days'), 1),
    ('cccccccc-0000-0000-0000-000000000008', 'Restock coffee', 'Office kitchen',        pg_temp.ldt('-7 days'),  1, 2, null,
     'bbbbbbbb-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000004', false, pg_temp.inst('-20 days'), 1);

-- statusType: 1 TODO, 2 IN_PROGRESS, 3 DONE. createType: 1 MANUAL, 2 AUTO
insert into taskassignments (id, "statusType", "statusDate", "taskId", "memberId", "dueDate",
                             "createdDate", "createType", "statusByMember") values
    -- Dishes (daily, Alice/rotating)
    ('dddddddd-0000-0000-0000-000000000001', 3, pg_temp.inst('-2 days'), 'cccccccc-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002', pg_temp.ldt('-2 days'), pg_temp.inst('-3 days'), 2, 'aaaaaaaa-0000-0000-0000-000000000002'),
    ('dddddddd-0000-0000-0000-000000000002', 3, pg_temp.inst('-1 days'), 'cccccccc-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000003', pg_temp.ldt('-1 days'), pg_temp.inst('-2 days'), 2, 'aaaaaaaa-0000-0000-0000-000000000003'),
    ('dddddddd-0000-0000-0000-000000000003', 1, pg_temp.inst('-1 days'), 'cccccccc-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000002', pg_temp.ldt('0'),       pg_temp.inst('-1 days'), 2, null),
    ('dddddddd-0000-0000-0000-000000000004', 1, pg_temp.inst('0'),       'cccccccc-0000-0000-0000-000000000001', 'aaaaaaaa-0000-0000-0000-000000000003', pg_temp.ldt('1 day'),   pg_temp.inst('0'),       2, null),
    -- Laundry (weekly, Bob)
    ('dddddddd-0000-0000-0000-000000000005', 3, pg_temp.inst('-6 days'), 'cccccccc-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000003', pg_temp.ldt('-7 days'), pg_temp.inst('-8 days'), 2, 'aaaaaaaa-0000-0000-0000-000000000003'),
    ('dddddddd-0000-0000-0000-000000000006', 2, pg_temp.inst('-1 hour'), 'cccccccc-0000-0000-0000-000000000002', 'aaaaaaaa-0000-0000-0000-000000000003', pg_temp.ldt('0'),       pg_temp.inst('-1 days'), 2, 'aaaaaaaa-0000-0000-0000-000000000003'),
    -- Vacuum (monthly, Alice) - overdue
    ('dddddddd-0000-0000-0000-000000000007', 1, pg_temp.inst('-25 days'), 'cccccccc-0000-0000-0000-000000000003', 'aaaaaaaa-0000-0000-0000-000000000002', pg_temp.ldt('-20 days'), pg_temp.inst('-25 days'), 2, null),
    -- Trash (weekly, Admin/rotating)
    ('dddddddd-0000-0000-0000-000000000008', 3, pg_temp.inst('-3 days'), 'cccccccc-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000001', pg_temp.ldt('-3 days'), pg_temp.inst('-4 days'), 2, 'aaaaaaaa-0000-0000-0000-000000000002'),
    ('dddddddd-0000-0000-0000-000000000009', 1, pg_temp.inst('0'),       'cccccccc-0000-0000-0000-000000000004', 'aaaaaaaa-0000-0000-0000-000000000002', pg_temp.ldt('4 days'),  pg_temp.inst('0'),       2, null),
    -- Water plants (on complete, Bob)
    ('dddddddd-0000-0000-0000-000000000010', 1, pg_temp.inst('-2 days'), 'cccccccc-0000-0000-0000-000000000005', 'aaaaaaaa-0000-0000-0000-000000000003', pg_temp.ldt('-2 days'), pg_temp.inst('-2 days'), 2, null),
    -- Check heating (paused Cabin house)
    ('dddddddd-0000-0000-0000-000000000011', 1, pg_temp.inst('-7 days'), 'cccccccc-0000-0000-0000-000000000007', 'aaaaaaaa-0000-0000-0000-000000000002', pg_temp.ldt('-7 days'), pg_temp.inst('-7 days'), 2, null),
    -- Restock coffee (Office, Carol)
    ('dddddddd-0000-0000-0000-000000000012', 1, pg_temp.inst('0'),       'cccccccc-0000-0000-0000-000000000008', 'aaaaaaaa-0000-0000-0000-000000000004', pg_temp.ldt('2 days'),  pg_temp.inst('0'),       1, null);

-- A push token so push-message code paths have someone to "send" to (dispatcher is a no-op locally)
insert into pushmessagetokens ("memberId", "deviceId", token, "uploadedDateTime") values
    ('aaaaaaaa-0000-0000-0000-000000000002', 'ffffffff-0000-0000-0000-000000000001', 'fake-fcm-token-alice', pg_temp.inst('0'));

-- Mark the TaskRepeater as having just run so it doesn't add assignments on top of the seed for an hour.
-- Creating or editing a task via the API triggers it immediately anyway.
delete from runtimelogs;
insert into runtimelogs ("runDate") values (pg_temp.inst('0'));

commit;
