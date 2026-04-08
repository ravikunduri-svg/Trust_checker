import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getExamples } from '../lib/api';
import { track } from '../lib/posthog';
import RiskBadge from '../components/RiskBadge';
import LoadingState from '../components/LoadingState';
import ErrorBanner from '../components/ErrorBanner';

const TYPE_ICONS = { message: '📩', url: '🔗', screenshot: '🖼️' };

export default function Examples() {
  const [examples, setExamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getExamples()
      .then(setExamples)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="max-w-4xl mx-auto px-4 py-10"><LoadingState /></div>;

  if (error) return (
    <div className="max-w-4xl mx-auto px-4 py-10">
      <ErrorBanner message={error} />
    </div>
  );

  return (
    <main className="max-w-4xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Example analyses</h1>
      <p className="text-gray-500 dark:text-gray-400 mb-8 text-sm">
        See how TrustCheck analyzes common scam patterns.
      </p>

      <div className="grid sm:grid-cols-2 gap-5">
        {examples.map((ex) => (
          <Link
            key={ex.id}
            to={`/result/${ex.id}`}
            onClick={() => track('trustcheck_example_viewed', { example_id: ex.id })}
            className="border border-gray-200 dark:border-gray-800 rounded-xl p-5 hover:border-brand-500 dark:hover:border-brand-500 transition-colors bg-white dark:bg-gray-900 group"
          >
            <div className="flex items-start justify-between gap-3 mb-3">
              <span className="text-xl">{TYPE_ICONS[ex.input_type] || '📄'}</span>
              <RiskBadge level={ex.risk_level} score={ex.risk_score} />
            </div>
            <h3 className="font-semibold text-gray-900 dark:text-white text-sm mb-1 group-hover:text-brand-600 dark:group-hover:text-brand-400 transition-colors">
              {ex.label}
            </h3>
            <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-2">{ex.explanation}</p>
          </Link>
        ))}
      </div>

      <div className="mt-10 text-center">
        <Link
          to="/check"
          className="inline-block bg-brand-500 hover:bg-brand-600 text-white font-semibold px-6 py-3 rounded-lg transition-colors"
        >
          Check something of your own
        </Link>
      </div>
    </main>
  );
}
