INSERT IGNORE  INTO authority (name) VALUES ('ROLE_ADMIN');
INSERT IGNORE INTO authority (name) VALUES ('ROLE_USER');
INSERT IGNORE INTO magasin (id, address, name, note, phone, registre) VALUES (1, '', '', 'Bienvenue !', '', '');
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url, activated, lang_key, activation_key, reset_key, created_by, created_date, reset_date, last_modified_by, last_modified_date) VALUES (1, 'system', '$2a$10$mE.qmcV0mFU5NcKh73TZx.z4ueI/.bDWbj0T1BYyqP481kGGarKLG', 'System', 'System', 'system@localhost', '', true, 'fr', null, null, 'system', null, null, 'system', null);
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url, activated, lang_key, activation_key, reset_key, created_by, created_date, reset_date, last_modified_by, last_modified_date) VALUES (2, 'anonymoususer', '$2a$10$j8S5d7Sr7.8VTOYNviDPOeWX8KcYILUVJBsYV83Y5NtECayypx9lO', 'Anonymous', 'User', 'anonymous@localhost', '', true, 'fr', null, null, 'system', null, null, 'system', null);
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url, activated, lang_key, activation_key, reset_key, created_by, created_date, reset_date, last_modified_by, last_modified_date) VALUES (3, 'admin', '$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC', 'Administrator', 'Administrator', 'admin@localhost', '', true, 'fr', null, null, 'system', null, null, 'system', null);
INSERT IGNORE INTO user (id, login, password_hash, first_name, last_name, email, image_url, activated, lang_key, activation_key, reset_key, created_by, created_date, reset_date, last_modified_by, last_modified_date) VALUES (4, 'user', '$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vfiEl7lwWgOH/K', 'User', 'User', 'user@localhost', '', true, 'fr', null, null, 'system', null, null, 'system', null);


INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (1, 'ROLE_ADMIN');
INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (1, 'ROLE_USER');
INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (3, 'ROLE_ADMIN');
INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (3, 'ROLE_USER');
INSERT IGNORE INTO user_authority (user_id, authority_name) VALUES (4, 'ROLE_USER');

INSERT IGNORE INTO magasin (id, address, name, note, phone, registre) VALUES (1, '17 rue de bruxelle', 'Easy shop', 'Bienvenue !', '075759146', '');
INSERT IGNORE INTO `payment_mode` (`id`, `code`, `libelle`, `payment_group`) VALUES (1, 'CASH', 'Espèce', 0);
