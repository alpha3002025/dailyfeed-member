insert into dailyfeed.members(
    password, roles, created_at, updated_at
)
values(
       'aaaaa',
       'PLAIN_USER',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    password, roles, created_at, updated_at
)
values(
        'bbbbb',
        'PLAIN_USER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    password, roles, created_at, updated_at
)
values(
        'ccccc',
        'PLAIN_USER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    password, roles, created_at, updated_at
)
values(
        'ddddd',
        'PLAIN_USER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    password, roles, created_at, updated_at
)
values(
        'eeeee',
        'PLAIN_USER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    password, roles, created_at, updated_at
)
values(
          'fffff',
          'PLAIN_USER',
          CURRENT_TIMESTAMP,
          CURRENT_TIMESTAMP
);

-- Member profiles for each member
insert into dailyfeed.member_profiles(
    member_id, member_name, display_name, handle, bio, location, website_url,
    birth_date, gender, is_active, privacy_level, language_code, country_code,
    verification_status, profile_completion_score, created_at, updated_at
)
select
    id, id, id, id, CONCAT('I am user ', id), 'Seoul, Korea', NULL,
    '1990-01-01', 'FEMALE', true, 'PUBLIC', 'ko', 'KR',
    'NONE', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from dailyfeed.members;