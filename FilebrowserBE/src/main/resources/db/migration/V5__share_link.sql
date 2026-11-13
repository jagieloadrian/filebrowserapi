create table share_link (
    id uuid primary key,
    owner text not null,
    path text not null,
    token_hash text not null unique,
    created_at timestamptz not null,
    expires_at timestamptz not null
);

create index share_link_owner_path on share_link (owner, path);
create index share_link_expires_at on share_link (expires_at);
