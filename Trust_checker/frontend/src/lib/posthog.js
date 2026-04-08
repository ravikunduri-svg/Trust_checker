import posthog from 'posthog-js';

export function initPostHog() {
  const key = import.meta.env.VITE_POSTHOG_API_KEY;
  if (!key) return; // no-op in local dev without key

  posthog.init(key, {
    api_host: import.meta.env.VITE_POSTHOG_HOST || 'https://app.posthog.com',
    autocapture: true,
    capture_pageview: true,
  });
}

export function track(event, properties = {}) {
  if (typeof posthog?.capture === 'function') {
    posthog.capture(event, properties);
  }
}
