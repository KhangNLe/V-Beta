'use client';

import { SOLUTION_VIDEO_ACCEPT } from '@/lib/videoUpload';
import { buttons, card, colors, fontFamily } from '@/ui/appTheme';
import { useRef, useState } from 'react';

export const DISCUSSION_COMMENT_MAX_LENGTH = 250;

/**
 * Floating composer for a discussion post with optional comment text and an
 * optional beta video. The parent decides which API to call from what was filled in.
 *
 * @param {{
 *   heading?: string,
 *   placeholder?: string,
 *   disabled?: boolean,
 *   submitting?: boolean,
 *   status?: { type: 'success' | 'error', message: string, publicURL?: string | null } | null,
 *   onSubmit: (entry: { text: string, file: File | null }) => Promise<boolean>,
 *   onFileChange?: () => void,
 *   onCancel?: () => void,
 * }} props
 */
export default function DiscussionComposer({
  heading = 'Add a comment or solution beta',
  placeholder = 'Write a comment here!',
  disabled = false,
  submitting = false,
  status = null,
  onSubmit,
  onFileChange,
  onCancel,
}) {
  const [text, setText] = useState('');
  const [file, setFile] = useState(null);
  const fileInputRef = useRef(null);

  const charCount = text.length;
  const overLimit = charCount > DISCUSSION_COMMENT_MAX_LENGTH;
  const hasContent = !!text.trim() || !!file;
  const canSubmit = !disabled && !submitting && hasContent && !overLimit;

  const clearFile = () => {
    setFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSubmit = async () => {
    if (!canSubmit) return;
    const posted = await onSubmit({ text, file });
    if (posted) {
      setText('');
      clearFile();
    }
  };

  let submitLabel = 'Post';
  if (submitting) {
    submitLabel = file ? 'Uploading...' : 'Posting...';
  }

  return (
    <div
      style={{
        ...card.surface,
        position: 'sticky',
        bottom: '16px',
        zIndex: 10,
        padding: '16px',
        boxShadow: '0 8px 24px rgba(0, 0, 0, 0.12)',
        fontFamily,
      }}
    >
      <p style={{ margin: '0 0 10px', fontWeight: 600, color: colors.text }}>
        {heading}
      </p>
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        aria-label="Discussion comment"
        style={{
          width: '100%',
          minHeight: '72px',
          padding: '10px 12px',
          border: `1px solid ${overLimit ? colors.danger : colors.muted}`,
          borderRadius: '6px',
          fontFamily,
          fontSize: '0.875rem',
          resize: 'vertical',
        }}
      />
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '12px',
          flexWrap: 'wrap',
          marginTop: '8px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
          <label
            htmlFor="solution-beta-file-input"
            style={{
              ...buttons.secondary,
              opacity: disabled ? 0.6 : 1,
              cursor: disabled ? 'not-allowed' : 'pointer',
            }}
          >
            {file ? 'Change video' : 'Attach video'}
          </label>
          <input
            ref={fileInputRef}
            id="solution-beta-file-input"
            className="sr-only"
            type="file"
            accept={SOLUTION_VIDEO_ACCEPT}
            aria-label="Choose Video"
            disabled={disabled}
            onChange={(e) => {
              setFile(e.target.files?.[0] || null);
              onFileChange?.();
            }}
          />
          {file && (
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: '0.8rem',
                color: colors.muted,
                minWidth: 0,
              }}
            >
              <span
                style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
              >
                {file.name}
              </span>
              <button
                type="button"
                onClick={clearFile}
                aria-label="Remove video"
                style={{
                  border: 'none',
                  background: 'transparent',
                  color: colors.subtle,
                  cursor: 'pointer',
                  padding: 0,
                }}
              >
                ✕
              </button>
            </span>
          )}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span
            style={{
              fontSize: '0.8rem',
              color: overLimit ? colors.danger : colors.subtle,
            }}
          >
            {charCount}/{DISCUSSION_COMMENT_MAX_LENGTH}
          </span>
          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              disabled={submitting}
              style={buttons.secondary}
            >
              Cancel
            </button>
          )}
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!canSubmit}
            style={{
              ...buttons.primary,
              opacity: canSubmit ? 1 : 0.6,
              cursor: canSubmit ? 'pointer' : 'not-allowed',
            }}
          >
            {submitLabel}
          </button>
        </div>
      </div>
      <p style={{ margin: '6px 0 0', fontSize: '0.75rem', color: colors.subtle }}>
        Allowed video types: .mp4, .webm, .mov (iPhone videos are converted to MP4)
      </p>
      {status && (
        <div
          style={{
            marginTop: '8px',
            fontSize: '0.85rem',
            color: status.type === 'success' ? colors.primary : colors.danger,
          }}
        >
          {status.message}
          {status.publicURL && (
            <>
              {' '}
              <a href={status.publicURL} target="_blank" rel="noreferrer">
                View file
              </a>
            </>
          )}
        </div>
      )}
    </div>
  );
}
