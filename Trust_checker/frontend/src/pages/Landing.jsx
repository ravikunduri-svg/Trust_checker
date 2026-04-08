import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { track } from '../lib/posthog';

const USE_CASES = [
  {
    icon: '📩',
    title: 'Suspicious message',
    description: 'That recruiter DM, "you won a prize" text, or urgent email asking you to act fast.',
  },
  {
    icon: '🔗',
    title: 'Sketchy link',
    description: 'A payment link that looks almost right, a login page from an unexpected sender.',
  },
  {
    icon: '🖼️',
    title: 'Screenshot claim',
    description: 'Someone shared a screenshot of a bank transfer, an offer letter, or a conversation.',
  },
];

export default function Landing() {
  const navigate = useNavigate();

  useEffect(() => {
    track('trustcheck_landed');
  }, []);

  function handleStart() {
    track('trustcheck_started');
    navigate('/check');
  }

  return (
    <main className="max-w-4xl mx-auto px-4 py-16">
      {/* Hero */}
      <div className="text-center mb-16">
        <h1 className="text-4xl sm:text-5xl font-bold text-gray-900 dark:text-white mb-4 leading-tight">
          Is this legit or a scam?
        </h1>
        <p className="text-lg text-gray-500 dark:text-gray-400 mb-8 max-w-xl mx-auto">
          Paste a suspicious message, link, or describe a screenshot. TrustCheck analyzes it in seconds and tells you exactly what to do.
        </p>
        <button
          onClick={handleStart}
          className="bg-brand-500 hover:bg-brand-600 text-white font-semibold px-8 py-3.5 rounded-lg text-lg transition-colors shadow-sm"
        >
          Check it now — it's free
        </button>
        <p className="text-xs text-gray-400 mt-3">Your content is never stored. Only a privacy-safe hash is kept.</p>
      </div>

      {/* Use case cards */}
      <div className="grid sm:grid-cols-3 gap-6 mb-16">
        {USE_CASES.map((uc) => (
          <div
            key={uc.title}
            className="border border-gray-200 dark:border-gray-800 rounded-xl p-6 bg-gray-50 dark:bg-gray-900"
          >
            <div className="text-3xl mb-3">{uc.icon}</div>
            <h3 className="font-semibold text-gray-900 dark:text-white mb-1">{uc.title}</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400">{uc.description}</p>
          </div>
        ))}
      </div>

      {/* Trust bar */}
      <div className="text-center text-sm text-gray-400 dark:text-gray-500">
        Powered by Groq + Llama 3.3 · No account required · Results are shareable
      </div>
    </main>
  );
}
