create table file_hash (
    owner text not null,
    path text not null,
    size bigint not null,
    modified_ms bigint not null,
    sha256 text not null,
    primary key (owner, path)
);

create index file_hash_owner_sha256 on file_hash (owner, sha256);
