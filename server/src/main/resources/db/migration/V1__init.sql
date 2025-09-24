create table if not exists app_info (
  id int primary key,
  name varchar(64) not null,
  created_at timestamp not null default current_timestamp
);
insert into app_info (id, name) values (1, 'LOCATION');
