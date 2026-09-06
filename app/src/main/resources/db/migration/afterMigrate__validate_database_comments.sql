DO $$
DECLARE
    missing_tables TEXT;
    missing_columns TEXT;
BEGIN
    SELECT string_agg(
        format('%I.%I', tables.table_schema, tables.table_name),
        ', ' ORDER BY tables.table_schema, tables.table_name
    )
    INTO missing_tables
    FROM information_schema.tables tables
    WHERE tables.table_schema IN ('work', 'mailer')
      AND tables.table_type = 'BASE TABLE'
      AND NULLIF(
          btrim(obj_description(to_regclass(format('%I.%I', tables.table_schema, tables.table_name)), 'pg_class')),
          ''
      ) IS NULL;

    SELECT string_agg(
        format('%I.%I.%I', columns.table_schema, columns.table_name, columns.column_name),
        ', ' ORDER BY columns.table_schema, columns.table_name, columns.ordinal_position
    )
    INTO missing_columns
    FROM information_schema.columns columns
    JOIN information_schema.tables tables
      ON tables.table_schema = columns.table_schema
     AND tables.table_name = columns.table_name
     AND tables.table_type = 'BASE TABLE'
    WHERE columns.table_schema IN ('work', 'mailer')
      AND NULLIF(
          btrim(
              col_description(
                  to_regclass(format('%I.%I', columns.table_schema, columns.table_name)),
                  columns.ordinal_position
              )
          ),
          ''
      ) IS NULL;

    IF missing_tables IS NOT NULL OR missing_columns IS NOT NULL THEN
        RAISE EXCEPTION E'Нарушено правило документирования БД. Таблицы без комментариев: %.\nСтолбцы без комментариев: %',
            COALESCE(missing_tables, 'нет'),
            COALESCE(missing_columns, 'нет');
    END IF;
END
$$;
