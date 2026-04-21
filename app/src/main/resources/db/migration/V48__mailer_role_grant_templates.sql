insert into mailer.notification_template (
    code,
    channel,
    subject_template,
    body_template,
    body_format,
    is_active
) values (
    'USER_REGISTERED',
    'EMAIL',
    'Добро пожаловать в Футбол Богородска',
    '<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Добро пожаловать.</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f4f7fb;padding:24px 0;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:640px;background-color:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 10px 30px rgba(15,23,42,0.08);">
          <tr>
            <td style="padding:32px 40px;background:linear-gradient(135deg,#0f172a 0%,#1d4ed8 100%);color:#ffffff;">
              <div style="font-size:12px;line-height:18px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.8;">Футбол Богородска</div>
              <h1 style="margin:12px 0 0;font-size:30px;line-height:36px;font-weight:700;">Добро пожаловать, ' || '$' || '{recipientName}' || '!</h1>
            </td>
          </tr>
          <tr>
            <td style="padding:32px 40px 24px;">
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Ваш аккаунт успешно создан, и теперь вы можете пользоваться системой.</p>
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Если вы представляете команду, обратитесь к администратору ресурса, чтобы получить права представителя и доступ к управлению составом.</p>
            </td>
          </tr>
          <tr>
            <td style="padding:0 40px 32px;">
              <div style="padding:16px 18px;border-radius:14px;background-color:#eef4ff;font-size:14px;line-height:22px;color:#334155;">
                Если у вас есть вопросы по доступу или настройке команды, свяжитесь с администратором ресурса, написав на почту info@bgfoot.ru.
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
), (
    'TEAM_REP_ROLE_GRANTED',
    'EMAIL',
    'Вам выданы права представителя команды',
    '<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Права представителя команды.</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f4f7fb;padding:24px 0;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:640px;background-color:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 10px 30px rgba(15,23,42,0.08);">
          <tr>
            <td style="padding:32px 40px;background:linear-gradient(135deg,#0f172a 0%,#1d4ed8 100%);color:#ffffff;">
              <div style="font-size:12px;line-height:18px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.8;">Футбол Богородска</div>
              <h1 style="margin:12px 0 0;font-size:30px;line-height:36px;font-weight:700;">Права представителя команды выданы.</h1>
            </td>
          </tr>
          <tr>
            <td style="padding:32px 40px 24px;">
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Уважаемый(ая) ' || '$' || '{recipientName}' || '.</p>
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Вам была выдана роль "Представитель команды ' || '$' || '{teamName}' || '".</p>
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Теперь вам доступна административная часть, в которой вы можете управлять своей командой в рамках всех сезонов.</p>
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
), (
    'REFEREE_ROLE_GRANTED',
    'EMAIL',
    'Вам выданы права рефери',
    '<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Права рефери.</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#f4f7fb;padding:24px 0;">
    <tr>
      <td align="center">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:640px;background-color:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 10px 30px rgba(15,23,42,0.08);">
          <tr>
            <td style="padding:32px 40px;background:linear-gradient(135deg,#0f172a 0%,#1d4ed8 100%);color:#ffffff;">
              <div style="font-size:12px;line-height:18px;letter-spacing:0.12em;text-transform:uppercase;opacity:0.8;">Футбол Богородска</div>
              <h1 style="margin:12px 0 0;font-size:30px;line-height:36px;font-weight:700;">Права рефери выданы.</h1>
            </td>
          </tr>
          <tr>
            <td style="padding:32px 40px 24px;">
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Уважаемый(ая) ' || '$' || '{recipientName}' || '.</p>
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Вам была выдана роль "Рефери".</p>
              <p style="margin:0 0 16px;font-size:16px;line-height:26px;">Теперь вам доступен административный контур для администрирования чемпионатов, матчей и связанных с ними процессов.</p>
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