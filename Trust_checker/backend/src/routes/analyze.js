import { Router } from 'express';
import { analyzeContent } from '../lib/groq.js';
import { supabase } from '../lib/supabase.js';
import { sha256 } from '../lib/hash.js';

const router = Router();

router.post('/', async (req, res) => {
  const { input_type, content, session_id, referrer } = req.body;

  // Validation
  if (!input_type || !['message', 'url', 'screenshot'].includes(input_type)) {
    return res.status(400).json({ error: 'input_type must be message, url, or screenshot' });
  }
  if (!content || typeof content !== 'string' || content.trim().length === 0) {
    return res.status(400).json({ error: 'content is required' });
  }
  if (content.length > 5000) {
    return res.status(400).json({ error: 'content exceeds 5000 character limit' });
  }

  const trimmed = content.trim();
  const inputHash = sha256(trimmed);

  // Detect device type from user-agent
  const ua = req.headers['user-agent'] || '';
  const device_type = /mobile|android|iphone|ipad/i.test(ua) ? 'mobile' : 'desktop';

  let analysis;
  try {
    analysis = await analyzeContent(input_type, trimmed);
  } catch (err) {
    console.error('Groq analysis error:', err.message);
    return res.status(502).json({ error: 'Analysis service unavailable. Please try again.' });
  }

  const { data, error } = await supabase
    .from('checks')
    .insert({
      input_type,
      input_hash: inputHash,
      risk_level: analysis.risk_level,
      risk_score: analysis.risk_score,
      red_flags: analysis.red_flags,
      explanation: analysis.explanation,
      next_actions: analysis.next_actions,
      confidence: analysis.confidence,
      session_id: session_id || null,
      device_type,
      referrer: referrer || req.headers['referer'] || null,
    })
    .select()
    .single();

  if (error) {
    console.error('Supabase insert error:', error.message);
    return res.status(500).json({ error: 'Failed to save result. Please try again.' });
  }

  return res.json(data);
});

export default router;
