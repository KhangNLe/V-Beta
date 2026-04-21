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
        border: '1px solid #d4d4d8',
        background: '#ffffff',
        color: '#52525b',
        lineHeight: 1.5,
        fontWeight: 500,
        boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
        ...style,
      }}
    >
      {message}
    </div>
  );
}
