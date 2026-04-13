INSERT INTO work.w_role(code, name_ru)
VALUES ('GUEST', 'Гость')
ON CONFLICT (code) DO NOTHING;