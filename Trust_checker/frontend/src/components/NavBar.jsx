import { Link, useNavigate } from 'react-router-dom';

export default function NavBar() {
  const navigate = useNavigate();

  return (
    <nav className="border-b border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-950">
      <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 font-semibold text-gray-900 dark:text-white">
          <svg className="w-6 h-6 text-brand-500" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="32" height="32" rx="8" fill="currentColor"/>
            <path d="M16 5L7 9v8c0 5 4 9.5 9 11 5-1.5 9-6 9-11V9L16 5z" fill="white" fillOpacity="0.9"/>
            <path d="M13 16l2.5 2.5L20 13" stroke="#0ea5e9" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          TrustCheck
        </Link>

        <div className="flex items-center gap-4 text-sm">
          <Link to="/examples" className="text-gray-500 hover:text-gray-900 dark:hover:text-white transition-colors">
            Examples
          </Link>
          <button
            onClick={() => navigate('/check')}
            className="bg-brand-500 hover:bg-brand-600 text-white px-3 py-1.5 rounded-md transition-colors"
          >
            Check something
          </button>
        </div>
      </div>
    </nav>
  );
}
