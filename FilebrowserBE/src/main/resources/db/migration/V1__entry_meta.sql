create table entry_meta (
    owner text not null,
    path text not null,
    description text,
    tags text[] not null default '{}',
    primary key (owner, path)
);

create index entry_meta_tags on entry_meta using gin (tags);
