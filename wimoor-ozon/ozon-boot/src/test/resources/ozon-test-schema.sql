CREATE TABLE IF NOT EXISTS t_ozon_auth (
  id varchar(64) NOT NULL,
  shop_id varchar(64) NOT NULL,
  name varchar(64) NOT NULL,
  client_id varchar(64) NOT NULL,
  api_key_ciphertext varchar(512) NOT NULL,
  api_key_fingerprint varchar(64) NOT NULL,
  status varchar(20) DEFAULT 'ACTIVE',
  disabled boolean DEFAULT false,
  last_sync_status varchar(20),
  last_sync_message varchar(255),
  last_sync_time timestamp,
  created_by varchar(64),
  updated_by varchar(64),
  create_time timestamp,
  update_time timestamp,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_ozon_api_log (
  id varchar(64) NOT NULL,
  auth_id varchar(64),
  shop_id varchar(64),
  api_group varchar(32) NOT NULL,
  action_name varchar(64) NOT NULL,
  endpoint varchar(128) NOT NULL,
  http_method varchar(16) NOT NULL,
  object_type varchar(32),
  object_id varchar(64),
  request_payload_json clob,
  response_payload_json clob,
  status varchar(20) NOT NULL,
  error_message varchar(1024),
  duration_ms bigint,
  operator varchar(64),
  create_time timestamp,
  update_time timestamp,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_ozon_operation_audit (
  id varchar(64) NOT NULL,
  auth_id varchar(64),
  shop_id varchar(64),
  operation_type varchar(64) NOT NULL,
  object_type varchar(32) NOT NULL,
  object_id varchar(64),
  object_code varchar(128),
  request_payload_json clob,
  result_status varchar(20) NOT NULL,
  result_message varchar(1024),
  operator varchar(64),
  create_time timestamp,
  update_time timestamp,
  PRIMARY KEY (id)
);
