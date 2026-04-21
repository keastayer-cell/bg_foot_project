insert into mailer.notification_template (
    code,
    channel,
    subject_template,
    body_template,
    body_format,
    is_active
) values (
    'PASSWORD_RESET_REQUESTED',
    'EMAIL',
    'Сброс пароля в Футбол Богородска',
    '<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Сброс пароля.</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f4f7fb;padding:24px 0;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:640px;background-color:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 10px 30px rgba(15,23,42,0.08);">
          <tr>
            <td style="padding:32px 40px;background:linear-gradient(135deg,#0f172a 0%,#1d4ed8 100%);color:#ffffff;">
              <div style="font-size:12px;line-height:18px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.8;">Футбол Богородска</div>
              <h1 style="margin:12px 0 0;font-size:30px;line-height:36px;font-weight:700;">Сброс пароля.</h1>
            </td>
          </tr>
          <tr>
            <td style="padding:32px 40px 24px;">
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Здравствуйте, ' || '$' || '{recipientName}' || '.</p>
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Вы запрашивали сброс пароля. Перейдите по ссылке ниже и назначьте новый пароль.</p>
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;"><a href="' || '$' || '{resetLink}' || '" style="color:#1d4ed8;font-weight:700;text-decoration:none;">Открыть страницу сброса пароля</a></p>
            </td>
          </tr>
          <tr>
            <td style="padding:0 40px 16px;">
              <div style="padding:16px 18px;border-radius:14px;background-color:#eef4ff;font-size:14px;line-height:22px;color:#334155;word-break:break-word;">
                Если кнопка не открывается, используйте ссылку вручную:<br>' || '$' || '{resetLink}' || '
              </div>
            </td>
          </tr>
          <tr>
            <td style="padding:0 40px 32px;">
              <div style="padding:16px 18px;border-radius:14px;background-color:#eef4ff;font-size:14px;line-height:22px;color:#334155;">
                Если у вас возникли вопросы, обратитесь к администратору ресурса по почте ' || '$' || '{contactEmail}' || '.
              </div>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>',
    'HTML',
    true
)
on conflict (code) do update
set channel = excluded.channel,
    subject_template = excluded.subject_template,
    body_template = excluded.body_template,
    body_format = excluded.body_format,
    is_active = excluded.is_active,
    updated_at = now();