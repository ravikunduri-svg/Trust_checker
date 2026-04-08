// Pre-seeded examples — IDs must match supabase/schema.sql inserts
export const EXAMPLES = [
  {
    id: '11111111-0000-0000-0000-000000000001',
    input_type: 'message',
    risk_level: 'HIGH',
    risk_score: 92,
    red_flags: [
      'Urgent language designed to panic',
      'Generic greeting (Dear Customer)',
      'Suspicious sender domain',
      'Link does not match claimed brand',
      'Threatens account closure',
    ],
    explanation:
      'This is a classic phishing email. Legitimate banks never ask you to verify credentials by clicking an email link. The urgency, generic greeting, and mismatched link are textbook red flags.',
    next_actions: [
      'Do not click any links in the email',
      "Log in directly via your bank's official website",
      "Forward the email to your bank's phishing report address",
      'Delete the email',
    ],
    confidence: 'HIGH',
    label: 'Phishing email — bank account alert',
  },
  {
    id: '11111111-0000-0000-0000-000000000002',
    input_type: 'message',
    risk_level: 'HIGH',
    risk_score: 88,
    red_flags: [
      'Unsolicited job offer with no application',
      'Unusually high salary for vague role',
      'Asks for personal documents upfront',
      'Company cannot be verified online',
      'Communication only via WhatsApp',
    ],
    explanation:
      'This matches the profile of a fake job offer scam. Real recruiters contact candidates through verified channels, do not offer large salaries without interviews, and never request ID documents before an offer letter.',
    next_actions: [
      'Do not share any personal documents',
      "Search the company name + 'scam' to check reports",
      "Verify the recruiter on LinkedIn using the company's official page",
      'Report the message to the job platform if applicable',
    ],
    confidence: 'HIGH',
    label: 'Fake job offer via WhatsApp',
  },
  {
    id: '11111111-0000-0000-0000-000000000003',
    input_type: 'url',
    risk_level: 'HIGH',
    risk_score: 85,
    red_flags: [
      'Domain registered less than 7 days ago',
      'Mimics a known payment brand (typosquatting)',
      'No HTTPS or invalid certificate',
      'Redirects through multiple domains',
      'No privacy policy or contact info',
    ],
    explanation:
      'This URL has multiple hallmarks of a fake payment page. Typosquatting (e.g. paypa1.com instead of paypal.com) is a common trick. The recent registration date and redirect chain confirm malicious intent.',
    next_actions: [
      'Do not enter any payment details',
      'Close the page immediately',
      'Access your payment account directly via the official app or bookmark',
      'Report the URL to Google Safe Browsing',
    ],
    confidence: 'HIGH',
    label: 'Suspicious payment link (typosquatting)',
  },
  {
    id: '11111111-0000-0000-0000-000000000004',
    input_type: 'screenshot',
    risk_level: 'MEDIUM',
    risk_score: 61,
    red_flags: [
      'Font inconsistency in key fields',
      'Metadata suggests image was edited after capture',
      'Balance figure uses different decimal format than platform standard',
      'Screenshot shared to request urgent action',
    ],
    explanation:
      'The screenshot shows signs of digital editing. Font rendering and decimal formatting inconsistencies suggest the balance or transaction amount may have been altered. Use this as a signal to verify through official channels.',
    next_actions: [
      'Do not make any payments or decisions based on this screenshot alone',
      'Ask the sender to share the live screen or official transaction ID',
      'Log in to the relevant platform yourself to verify the claim',
      "If fraud is suspected, report to the platform's trust and safety team",
    ],
    confidence: 'MEDIUM',
    label: 'Edited payment screenshot',
  },
  {
    id: '11111111-0000-0000-0000-000000000005',
    input_type: 'message',
    risk_level: 'HIGH',
    risk_score: 90,
    red_flags: [
      'Charity name cannot be found in official registries',
      'Payment requested via gift cards or wire transfer',
      'No official website or registration number provided',
      'Emotional pressure tactics around a recent disaster',
      'Contact is a personal mobile number',
    ],
    explanation:
      'This is consistent with a charity scam that exploits a real disaster. Legitimate charities are registered, accept traceable payments, and have official websites. Gift card requests are a near-certain indicator of fraud.',
    next_actions: [
      'Do not send any money',
      'Verify charities at give.org or charitynavigator.org before donating',
      "Donate directly through the charity's official website",
      'Report the message to the FTC at reportfraud.ftc.gov',
    ],
    confidence: 'HIGH',
    label: 'Fake charity after a disaster',
  },
];
