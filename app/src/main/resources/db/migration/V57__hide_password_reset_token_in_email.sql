do $migration$
declare
    v_updated_rows integer;
    v_reset_link_placeholder text := chr(36) || '{resetLink}';
begin
    update mailer.notification_template
       set body_template = replace(
               body_template,
               $old$Если кнопка не открывается, используйте ссылку вручную:<br>$old$ || v_reset_link_placeholder,
               $new$Если кнопка не открывается, используйте <a href="$new$ || v_reset_link_placeholder || $new$" style="color:#1d4ed8;font-weight:700;text-decoration:underline;">ссылку для сброса пароля</a>.$new$
           ),
           updated_at = now()
     where code = 'PASSWORD_RESET_REQUESTED'
       and body_template like '%Если кнопка не открывается, используйте ссылку вручную:<br>%';

    get diagnostics v_updated_rows = row_count;
    if v_updated_rows <> 1 then
        raise exception 'Не удалось обновить отображение ссылки в шаблоне PASSWORD_RESET_REQUESTED';
    end if;
end
$migration$;
