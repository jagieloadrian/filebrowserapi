create table trash_item (
    id uuid primary key,
    owner text not null,
    original_path text not null,
    directory boolean not null,
    size bigint not null,
    deleted_at timestamptz not null
);

create index trash_item_owner on trash_item (owner, deleted_at);
create index trash_item_deleted_at on trash_item (deleted_at);
