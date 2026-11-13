create table file_version (
    id uuid primary key,
    owner text not null,
    path text not null,
    size bigint not null,
    created_at timestamptz not null
);

create index file_version_owner_path on file_version (owner, path, created_at desc);
