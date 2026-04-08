import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { analyze } from '../lib/api';
import { track } from '../lib/posthog';
import LoadingState from '../components/LoadingState';
import ErrorBanner from '../components/ErrorBanner';

const TABS = [
  { id: 'message',    label: 'Message',    placeholder: 'Paste the suspicious message here…' },
  { id: 'url',        label: 'URL',        placeholder: 'Paste the suspicious URL here…' },
  { id: 'screenshot', label: 'Screenshot', placeholder: 'Describe what the screenshot shows. E.g. "A bank transfer confirmation for $2,400 from account ending 4821, shared on WhatsApp by someone claiming to be a buyer."' },
];

export default function Check() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('message');
  const [content, setContent] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState('');

  // Reset content and errors when tab changes
  useEffect(() => {
    setContent('');
    setFieldError('');
    setApiError('');
  }, [tab]);

  async function handleSubmit(e) {
    e.preventDefault();
    setFieldError('');
    setApiError('');

    if (!content.trim()) {
      setFieldError('Please enter something to analyze.');
      return;
    }
    if (content.trim().length < 10) {
      setFieldError('Please provide at least 10 characters for a meaningful analysis.');
      return;
    }

    track('trustcheck_submitted', { input_type: tab });
    setLoading(true);

    try {
      const result = await analyze(tab, content.trim());
      navigate(`/result/${result.id}`);
    } catch (err) {
      setApiError(err.message || 'Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  const currentTab = TABS.find((t) => t.id === tab);

  if (loading) return <div className="max-w-2xl mx-auto px-4 py-8"><LoadingState /></div>;

  return (
    <main className="max-w-2xl mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">What do you want to check?</h1>
      <p className="text-gray-500 dark:text-gray-400 mb-8 text-sm">
        Your content is never stored — only a privacy-safe hash is kept.
      </p>

      {/* Tabs */}
      <div className="flex gap-1 mb-6 border-b border-gray-200 dark:border-gray-800">
        {TABS.map((t) => (
          <button
            key={t.id}
            onClick={() => setTab(t.id)}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors -mb-px ${
              tab === t.id
                ? 'border-brand-500 text-brand-600 dark:text-brand-400'
                : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit} noValidate>
        <textarea
          value={content}
          onChange={(e) => { setContent(e.target.value); setFieldError(''); }}
          placeholder={currentTab.placeholder}
          rows={tab === 'screenshot' ? 6 : 5}
          maxLength={5000}
          className={`w-full rounded-lg border p-4 text-sm bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100
            placeholder-gray-400 dark:placeholder-gray-600 resize-y focus:outline-none focus:ring-2 focus:ring-brand-500
            ${fieldError
              ? 'border-red-400 dark:border-red-600'
              : 'border-gray-300 dark:border-gray-700'
            }`}
        />
        <div className="flex justify-between items-center mt-1 mb-4">
          {fieldError ? (
            <p className="text-xs text-red-500">{fieldError}</p>
          ) : (
            <span />
          )}
          <span className="text-xs text-gray-400">{content.length}/5000</span>
        </div>

        {apiError && (
          <div className="mb-4">
            <ErrorBanner message={apiError} onDismiss={() => setApiError('')} />
          </div>
        )}

        <button
          type="submit"
          className="w-full bg-brand-500 hover:bg-brand-600 text-white font-semibold py-3 rounded-lg transition-colors"
        >
          Analyze
        </button>
      </form>
    </main>
  );
}
