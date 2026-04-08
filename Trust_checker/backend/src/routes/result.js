import { Router } from 'express';
import { supabase } from '../lib/supabase.js';

const router = Router();

router.get('/:id', async (req, res) => {
  const { id } = req.params;

  // Basic UUID format guard
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (!uuidRegex.test(id)) {
    return res.status(400).json({ error: 'Invalid result ID format' });
  }

  const { data, error } = await supabase
    .from('checks')
    .select('*')
    .eq('id', id)
    .maybeSingle();

  if (error) {
    console.error('Supabase fetch error:', error.message);
    return res.status(500).json({ error: 'Failed to fetch result' });
  }

  if (!data) {
    return res.status(404).json({ error: 'Result not found' });
  }

  return res.json(data);
});

export default router;
