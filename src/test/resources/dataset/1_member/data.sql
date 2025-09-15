insert into dailyfeed.members(
    email, password, name, roles, created_at, updated_at
)
values(
       'a@gmail.com',
       'aaaaa',
       'A',
       'PLAIN_USER',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    email, password, name, roles, created_at, updated_at
)
values(
        'b@gmail.com',
        'bbbbb',
        'B',
        'PLAIN_USER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    email, password, name, roles, created_at, updated_at
)
values(
        'c@gmail.com',
        'ccccc',
        'C',
        'PLAIN_USER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    email, password, name, roles, created_at, updated_at
)
values(
        'd@gmail.com',
        'ddddd',
        'D',
        'PLAIN_USER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    email, password, name, roles, created_at, updated_at
)
values(
        'e@gmail.com',
        'eeeee',
        'E',
        'PLAIN_USER',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
);

insert into dailyfeed.members(
    email, password, name, roles, created_at, updated_at
)
values(
          'f@gmail.com',
          'fffff',
          'F',
          'PLAIN_USER',
          CURRENT_TIMESTAMP,
          CURRENT_TIMESTAMP
);

-- Member profiles for each member
insert into dailyfeed.member_profiles(
    member_id, member_name, display_name, handle, bio, location, website_url,
    birth_date, gender, is_active, privacy_level, language_code, country_code,
    timezone, verification_status, profile_completion_score, created_at, updated_at
)
select
    id, name, name, CONCAT('@', LOWER(name)), CONCAT('I am user ', name), 'Seoul, Korea', NULL,
    '1990-01-01', 'OTHER', true, 'PUBLIC', 'ko', 'KR',
    'Asia/Seoul', 'NONE', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from dailyfeed.members;