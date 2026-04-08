const BASE = import.meta.env.VITE_API_URL || 'http://localhost:3001';

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  const data = await res.json();

  if (!res.ok) {
    throw new Error(data.error || `Request failed (${res.status})`);
  }

  return data;
}

export function analyze(input_type, content) {
  const session_id = sessionStorage.getItem('tc_session') || crypto.randomUUID();
  sessionStorage.setItem('tc_session', session_id);

  return request('/api/analyze', {
    method: 'POST',
    body: JSON.stringify({
      input_type,
      content,
      session_id,
      referrer: document.referrer || null,
    }),
  });
}

export function getResult(id) {
  return request(`/api/result/${id}`);
}

export function getExamples() {
  return request('/api/examples');
}
