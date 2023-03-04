create table cqc_elem_dict
(
    id   uuid unique  not null
        constraint cqc_elem_dict_pk
            primary key,
    name varchar(150) not null
        unique
);

alter table cqc_elem_dict
    owner to postgres;

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
        primary key (child_type_id)
);

alter table cqc_elem_hierarchy
    owner to postgres;

create or replace function hierarchy_insert_trigger() returns trigger
    language plpgsql
as
$$
BEGIN
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

    raise notice 'res %', childArr;
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

-- drop trigger hierarchy_insert_trigger on cqc_elem_hierarchy;
drop trigger hierarchy_delete_trigger on cqc_elem_hierarchy;

create or replace function hierarchy_delete(pID uuid, cID uuid) returns void
    language plpgsql as
$$
DECLARE
    res uuid[];
BEGIN
    res := ARRAY(select distinct parent_type_id from cqc_elem_hierarchy);
    raise notice 'res %', res;

    if (cID in (select unnest(res)) and (pID in (select unnest(res)))) then
        raise notice 'TUT!';
        update cqc_elem_hierarchy
        set child_type_id = cID
        where child_type_id = pID;
    end if;
end;
$$;

create function get_course_parts(courseid uuid)
    returns TABLE
            (
                id        uuid,
                parent_id uuid,
                type      uuid,
                value     character varying
            )
    language plpgsql
as
$$
DECLARE
    temp    UUID;
    res     UUID[];
    leafArr UUID[];

BEGIN
    CREATE TEMP TABLE steps
    (
        stepNumber SERIAL,
        step       UUID[]
    );

    leafArr := (SELECT id
                FROM cqc_elem
                         LEFT JOIN course_output_leaf_link ON id = leaf_id
                WHERE courseId = courseId);

    INSERT INTO steps(step)
    VALUES (leafArr);

    res := leafArr;

    WHILE (NOT ((SELECT step FROM steps ORDER BY stepNumber DESC LIMIT 1) = '{}'))
        LOOP
            DECLARE
                tempArr UUID[];

            BEGIN
                FOREACH temp IN ARRAY (SELECT step FROM steps ORDER BY stepNumber DESC LIMIT 1)
                    LOOP
                        tempArr := ARRAY(SELECT cqc_elem.parent_id
                                         FROM cqc_elem
                                         WHERE cqc_elem.parent_id IS NOT NULL
                                           AND cqc_elem.id = temp);

                        res := array_cat(res, tempArr);
                        INSERT INTO steps(step) VALUES (tempArr);
                    END LOOP;
            END;
        END LOOP;
    DROP TABLE steps;

    RETURN QUERY (SELECT * FROM cqc_elem WHERE cqc_elem.id IN (SELECT DISTINCT r FROM unnest(res) AS result(r)));
END;
$$;


create table cqc_elem
(
    id        uuid         not null
        constraint cqc_element_pk
            primary key,
    parent_id uuid
        constraint parent_id_fk
            references cqc_elem
            on delete cascade,
    type      uuid         not null
        constraint type_id_fk
            references cqc_elem_dict
            on delete cascade,
    value     varchar(250) not null
);

alter table cqc_elem
    owner to postgres;

create table course
(
    id   uuid         not null
        constraint course_pk
            primary key,
    name varchar(250) not null
);

alter table course
    owner to postgres;

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

alter table course_input_leaf_link
    owner to postgres;

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

alter table course_output_leaf_link
    owner to postgres;

create function course_leaf_insert_trigger() returns trigger
    language plpgsql
as
$$
BEGIN
    IF ((SELECT parent_type
         FROM cqc_elem_hierarchy
         WHERE parent_type =
               (SELECT type FROM cqc_elem WHERE id = NEW.leaf_id)
         LIMIT 1) IS NOT NULL) THEN
        RAISE EXCEPTION 'invalid relationship';
    END IF;
    RETURN NEW;
END;
$$;

alter function course_leaf_insert_trigger() owner to postgres;

create function cqc_elem_insert_trigger() returns trigger
    language plpgsql
as
$$
DECLARE
    parentType uuid;
    temp       uuid;

BEGIN
    IF (NEW.parent_id IS NOT NULL) THEN
        parentType := (SELECT cqc_elem.type
                       FROM cqc_elem
                       WHERE id = NEW.parent_id);

        temp := (SELECT cqc_elem_hierarchy.parent_type
                 FROM cqc_elem_hierarchy
                 WHERE child_type = NEW.type);

        IF (temp IS NULL OR parentType <> temp) THEN
            RAISE EXCEPTION 'invalid relationship';
        END IF;
    END IF;
    RETURN NEW;
END ;
$$;

alter function cqc_elem_insert_trigger() owner to postgres;

create function get_course_parts(courseid uuid)
    returns TABLE
            (
                id        uuid,
                parent_id uuid,
                type      uuid,
                value     character varying
            )
    language plpgsql
as
$$
DECLARE
    temp    UUID;
    res     UUID[];
    leafArr UUID[];

BEGIN
    CREATE TEMP TABLE steps
    (
        stepNumber SERIAL,
        step       UUID[]
    );

    leafArr := (SELECT id
                FROM cqc_elem
                         LEFT JOIN course_output_leaf_link ON id = leaf_id
                WHERE courseId = courseId);

    INSERT INTO steps(step)
    VALUES (leafArr);

    res := leafArr;

    WHILE (NOT ((SELECT step FROM steps ORDER BY stepNumber DESC LIMIT 1) = '{}'))
        LOOP
            DECLARE
                tempArr UUID[];

            BEGIN
                FOREACH temp IN ARRAY (SELECT step FROM steps ORDER BY stepNumber DESC LIMIT 1)
                    LOOP
                        tempArr := ARRAY(SELECT cqc_elem.parent_id
                                         FROM cqc_elem
                                         WHERE cqc_elem.parent_id IS NOT NULL
                                           AND cqc_elem.id = temp);

                        res := array_cat(res, tempArr);
                        INSERT INTO steps(step) VALUES (tempArr);
                    END LOOP;
            END;
        END LOOP;
    DROP TABLE steps;

    RETURN QUERY (SELECT * FROM cqc_elem WHERE cqc_elem.id IN (SELECT DISTINCT r FROM unnest(res) AS result(r)));
END;
$$;

alter function get_course_parts(uuid) owner to postgres;