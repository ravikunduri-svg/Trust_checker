import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import rateLimit from 'express-rate-limit';

import analyzeRouter from './routes/analyze.js';
import resultRouter from './routes/result.js';
import examplesRouter from './routes/examples.js';

const app = express();
const PORT = process.env.PORT || 3001;

// CORS — allow configured frontend origin
app.use(
  cors({
    origin: process.env.FRONTEND_URL || 'http://localhost:5173',
    methods: ['GET', 'POST'],
  })
);

app.use(express.json({ limit: '64kb' }));

// Rate limit the expensive endpoint
const analyzeLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 minute
  max: 10,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests. Please wait a minute and try again.' },
});

app.get('/health', (_req, res) => res.json({ status: 'ok' }));

app.use('/api/analyze', analyzeLimiter, analyzeRouter);
app.use('/api/result', resultRouter);
app.use('/api/examples', examplesRouter);

// Global error handler
app.use((err, _req, res, _next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

app.listen(PORT, () => {
  console.log(`TrustCheck API running on port ${PORT}`);
});
