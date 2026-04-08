export default function LoadingState() {
  return (
    <div className="flex flex-col items-center justify-center py-20 gap-4 text-gray-500 dark:text-gray-400">
      <svg
        className="w-10 h-10 animate-spin text-brand-500"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path
          className="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
        />
      </svg>
      <p className="text-sm">Analyzing for trust signals…</p>
    </div>
  );
}
