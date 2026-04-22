'use client';

export default function GuestBanner({
  message = 'You are browsing as a guest. Log in or sign up to unlock interactive features.',
  style = {},
}) {
  return (
    <div
      style={{
        width: '100%',
        marginBottom: '20px',
        padding: '14px 18px',
        borderRadius: '10px',
        border: '1px solid var(--app-border-hairline)',
        background: 'var(--app-card-surface-bg)',
        color: 'var(--app-muted)',
        lineHeight: 1.5,
        fontWeight: 500,
        boxShadow: 'var(--app-card-shadow)',
        ...style,
      }}
    >
      {message}
    </div>
  );
}
