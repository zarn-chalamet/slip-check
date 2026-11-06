-- SlipCheck V1 schema.
-- Money is integer satang (bigint). All timestamps are timestamptz (UTC in the DB).
-- Slip images are never stored.

create table sellers (
  id               uuid primary key,
  email            text not null unique,
  password_hash    text not null,
  display_name     text not null,
  language         text not null default 'en' check (language in ('en','th','my')),
  time_zone        text not null default 'Asia/Bangkok',
  created_at       timestamptz not null default now()
);

create table seller_bank_accounts (
  id                 uuid primary key,
  seller_id          uuid not null references sellers(id) on delete cascade,
  bank_code          text not null,              -- Thai bank code, as used by the provider
  account_no_enc     bytea not null,             -- AES-GCM encrypted digits (IV prefixed)
  account_last4      text not null,
  holder_name        text not null,
  created_at         timestamptz not null default now()
);

create table channel_identities (
  id                 uuid primary key,
  seller_id          uuid not null references sellers(id) on delete cascade,
  channel            text not null check (channel in ('TELEGRAM','LINE','WEB')),
  external_user_id   text not null,              -- Telegram chat id / LINE userId / seller id for WEB
  language           text not null default 'en' check (language in ('en','th','my')),
  summary_push       boolean not null default false,
  linked_at          timestamptz not null default now(),
  unique (channel, external_user_id)
);

create table link_codes (
  id            uuid primary key,
  seller_id     uuid not null references sellers(id) on delete cascade,
  code_hash     text not null,                   -- sha-256 hex of the 6-digit code
  expires_at    timestamptz not null,
  used_at       timestamptz
);
create index idx_link_codes_code_hash on link_codes (code_hash) where used_at is null;

create table slip_submissions (
  id                    uuid primary key,
  seller_id             uuid references sellers(id) on delete cascade,  -- null if chat not linked
  channel               text not null,
  external_message_id   text not null,           -- webhook idempotency
  status                text not null check (status in
                          ('RECEIVED','PROCESSING','DONE','RETRY_SCHEDULED')),
  result_code           text,                    -- see spec section 13
  caption_raw           text,
  expected_amount_satang bigint,                 -- parsed from caption, optional
  label                 text,                    -- rest of caption
  sending_bank          text,
  trans_ref             text,
  amount_satang         bigint,
  transferred_at        timestamptz,
  receiver_name_masked  text,
  receiver_account_masked text,
  image_sha256          text,
  provider_name         text,
  provider_response     jsonb,                   -- trimmed; removed by retention job
  attempts              int not null default 0,
  next_retry_at         timestamptz,
  review_decision       text check (review_decision in ('APPROVED','REJECTED')),
  review_note           text,
  reviewed_at           timestamptz,
  created_at            timestamptz not null default now(),
  processed_at          timestamptz,
  unique (channel, external_message_id)
);
create index idx_slip_submissions_seller_created on slip_submissions (seller_id, created_at desc);
create index idx_slip_submissions_retry on slip_submissions (status, next_retry_at);
create index idx_slip_submissions_image_hash on slip_submissions (seller_id, image_sha256);

-- A transaction is "claimed" by a seller once it is real AND sent to that seller's account.
-- The unique key is the duplicate guarantee, even under concurrency.
create table claimed_transactions (
  id              uuid primary key,
  seller_id       uuid not null references sellers(id) on delete cascade,
  sending_bank    text not null,
  trans_ref       text not null,
  submission_id   uuid not null references slip_submissions(id),
  claimed_at      timestamptz not null default now(),
  unique (seller_id, sending_bank, trans_ref)
);

create table audit_events (
  id            uuid primary key,
  seller_id     uuid,
  actor_type    text not null,                   -- SELLER | SYSTEM | CHANNEL
  actor_id      text,
  event_type    text not null,                   -- SLIP_PROCESSED, REVIEW_APPROVED, ACCOUNT_ADDED, ...
  entity_type   text,
  entity_id     text,
  payload       jsonb,
  created_at    timestamptz not null default now()
);
create index idx_audit_events_seller_created on audit_events (seller_id, created_at desc);

-- Append-only intent: in production, grant the application role INSERT and SELECT only
-- on audit_events, e.g.:
--   revoke update, delete on audit_events from slipcheck_app;
-- (Kept as a comment so local/dev databases stay simple. The sole exception path is
-- PDPA deletion, which anonymises audit_events.seller_id for a deleted seller.)
