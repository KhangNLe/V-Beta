'use client';

import { SOLUTION_VIDEO_ACCEPT } from '@/lib/videoUpload';
import { buttons, card, colors, fontFamily } from '@/ui/appTheme';
import { useId, useRef, useState } from 'react';

export const DISCUSSION_COMMENT_MAX_LENGTH = 250;

/**
 * Sticky/floating composer for a discussion post with optional comment text and an
 * optional beta video. Also supports an inline variant for replies under a post.
 *
 * The parent decides which API to call from what was filled in:
 * text only → comment; video only → beta save; both → mixed discussion-content
 * (after the signed-URL upload).
 *
 * @param {{
 *   heading?: string,
 *   placeholder?: string,
 *   submitLabel?: string,
 *   variant?: 'floating' | 'inline',
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
  submitLabel: submitLabelProp,
  variant = 'floating',
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
  const reactId = useId();
  const fileInputId = `discussion-file-input-${reactId}`;
  const isInline = variant === 'inline';

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

  let submitLabel = submitLabelProp || 'Post';
  if (submitting) {
    submitLabel = file ? 'Uploading...' : 'Posting...';
  }

  return (
    <div
      className="discussion-composer"
      style={{
        ...card.surface,
        ...(isInline
          ? {
              position: 'relative',
              bottom: 'auto',
              zIndex: 'auto',
              boxShadow: 'none',
              marginTop: '12px',
            }
          : {
              position: 'sticky',
              bottom: 'max(12px, env(safe-area-inset-bottom, 0px))',
              zIndex: 10,
              boxShadow: '0 8px 24px rgba(0, 0, 0, 0.12)',
            }),
        padding: isInline ? '12px' : '14px',
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
        aria-label={isInline ? 'Discussion reply' : 'Discussion comment'}
        style={{
          width: '100%',
          minHeight: isInline ? '56px' : '72px',
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
            htmlFor={fileInputId}
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
            id={fileInputId}
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
