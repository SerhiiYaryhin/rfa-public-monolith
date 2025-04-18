SELECT tablename 
FROM pg_tables 
WHERE tablename LIKE 'acc_%';
DO
$$
DECLARE
    _tableName TEXT;
BEGIN
    FOR _tableName IN SELECT tablename FROM pg_tables WHERE tablename LIKE 'acc_%'
    LOOP
        -- Подготовка к удалению таблиц.
        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(_tableName) || ' CASCADE';
    END LOOP;
END;
$$;