'use client';

export default function GuestBanner({
  message = 'You are browsing as a guest. Log in or sign up to unlock interactive features.',
  style = {},
  className = '',
}) {
  return (
    <div
      className={className}
      style={{
        width: '100%',
        marginBottom: '20px',
        padding: '12px 14px',
        borderRadius: '10px',
        border: '1px solid var(--app-border-hairline)',
        background: 'var(--app-card-surface-bg)',
        color: 'var(--app-muted)',
        lineHeight: 1.5,
        fontWeight: 500,
        fontSize: '0.9375rem',
        boxShadow: 'var(--app-card-shadow)',
        ...style,
      }}
    >
      {message}
    </div>
  );
}
