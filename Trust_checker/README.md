# TrustCheck

AI-powered trust assistant. Paste a suspicious message, URL, or screenshot description → get a risk level, red flags, and next actions in seconds.

**Stack:** React + Vite + Tailwind (Vercel) · Node.js + Express (Railway/Render) · Groq / Llama 3.3 · Supabase

---

## Local setup

### 1. Database (Supabase)

1. Create a new [Supabase](https://supabase.com) project.
2. Open the SQL editor and run `supabase/schema.sql` — this creates the `checks` table, `daily_stats` view, and inserts 5 pre-seeded examples.
3. Copy your **Project URL** and **anon public key** from Project Settings → API.

### 2. Backend

```bash
cd backend
cp .env.example .env
# Fill in GROQ_API_KEY, SUPABASE_URL, SUPABASE_ANON_KEY
npm install
npm run dev
# → API running on http://localhost:3001
```

Get a free Groq API key at [console.groq.com](https://console.groq.com).

**Smoke test:**
```bash
curl -X POST http://localhost:3001/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"input_type":"message","content":"You won a prize! Click here now to claim it before it expires!"}'
```
Expected: JSON with `risk_level`, `red_flags`, `explanation`, `next_actions`.

### 3. Frontend

```bash
cd frontend
cp .env.example .env
# VITE_API_URL=http://localhost:3001 (default, no change needed for local)
# Optionally add VITE_POSTHOG_API_KEY
npm install
npm run dev
# → App running on http://localhost:5173
```

---

## Deployment

### Backend → Railway (or Render)

1. Push repo to GitHub.
2. Create a new Railway project → deploy from GitHub → select `backend/` as root.
3. Set environment variables:
   ```
   GROQ_API_KEY=...
   SUPABASE_URL=...
   SUPABASE_ANON_KEY=...
   FRONTEND_URL=https://your-app.vercel.app   ← no trailing slash
   PORT=3001
   ```
4. Note the deployed URL (e.g. `https://trustcheck-api.up.railway.app`).

### Frontend → Vercel

1. Import repo on [vercel.com](https://vercel.com) → set **Root Directory** to `frontend`.
2. Set environment variables:
   ```
   VITE_API_URL=https://trustcheck-api.up.railway.app
   VITE_POSTHOG_API_KEY=...        (optional)
   VITE_POSTHOG_HOST=https://app.posthog.com
   ```
3. Deploy.

---

## Verification checklist

- [ ] `supabase/schema.sql` ran → `checks` table + `daily_stats` view exist + 5 example rows seeded
- [ ] `GET http://localhost:3001/health` → `{"status":"ok"}`
- [ ] Smoke test `POST /api/analyze` returns valid JSON with `risk_level`
- [ ] Frontend loads at `:5173`, landing page shows 3 use-case cards
- [ ] Submit a test message → result page loads with badge, flags, actions
- [ ] Copy result URL → open incognito → result loads (shareable)
- [ ] `/examples` shows 5 cards, each links to a result page
- [ ] PostHog dashboard shows events (if key configured)
- [ ] Supabase `checks` table shows new rows after submissions
- [ ] `npm run build` in `frontend/` completes without errors

---

## Architecture

```
Browser → Vercel (React SPA)
              ↓ fetch /api/*
         Railway (Express)
           ├── POST /api/analyze → Groq (Llama 3.3) → Supabase (insert)
           ├── GET  /api/result/:id              → Supabase (select)
           └── GET  /api/examples                → hardcoded array
```

**Privacy:** Raw content is never stored. Only a SHA-256 hash of the input is saved alongside the AI analysis result.

**Rate limiting:** `/api/analyze` is limited to 10 requests/minute/IP.
