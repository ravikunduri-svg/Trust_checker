import Groq from 'groq-sdk';

const groq = new Groq({ apiKey: process.env.GROQ_API_KEY });

const SYSTEM_PROMPT = `You are TrustCheck, a calm and helpful digital trust assistant.
Your job is to analyze suspicious messages, URLs, or screenshot descriptions and determine their trust level.

You must respond ONLY with valid JSON — no markdown, no explanation outside the JSON.

Return this exact structure:
{
  "risk_level": "LOW" | "MEDIUM" | "HIGH",
  "risk_score": <integer 0-100, higher = more risky>,
  "red_flags": [<short string per flag, max 8 flags>],
  "explanation": "<2-4 sentence plain-English explanation of your assessment>",
  "next_actions": [<actionable step strings, 2-5 items>],
  "confidence": "LOW" | "MEDIUM" | "HIGH"
}

Guidelines:
- Be calm and factual. Never alarmist.
- red_flags: only genuine signals, not hypotheticals. Empty array [] if none.
- next_actions: concrete, specific steps the user can take right now.
- If the content appears legitimate, say so clearly in explanation and return LOW risk.
- risk_score: 0-30 = LOW, 31-69 = MEDIUM, 70-100 = HIGH. risk_level must match.
- confidence reflects how certain you are given the information provided.`;

export async function analyzeContent(inputType, content) {
  const userPrompt = `Input type: ${inputType}\n\nContent to analyze:\n${content}`;

  const completion = await groq.chat.completions.create({
    model: 'llama-3.3-70b-versatile',
    messages: [
      { role: 'system', content: SYSTEM_PROMPT },
      { role: 'user', content: userPrompt },
    ],
    temperature: 0.2,
    max_tokens: 1024,
  });

  const raw = completion.choices[0]?.message?.content?.trim();
  if (!raw) throw new Error('Empty response from Groq');

  // Strip markdown code fences if model wraps response
  const cleaned = raw.replace(/^```(?:json)?\s*/i, '').replace(/\s*```$/, '').trim();

  const parsed = JSON.parse(cleaned);

  // Validate required fields
  const required = ['risk_level', 'risk_score', 'red_flags', 'explanation', 'next_actions', 'confidence'];
  for (const field of required) {
    if (!(field in parsed)) throw new Error(`Missing field in Groq response: ${field}`);
  }

  return parsed;
}
