-- Словарь элементов ККХ
create table cqc_elem_dict
(
    id         uuid unique  not null
        constraint cqc_elem_dict_pk
            primary key,
    name       varchar(150) not null
        unique,
    is_deleted boolean      not null
);

-- Уровни иерархии элементов ККХ
create table cqc_elem_hierarchy
(
    parent_type_id uuid not null
        constraint parent_fk
            references cqc_elem_dict
            on delete cascade,
    child_type_id  uuid not null
        constraint child_fk
            references cqc_elem_dict
            on delete cascade,
    constraint child_parent_pk
        primary key (parent_type_id, child_type_id)
);

-- Триггер перестройки связей при вставке новых уровней в иерархию ККХ
create or replace function hierarchy_insert_trigger() returns trigger
    language plpgsql
as
$$
begin
	if (new.parent_type_id = (select parent_type_id from cqc_elem_hierarchy where child_type_id = new.child_type_id)) then 
		raise exception 'Such record exists Parent: % - Child: %',
			new.parent_type_id,
			new.child_type_id;
	end if;

    if (new.child_type_id in (select child_type_id from cqc_elem_hierarchy)) then
        update cqc_elem_hierarchy
        set child_type_id = new.parent_type_id
        where new.child_type_id = child_type_id;
    end if;
    return new;
END;
$$;

create trigger hierarchy_insert_trigger
    before insert
    on cqc_elem_hierarchy
    for each row
execute procedure hierarchy_insert_trigger();


-- Триггер перестройки связей в иерархии при удалении уровня иерархии
create or replace function hierarchy_delete_trigger() returns trigger
    language plpgsql
as
$$
DECLARE
    childArr  uuid[];
    parentArr uuid[];

BEGIN
    childArr := ARRAY(select distinct child_type_id from cqc_elem_hierarchy);
    parentArr := ARRAY(select distinct parent_type_id from cqc_elem_hierarchy);

    if ((old.parent_type_id in (select unnest(childArr))) and
        (old.child_type_id in (select unnest(parentArr)))) then
        update cqc_elem_hierarchy
        set child_type_id = old.child_type_id
        where child_type_id = old.parent_type_id;
    end if;
    return old;
END;
$$;

create trigger hierarchy_delete_trigger
    after delete
    on cqc_elem_hierarchy
    for each row
execute procedure hierarchy_delete_trigger();

-- Таблица самих ККХ
create table cqc_elem
(
    id        uuid not null
        constraint cqc_element_pk
            primary key,
    parent_id uuid
        constraint parent_id_fk
            references cqc_elem,
    type_id   uuid null
        constraint type_id_fk
            references cqc_elem_dict,
    value     varchar(250) unique
);

-- Проверка соответсвия иерархии
create or replace function cqc_element_insert_trigger() returns trigger
    language plpgsql
as
$$
declare
    parentType uuid;

begin  
   parentType = (select type_id from cqc_elem where id = new.parent_id);
   

  
    if (new.type_id in (select child_type_id from cqc_elem_hierarchy)
        and (
                (parentType is null) or
                (parentType != (select parent_type_id from cqc_elem_hierarchy where child_type_id = new.type_id))
            )) then
        raise exception 'Hierarchy violation. Parent: % - Child:%',
                (select name from cqc_elem_dict where id = parentType),
                (select name from cqc_elem_dict where id = new.type_id);
    end if;
    return new;
end;
$$;

create trigger cqc_element_insert_trigger
    before insert or update
    on cqc_elem
    for each row
execute procedure cqc_element_insert_trigger();

create table course
(
    id   uuid         not null
        constraint course_pk
            primary key,
    name varchar(250) not null
);

create table course_input_leaf_link
(
    course_id uuid not null
        constraint course_input_leaf_fk
            references course
            on delete cascade,
    leaf_id   uuid not null
        constraint input_leaf_fk
            references cqc_elem
            on delete cascade
);

-- Если входной элемент не верх иерархии (не является никому дочерним)
-- и нет связи с его родителем - ошибка
create or replace function course_input_leafs_insert_trigger() returns trigger
    language plpgsql
