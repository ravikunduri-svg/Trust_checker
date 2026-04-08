import { Router } from 'express';
import { EXAMPLES } from '../data/examples.js';

const router = Router();

router.get('/', (_req, res) => {
  res.json(EXAMPLES);
});

export default router;
