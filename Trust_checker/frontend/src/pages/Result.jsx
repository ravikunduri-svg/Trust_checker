import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getResult } from '../lib/api';
import { track } from '../lib/posthog';
import RiskBadge from '../components/RiskBadge';
import LoadingState from '../components/LoadingState';
import ErrorBanner from '../components/ErrorBanner';

const TYPE_LABELS = { message: 'Message', url: 'URL', screenshot: 'Screenshot' };

export default function Result() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    setResult(null);
    setError('');
    setLoading(true);

    getResult(id)
      .then((data) => {
        setResult(data);
        track('trustcheck_result_viewed', { risk_level: data.risk_level, input_type: data.input_type });
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  function handleShare() {
    navigator.clipboard.writeText(window.location.href).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
    track('trustcheck_result_shared', { result_id: id });
  }

  function handleCheckAnother() {
    track('trustcheck_checked_again');
    navigate('/check');
  }

  if (loading) return <div className="max-w-2xl mx-auto px-4 py-10"><LoadingState /></div>;

  if (error) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-10">
        <ErrorBanner message={error} />
        <Link to="/check" className="block mt-4 text-sm text-brand-500 hover:underline">← Check something else</Link>
      </div>
    );
  }

  const flags = Array.isArray(result.red_flags) ? result.red_flags : [];
  const actions = Array.isArray(result.next_actions) ? result.next_actions : [];

  return (
    <main className="max-w-2xl mx-auto px-4 py-10 space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <p className="text-xs text-gray-400 dark:text-gray-500 uppercase tracking-wide mb-1">
            {TYPE_LABELS[result.input_type] || result.input_type} analysis
          </p>
          <RiskBadge level={result.risk_level} score={result.risk_score} size="lg" />
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleShare}
            className="text-sm border border-gray-300 dark:border-gray-700 px-3 py-1.5 rounded-md hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
          >
            {copied ? '✓ Copied' : 'Share link'}
          </button>
          <button
            onClick={handleCheckAnother}
            className="text-sm bg-brand-500 hover:bg-brand-600 text-white px-3 py-1.5 rounded-md transition-colors"
          >
            Check another
          </button>
        </div>
      </div>

      {/* Explanation */}
      <section className="bg-gray-50 dark:bg-gray-900 rounded-xl p-5 border border-gray-200 dark:border-gray-800">
        <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">What we found</h2>
        <p className="text-gray-800 dark:text-gray-200 text-sm leading-relaxed">{result.explanation}</p>
      </section>

      {/* Red flags */}
      {flags.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Red flags ({flags.length})</h2>
          <ul className="space-y-2">
            {flags.map((flag, i) => (
              <li key={i} className="flex items-start gap-2.5 text-sm">
                <span className="text-red-500 mt-0.5">⚠</span>
                <span className="text-gray-700 dark:text-gray-300">{flag}</span>
              </li>
            ))}
          </ul>
        </section>
      )}

      {flags.length === 0 && (
        <section>
          <p className="text-sm text-green-600 dark:text-green-400 flex items-center gap-2">
            <span>✓</span> No red flags detected.
          </p>
        </section>
      )}

      {/* Next actions */}
      {actions.length > 0 && (
        <section>
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">What to do</h2>
          <ol className="space-y-2 list-none">
            {actions.map((action, i) => (
              <li key={i} className="flex items-start gap-3 text-sm">
                <span className="bg-brand-500 text-white rounded-full w-5 h-5 flex items-center justify-center shrink-0 text-xs font-semibold mt-0.5">
                  {i + 1}
                </span>
                <span className="text-gray-700 dark:text-gray-300">{action}</span>
              </li>
            ))}
          </ol>
        </section>
      )}

      {/* Confidence + footer */}
      <p className="text-xs text-gray-400 dark:text-gray-500 pt-2 border-t border-gray-100 dark:border-gray-800">
        Confidence: {result.confidence} · Analyzed by Groq / Llama 3.3 · This is not legal advice.
      </p>
    </main>
  );
}
