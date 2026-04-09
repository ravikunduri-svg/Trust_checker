-- TrustCheck schema
-- Run this in Supabase SQL editor before starting the backend

create extension if not exists "pgcrypto";

create table if not exists checks (
  id           uuid primary key default gen_random_uuid(),
  created_at   timestamptz not null default now(),
  input_type   text not null check (input_type in ('message', 'url', 'screenshot')),
  input_hash   text not null,           -- sha256 of raw content; raw text never stored
  risk_level   text not null check (risk_level in ('LOW', 'MEDIUM', 'HIGH')),
  risk_score   int not null check (risk_score between 0 and 100),
  red_flags    jsonb not null default '[]',
  explanation  text not null,
  next_actions jsonb not null default '[]',
  confidence   text not null check (confidence in ('LOW', 'MEDIUM', 'HIGH')),
  session_id   text,
  device_type  text,
  referrer     text
);

-- Index for fast UUID lookups
create index if not exists checks_id_idx on checks (id);

-- RLS: enable but allow anon access (public app, no auth required)
alter table checks enable row level security;

create policy "anon can insert checks"
  on checks for insert
  to anon
  with check (true);

create policy "anon can read checks"
  on checks for select
  to anon
  using (true);

-- Daily stats view for monitoring
create or replace view daily_stats as
select
  date_trunc('day', created_at) as day,
  input_type,
  risk_level,
  count(*) as total
from checks
group by 1, 2, 3
order by 1 desc, 2, 3;

-- Pre-seeded examples with stable UUIDs
-- These must match the IDs returned by GET /api/examples
insert into checks (id, input_type, input_hash, risk_level, risk_score, red_flags, explanation, next_actions, confidence)
values
  (
    '11111111-0000-0000-0000-000000000001',
    'message',
    'example-phishing-email',
    'HIGH',
    92,
    '["Urgent language designed to panic", "Generic greeting (Dear Customer)", "Suspicious sender domain", "Link does not match claimed brand", "Threatens account closure"]',
    'This is a classic phishing email. Legitimate banks never ask you to verify credentials by clicking an email link. The urgency, generic greeting, and mismatched link are textbook red flags.',
    '["Do not click any links in the email", "Log in directly via your bank''s official website", "Forward the email to your bank''s phishing report address", "Delete the email"]',
    'HIGH'
  ),
  (
    '11111111-0000-0000-0000-000000000002',
    'message',
    'example-fake-job-offer',
    'HIGH',
    88,
    '["Unsolicited job offer with no application", "Unusually high salary for vague role", "Asks for personal documents upfront", "Company cannot be verified online", "Communication only via WhatsApp"]',
    'This matches the profile of a fake job offer scam. Real recruiters contact candidates through verified channels, do not offer large salaries without interviews, and never request ID documents before an offer letter.',
    '["Do not share any personal documents", "Search the company name + ''scam'' to check reports", "Verify the recruiter on LinkedIn using the company''s official page", "Report the message to the job platform if applicable"]',
    'HIGH'
  ),
  (
    '11111111-0000-0000-0000-000000000003',
    'url',
    'example-payment-link',
    'HIGH',
    85,
    '["Domain registered less than 7 days ago", "Mimics a known payment brand (typosquatting)", "No HTTPS or invalid certificate", "Redirects through multiple domains", "No privacy policy or contact info"]',
    'This URL has multiple hallmarks of a fake payment page. Typosquatting (e.g. paypa1.com instead of paypal.com) is a common trick. The recent registration date and redirect chain confirm malicious intent.',
    '["Do not enter any payment details", "Close the page immediately", "Access your payment account directly via the official app or bookmark", "Report the URL to Google Safe Browsing at safebrowsing.google.com/safebrowsing/report_phish/"]',
    'HIGH'
  ),
  (
    '11111111-0000-0000-0000-000000000004',
    'screenshot',
    'example-edited-screenshot',
    'MEDIUM',
    61,
    '["Font inconsistency in key fields", "Metadata suggests image was edited after capture", "Balance figure uses different decimal format than platform standard", "Screenshot shared to request urgent action"]',
    'The screenshot shows signs of digital editing. Font rendering and decimal formatting inconsistencies suggest the balance or transaction amount may have been altered. Use this as a signal to verify through official channels.',
    '["Do not make any payments or decisions based on this screenshot alone", "Ask the sender to share the live screen or official transaction ID", "Log in to the relevant platform yourself to verify the claim", "If fraud is suspected, report to the platform''s trust and safety team"]',
    'MEDIUM'
  ),
  (
    '11111111-0000-0000-0000-000000000005',
    'message',
    'example-fake-charity',
    'HIGH',
    90,
    '["Charity name cannot be found in official registries", "Payment requested via gift cards or wire transfer", "No official website or registration number provided", "Emotional pressure tactics around a recent disaster", "Contact is a personal mobile number"]',
    'This is consistent with a charity scam that exploits a real disaster. Legitimate charities are registered, accept traceable payments, and have official websites. Gift card requests are a near-certain indicator of fraud.',
    '["Do not send any money", "Verify charities at give.org or charitynavigator.org before donating", "Donate directly through the charity''s official website", "Report the message to the FTC at reportfraud.ftc.gov"]',
    'HIGH'
  )
on conflict (id) do nothing;
