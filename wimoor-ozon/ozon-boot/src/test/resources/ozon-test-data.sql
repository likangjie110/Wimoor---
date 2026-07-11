INSERT INTO t_ozon_auth (
  id, shop_id, name, client_id, api_key_ciphertext, api_key_fingerprint, status, disabled, created_by, updated_by, create_time, update_time
) VALUES
('auth-integration-1', 'shop-integration-1', 'API integration auth', 'client-api-1', 'cipher', 'fingerprint-api-1', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-test-1', 'shop-integration-1', 'API status auth', 'client-api-2', 'cipher', 'fingerprint-api-2', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-obj-1', 'shop-integration-1', 'API object auth', 'client-api-3', 'cipher', 'fingerprint-api-3', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-id-1', 'shop-integration-1', 'API id auth', 'client-api-4', 'cipher', 'fingerprint-api-4', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-time-1', 'shop-integration-1', 'API time auth', 'client-api-5', 'cipher', 'fingerprint-api-5', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-audit-1', 'shop-audit-1', 'Audit integration auth', 'client-audit-1', 'cipher', 'fingerprint-audit-1', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-type-1', 'shop-audit-1', 'Audit type auth', 'client-audit-2', 'cipher', 'fingerprint-audit-2', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-status-1', 'shop-audit-1', 'Audit status auth', 'client-audit-3', 'cipher', 'fingerprint-audit-3', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-obj-audit-1', 'shop-audit-1', 'Audit object auth', 'client-audit-4', 'cipher', 'fingerprint-audit-4', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-id-audit-1', 'shop-audit-1', 'Audit id auth', 'client-audit-5', 'cipher', 'fingerprint-audit-5', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-operator-1', 'shop-audit-1', 'Audit operator auth', 'client-audit-6', 'cipher', 'fingerprint-audit-6', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-time-audit-1', 'shop-audit-1', 'Audit time auth', 'client-audit-7', 'cipher', 'fingerprint-audit-7', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('auth-code-1', 'shop-audit-1', 'Audit code auth', 'client-audit-8', 'cipher', 'fingerprint-audit-8', 'ACTIVE', false, 'test', 'test', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
