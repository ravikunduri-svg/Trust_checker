const LABELS = {
  LOW: 'Low Risk',
  MEDIUM: 'Medium Risk',
  HIGH: 'High Risk',
};

const CLASSES = {
  LOW: 'badge-low',
  MEDIUM: 'badge-medium',
  HIGH: 'badge-high',
};

export default function RiskBadge({ level, score, size = 'md' }) {
  const label = LABELS[level] || level;
  const cls = CLASSES[level] || 'bg-gray-100 text-gray-800';

  const textSize = size === 'lg' ? 'text-lg font-bold px-5 py-2.5' : 'text-sm font-semibold px-3 py-1';

  return (
    <span className={`inline-flex items-center gap-2 rounded-full ${textSize} ${cls}`}>
      {label}
      {score !== undefined && (
        <span className="opacity-70 font-normal text-xs">
          {score}/100
        </span>
      )}
    </span>
  );
}