as
$$
begin 
	
	
	if (((select parent_type_id from cqc_elem_hierarchy where child_type_id = (select type_id from cqc_elem where id = new.leaf_id)) is not null)
	and ((select leaf_id from course_input_leaf_link where leaf_id = (select parent_id from cqc_elem where id = new.leaf_id) and course_id = new.course_id) is null)) then
		raise exception 'Hierarchy violation. The course has no relation to the elements: %', (select value from cqc_elem where id = new.leaf_id);
	end if;
	
	return new;
end;
$$;

create trigger course_input_leafs_insert_trigger
    before insert or update
    on course_input_leaf_link
    for each row
execute procedure course_input_leafs_insert_trigger();

drop trigger course_input_leafs_insert_trigger on course_input_leaf_link;

create table course_output_leaf_link
(
    course_id uuid not null
        constraint course_output_leaf_fk
            references course
            on delete cascade,
    leaf_id   uuid not null
        constraint output_leaf_fk
            references cqc_elem
            on delete cascade
);

-- Проверка, что переданный тип является листом.
create or replace function is_leaf_type(elem_type_id uuid) returns boolean  language plpgsql as $$
begin 
	if ((select child_type_id from cqc_elem_hierarchy where parent_type_id = elem_type_id limit 1) is null) 
		then return true;
	else return false;
	end if;
end;
$$;

-- Получение элементов ККХ, входящих в данный курс
create or replace function get_input_elements(c_id uuid) returns table(elem_id uuid, elem_parent_id uuid, type_id uuid, type_name character varying, type_id_deleted boolean, elem_value character varying) language plpgsql as $$
declare
	childs uuid[];
	res_id uuid[];
	elem uuid;
	
begin
	-- Получение id элементов, которые напрямую связаны с курсом.
	-- Имеют отношения в таблице связи (course_input_leaf_link).
	childs := ARRAY(select leaf_id from course_input_leaf_link where course_id = c_id);
	res_id := childs;

	-- 1. Определяем предпоследние элементы иерархии (имеют связь только с листами).
	-- 2. Выбираем листовые элементы через ссылку на родителя, полученные в 1.
	foreach elem in array childs
	loop
		if (select * from is_leaf_type( 
			(select child_type_id from cqc_elem_hierarchy where parent_type_id =
			(select el.type_id from cqc_elem el where el.id = elem) limit 1))) then 
		res_id := res_id || array(select id from cqc_elem where parent_id = elem);
		end if;
	end loop;

	-- Возвращаем все ходящие в курс элементы ККХ вместе с данными об их типе.
	return query
		select el.id, el.parent_id, dict.id, dict."name", dict.is_deleted, el.value from cqc_elem el 
		left join cqc_elem_dict dict 
		on el.type_id = dict.id
		where el.id = any(res_id);
end;
$$;

-- Получение исходящих из данного курса элементов ККХ
create or replace function get_output_elements(c_id uuid) returns table(elem_id uuid, elem_parent_id uuid, type_id uuid, type_name character varying, type_id_deleted boolean, elem_value character varying) language plpgsql as $$
declare
	childs uuid[];
	res_id uuid[];
	elem uuid;
	
begin
	-- Получение id элементов, которые напрямую связаны с курсом.
	-- Имеют отношения в таблице связи (course_output_leaf_link).
	childs := ARRAY(select leaf_id from course_output_leaf_link where course_id = c_id);
	res_id := childs;

	-- 1. Определяем предпоследние элементы иерархии (имеют связь только с листами).
	-- 2. Выбираем листовые элементы через ссылку на родителя, полученные в 1.
	foreach elem in array childs
	loop
		if (select * from is_leaf_type( 
			(select child_type_id from cqc_elem_hierarchy where parent_type_id =
			(select el.type_id from cqc_elem el where el.id = elem) limit 1))) then 
		res_id := res_id || array(select id from cqc_elem where parent_id = elem);
		end if;
	end loop;

	-- Возвращаем все выходные из курса элементы ККХ вместе с данными об их типе.
	return query
		select el.id, el.parent_id, dict.id, dict."name", dict.is_deleted, el.value from cqc_elem el 
		left join cqc_elem_dict dict 
		on el.type_id = dict.id
		where el.id = any(res_id);
end;
$$;		


select dict1.id, dict1."name", dict1.is_deleted, dict2.id, dict2."name", dict2.is_deleted  from cqc_elem_hierarchy 
left join cqc_elem_dict as dict1
on parent_type_id = dict1.id 
left join cqc_elem_dict as dict2
on child_type_id = dict2.id;