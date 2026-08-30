'use client';

import {
  deleteSolutionBetaFromDatabase,
  requestSignedUploadUrl,
  saveSolutionBetaToDatabase,
  uploadSolutionBeta,
} from "@/api/solutionBeta";
import { fetchProblemForUser } from "@/api/wallSections";
import {
  addUserSuggestedGrade,
  deleteUserComment,
  postCommentForUser,
} from "@/api/comments";
import {
  createContentReport,
  REPORT_CATEGORIES,
  REPORT_REASON_MAX_LENGTH,
} from "@/api/reports";
import PageLoader from "@/components/ui/PageLoader";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useRequireAuth } from '@/hooks/useRequireAuth';
import { getAccountId, getAccountRole } from '@/lib/accountSession';
import { discussionDeletionReason } from '@/lib/discussionDeletion';
import { buttons, card, colors, layout, fontFamily } from '@/ui/appTheme';
import { ArrowLeftIcon, MoreVertical } from 'lucide-react';
import { useParams, useRouter } from 'next/navigation';
import { useEffect, useMemo, useRef, useState } from 'react';
import { toast } from 'react-toastify';
import GuestBanner from '@/components/GuestBanner';

/** @param {string | null | undefined} raw */
function formatCommentDate(raw) {
  if (!raw) return 'Recently';
  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) return String(raw);
  return parsed.toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

/** @param {string | null | undefined} url */
function inferVideoMimeType(url) {
  const value = (url || '').toLowerCase();
  return value.endsWith('.webm') ? 'video/webm' : 'video/mp4';
}

/** @param {unknown} comment */
function getCommentAuthorId(comment) {
  if (!comment || typeof comment !== 'object') return null;
  const commentRecord = /** @type {Record<string, unknown>} */ (comment);
  const directAuthorId =
    commentRecord.authorId ?? commentRecord.userId ?? commentRecord.uid;
  if (
    typeof directAuthorId === 'string' ||
    typeof directAuthorId === 'number'
  ) {
    return String(directAuthorId);
  }
  const nestedAuthor = commentRecord.author;
  if (nestedAuthor && typeof nestedAuthor === 'object') {
    const nestedRecord = /** @type {Record<string, unknown>} */ (nestedAuthor);
    const nestedId = nestedRecord.id ?? nestedRecord.uid ?? nestedRecord.userId;
    if (typeof nestedId === 'string' || typeof nestedId === 'number') {
      return String(nestedId);
    }
  }
  return null;
}

/** @param {unknown} currentUser */
function getCurrentUserId(currentUser) {
  if (!currentUser || typeof currentUser !== 'object') return null;
  const userRecord = /** @type {Record<string, unknown>} */ (currentUser);
  const idValue = userRecord.uid ?? userRecord.id ?? userRecord.userId;
  if (typeof idValue === 'string' || typeof idValue === 'number') {
    return String(idValue);
  }
  return null;
}

/** @param {unknown} error */
function extractErrorMessage(error) {
  if (error instanceof Error) return error.message;
  if (error && typeof error === 'object' && 'message' in error) {
    const candidate = error.message;
    return typeof candidate === 'string' && candidate.trim()
      ? candidate
      : 'Unexpected error shape.';
  }
  return typeof error === 'string' && error.trim() ? error : 'Unknown error.';
}
/** @param {string | number | null | undefined} raw */
function parsePositiveNumberId(raw) {
  if (raw == null || raw === '') return null;
  const parsed = Number(raw);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

/** @param {unknown} discussionItem */
function getDiscussionId(discussionItem) {
  if (!discussionItem || typeof discussionItem !== 'object') return null;
  const discussionRecord = /** @type {Record<string, unknown>} */ (discussionItem);
  const discussionId =
    discussionRecord.discussionId ?? discussionRecord.id ?? discussionRecord.commentId;
  return parsePositiveNumberId(discussionId);
}

/** @param {unknown} discussionItem */
function getDiscussionContent(discussionItem) {
  if (!discussionItem || typeof discussionItem !== 'object') return '';
  const discussionRecord = /** @type {Record<string, unknown>} */ (discussionItem);
  const value = discussionRecord.discussionContent ?? discussionRecord.comment ?? '';
  return typeof value === 'string' ? value : '';
}

/** @param {unknown} discussionItem */
function getDiscussionType(discussionItem) {
  if (!discussionItem || typeof discussionItem !== 'object') return '';
  const discussionRecord = /** @type {Record<string, unknown>} */ (discussionItem);
  const value = discussionRecord.discussionType;
  return typeof value === 'string' ? value.toUpperCase() : '';
}

/** @param {unknown} discussionItem */
function getDiscussionMediaUrl(discussionItem) {
  if (!discussionItem || typeof discussionItem !== 'object') return '';
  const discussionRecord = /** @type {Record<string, unknown>} */ (discussionItem);
  const value = discussionRecord.videoURL ?? discussionRecord.discussionContent ?? '';
  return typeof value === 'string' ? value : '';
}

function buildShortUploadFileName(originalFileName, problemId) {
  const name = (originalFileName || '').toLowerCase();
  const extension = name.endsWith('.webm') ? 'webm' : 'mp4';
  return `beta_${problemId}.${extension}`;
}
export default function ProblemPage() {
  const router = useRouter();
  const params = useParams();
  const { user, account, ready } = useRequireAuth({
    redirectMode: "push",
    requireAuth: false,
    requireEmailVerified: true,
  });

  const [problem, setProblem] = useState(null);
  const [fetchError, setFetchError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [commentText, setCommentText] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);
  const [perceivedGrade, setPerceivedGrade] = useState('VB');
  const [entryMode, setEntryMode] = useState('comment'); // "comment" | "file"
  const [solutionFile, setSolutionFile] = useState(null);
  const isAdmin = getAccountRole(account).toUpperCase().includes('ADMIN');
  const currentUserId = useMemo(() => {
    const accountId = getAccountId(account);
    if (accountId != null) return String(accountId);
    return getCurrentUserId(user);
  }, [account, user]);
  const [uploadingSolution, setUploadingSolution] = useState(false);
  const [uploadStatus, setUploadStatus] = useState(null);
  const [reportTarget, setReportTarget] = useState(null);
  const [reportCategory, setReportCategory] = useState('');
  const [reportReason, setReportReason] = useState('');
  const [submittingReport, setSubmittingReport] = useState(false);
  const fileInputRef = useRef(null);

  const clearSelectedSolutionFile = () => {
    setSolutionFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const refreshProblem = async () => {
    const refreshedProblem = await fetchProblemForUser(
      user,
      wallSectionID,
      problemId,
    );
    setProblem(refreshedProblem);
  };

  const handleDeleteComment = async (targetComment) => {
    if (!user || !problemId) {
      return;
    }

    const authorId = getCommentAuthorId(targetComment);
    const isOwner = !!currentUserId && !!authorId && currentUserId === authorId;
    const canDeleteComment = isAdmin || isOwner;
    if (!canDeleteComment) {
      return;
    }

    const payloadAuthorId = parsePositiveNumberId(authorId);
    const discussionId = getDiscussionId(targetComment);
    const commentContent = getDiscussionContent(targetComment).trim();
    if (!payloadAuthorId || !discussionId || !commentContent) {
      toast.error("Unable to determine comment payload for deletion.");
      return;
    }

    try {
      await deleteUserComment(user, {
        authorId: payloadAuthorId,
        problemId,
        discussionId,
        commentContent,
        deletedReason: discussionDeletionReason(isOwner),
      });
      await refreshProblem();
      toast.success("Comment deleted.");
    } catch (err) {
      toast.error(`Failed to delete comment: ${extractErrorMessage(err)}`);
    }
  };

  const handleDeleteSolutionBeta = async (targetComment) => {
    if (!targetComment || !user || !problemId) {
      return;
    }
    const mediaUrl = getDiscussionMediaUrl(targetComment);
    if (!mediaUrl) return;

    const authorId = getCommentAuthorId(targetComment);
    const isOwner = !!currentUserId && !!authorId && currentUserId === authorId;
    const canDeleteSolutionBeta = isAdmin || isOwner;
    if (!canDeleteSolutionBeta) {
      return;
    }

    const payloadUserId = parsePositiveNumberId(authorId);
    const discussionId = getDiscussionId(targetComment);
    if (!payloadUserId || !discussionId) {
      toast.error('Unable to determine owner id for solution beta deletion.');
      return;
    }

    try {
      await deleteSolutionBetaFromDatabase(user, {
        userId: payloadUserId,
        problemId,
        discussionId,
        publicUrl: mediaUrl,
        deleteReason: discussionDeletionReason(isOwner),
      });
      await refreshProblem();
      clearSelectedSolutionFile();
      toast.success('Solution beta deletion requested.');
    } catch (err) {
      const message = extractErrorMessage(err);
      toast.error(`Failed to delete solution beta: ${message}`);
    }
  };

  const openReportDialog = (targetComment) => {
    setReportTarget(targetComment);
    setReportCategory('');
    setReportReason('');
  };

  const closeReportDialog = () => {
    if (submittingReport) return;
    setReportTarget(null);
    setReportReason('');
    setReportCategory('');
  };

  const handleSubmitReport = async (event) => {
    event.preventDefault();
    if (!user || !reportTarget || submittingReport) return;

    const discussionId = getDiscussionId(reportTarget);
    const reason = reportReason.trim();
    if (!discussionId) {
      toast.error('Unable to determine discussion to report.');
      return;
    }
    if (!reportCategory) {
      toast.error('Please choose a report category.');
      return;
    }
    if (!reason) {
      toast.error('Please enter a reason for this report.');
      return;
    }
    if (reason.length > REPORT_REASON_MAX_LENGTH) {
      toast.error(
        `Report reason must be ${REPORT_REASON_MAX_LENGTH} characters or fewer.`,
      );
      return;
    }

    try {
      setSubmittingReport(true);
      await createContentReport(user, {
        reportTargetType: 'DISCUSSION',
        reportReason: reason,
        reportCategoryName: reportCategory,
        targetId: discussionId,
      });
      toast.success('Report submitted.');
      setReportTarget(null);
      setReportReason('');
      setReportCategory('');
    } catch (err) {
      toast.error(`Failed to submit report: ${extractErrorMessage(err)}`);
    } finally {
      setSubmittingReport(false);
    }
  };

  const handleSuggestPerceivedGrade = async () => {
    if (!user || !problemId || !perceivedGrade) return;
    try {
      await addUserSuggestedGrade(
        user,
        { perceivedGrade: perceivedGrade.trim() },
        problemId
      );
      await refreshProblem();
      toast.success("Suggested grade submitted.");
    } catch (err) {
      toast.error(`Failed to suggest grade: ${extractErrorMessage(err)}`);
    }
  };

  const rawWallSectionID = params?.wallSectionID;
  const rawProblemId = params?.problemId;

  const wallSectionID = useMemo(() => {
    const normalized = Array.isArray(rawWallSectionID)
      ? rawWallSectionID[0]
      : rawWallSectionID;
    if (!normalized) return null;
    const parsed = Number(normalized);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }, [rawWallSectionID]);

  const problemId = useMemo(() => {
    const normalized = Array.isArray(rawProblemId)
      ? rawProblemId[0]
      : rawProblemId;
    if (!normalized) return null;
    const parsed = Number(normalized);
    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }, [rawProblemId]);

  useEffect(() => {
    if (!ready) return;
    if (!wallSectionID || !problemId) {
      setLoading(false);
      setFetchError('Invalid wall section or problem id.');
      return;
    }

    let cancelled = false;
    (async () => {
      try {
        setLoading(true);
        const problemData = await fetchProblemForUser(
          user,
          wallSectionID,
          problemId,
        );
        if (cancelled) return;

        setProblem(problemData);
        setFetchError(null);
      } catch (err) {
        toast.error('Failed to fetch problem data:', err);
        if (!cancelled)
          setFetchError(err instanceof Error ? err.message : 'Unknown error');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [ready, user, wallSectionID, problemId]);

  const handleBackToSection = () => {
    router.push(`/wall/${wallSectionID}`);
  };

  const handlePostComment = async () => {
    if (!user) {
      toast.error('Please sign in to post a comment.');
      return;
    }

    const charCount = commentText.length;
    if (!commentText.trim() || charCount > 250) return;

    setSubmittingComment(true);
    try {
      await postCommentForUser(user, problemId, commentText.trim());
      setCommentText('');
      const refreshedProblem = await fetchProblemForUser(
        user,
        wallSectionID,
        problemId,
      );
      setProblem(refreshedProblem);
      toast.success('Comment posted successfully!');
    } catch (err) {
      toast.error('Failed to post comment:', err);
      setFetchError(
        err instanceof Error ? err.message : 'Failed to post comment',
      );
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleUploadSolutionBeta = async () => {
    if (!user) {
      toast.error('Please sign in to upload a solution beta.');
      return;
    }

    if (!solutionFile || !user || !problemId || !wallSectionID) return;
    setUploadingSolution(true);
    setUploadStatus(null);

    try {
      const shortenedUploadName = buildShortUploadFileName(
        solutionFile.name,
        problemId,
      );
      const requestPayload = {
        fileName: shortenedUploadName,
        contentType: solutionFile.type || 'application/octet-stream',
        problemId,
        wallSectionId: wallSectionID,
      };

      const signedData = await requestSignedUploadUrl(user, requestPayload);
      if (!signedData?.signedURL) {
        throw new Error('Signed URL response is missing signedURL.');
      }

      const uploadObjectName =
        signedData.uploadObjectName || signedData.objectName || '';
      if (!uploadObjectName) {
        throw new Error('Signed URL response is missing uploadObjectName.');
      }

      await uploadSolutionBeta(solutionFile, signedData);

      let verificationMessage = 'Uploaded to bucket successfully.';
      if (signedData.publicURL) {
        try {
          const verifyResponse = await fetch(signedData.publicURL, {
            method: 'HEAD',
          });
          verificationMessage = verifyResponse.ok
            ? 'Uploaded and verified from bucket.'
            : 'Uploaded, but public URL verification did not return success.';
        } catch {
          verificationMessage =
            'Uploaded, but public URL verification was unavailable (often caused by browser/network policy).';
        }
      }

      try {
        await saveSolutionBetaToDatabase(user, {
          problemId,
          objectFileName: uploadObjectName,
          videoURL: signedData.publicURL || '',
        });
        verificationMessage = `${verificationMessage} Metadata saved to database.`;
      } catch (dbError) {
        const dbMessage = extractErrorMessage(dbError);
        verificationMessage = `${verificationMessage} Upload succeeded, but DB save failed: ${dbMessage}. Please contact the developer team.`;
      }

      try {
        await refreshProblem();
      } catch (refreshError) {
        const refreshMessage = extractErrorMessage(refreshError);
        verificationMessage = `${verificationMessage} Saved data, but failed to refresh page data: ${refreshMessage}`;
      }

      setUploadStatus({
        type: verificationMessage.includes('DB save failed')
          ? 'error'
          : 'success',
        message: verificationMessage,
        publicURL: signedData.publicURL || null,
      });
      clearSelectedSolutionFile();
    } catch (err) {
      setUploadStatus({
        type: 'error',
        message: `Upload failed before completion: ${
          err instanceof Error ? err.message : 'Unexpected upload error.'
        }`,
        publicURL: null,
      });
    } finally {
      setUploadingSolution(false);
    }
  };

  if (!ready) return <PageLoader message="Loading…" />;
  if (loading) return <PageLoader message="Loading problem…" />;

  const isSignedIn = !!user;
  const commentCharCount = commentText.length;
  const commentOverLimit = commentCharCount > 250;
  const canPostComment =
    !!user && !!commentText.trim() && !commentOverLimit && !submittingComment;
  const reportReasonOverLimit = reportReason.length > REPORT_REASON_MAX_LENGTH;
  const canSubmitReport =
    !!reportCategory &&
    !!reportReason.trim() &&
    !reportReasonOverLimit &&
    !submittingReport;

  return (
    <main style={layout.main}>
      <div style={layout.maxWidth960}>

        {/* Guest View Message */}
        {!isSignedIn && (
          <GuestBanner
            message="You are viewing as a guest. Sign in to comment, upload beta, suggest a difficulty, and access your account."
            className="mb-5"
          />
        )}
        
        {/* Back to Wall Section Button */}
        <Button
          type="button"
          variant="ghost"
          onClick={handleBackToSection}
          className="mb-4 text-muted-foreground hover:text-foreground"
          aria-label="Back to wall section"
        >
          <ArrowLeftIcon className="size-4" />
        </Button>


        {fetchError && (
          <div
            style={{
              color: colors.danger,
              background: colors.dangerBg,
              border: `1px solid ${colors.dangerBorder}`,
              borderRadius: '8px',
              padding: '12px 14px',
              marginBottom: '20px',
            }}
          >
            {fetchError}
          </div>
        )}

        {problem && (
          <>
            {/* Problem Details Card */}
            <section
              style={{
                ...card.surface,
                position: 'relative',
                padding: '22px 22px 22px 20px',
                marginBottom: '28px',
                overflow: 'hidden',
                fontFamily,
              }}
            >
              <div style={card.accentBar} aria-hidden />
              <h1
                style={{
                  margin: '0 0 8px',
                  fontSize: '1.75rem',
                  fontWeight: 700,
                  color: colors.text,
                }}
              >
                {problem.holdColor}
              </h1>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'baseline',
                  gap: '12px',
                  flexWrap: 'wrap',
                }}
              >
                <p style={{ margin: 0, color: colors.muted, lineHeight: 1.55 }}>
                  Assigned Grade: {problem.assignedGrade || 'V?'}
                </p>
                <p style={{ margin: 0, color: colors.muted, lineHeight: 1.55 }}>
                  Perceived Difficulty: {problem.perceiveGrade.trim() || 'N/A'}
                </p>
              </div>
              <p
                style={{
                  margin: 0,
                  color: colors.muted,
                  lineHeight: 1.55,
                  maxWidth: '65ch',
                }}
              >
                Info: {problem.info || 'No problem notes available.'}
              </p>
            </section>

            {/* Discussion Section */}
            <section style={{ marginBottom: '28px' }}>
              <h2
                style={{
                  margin: '0 0 16px',
                  fontSize: '1.125rem',
                  fontWeight: 600,
                  color: colors.text,
                }}
              >
                User Discussion
              </h2>

              {/* Comments List */}
              {problem.discussion && problem.discussion.length > 0 ? (
                <div
                  style={{
                    ...card.surface,
                    marginBottom: '24px',
                    overflow: 'hidden',
                  }}
                >
                  {problem.discussion.map((comment, index) =>
                    (() => {
                      const authorId = getCommentAuthorId(comment);
                      const discussionType = getDiscussionType(comment);
                      const discussionContent = getDiscussionContent(comment);
                      const mediaUrl = getDiscussionMediaUrl(comment);
                      const isOwner =
                        !!currentUserId &&
                        !!authorId &&
                        currentUserId === authorId;
                      const canDeleteComment = isAdmin || isOwner;
                      const canReportComment = isSignedIn && !isOwner;
                      const showDiscussionMenu =
                        isSignedIn && (canDeleteComment || canReportComment);
                      const isSolutionBeta =
                        discussionType === 'BETA' ||
                        (discussionType !== 'COMMENT' &&
                          !discussionContent &&
                          !!mediaUrl);
                      return (
                        <article
                          key={
                            mediaUrl ||
                            `${comment.username || 'anonymous'}-${
                              comment.createdDate || index
                            }-${index}`
                          }
                          style={{
                            padding: '16px',
                            borderTop:
                              index > 0 ? `1px solid ${colors.border}` : 'none',
                          }}
                        >
                          <div
                            style={{
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'flex-start',
                              position: 'relative',
                              gap: '12px',
                            }}
                          >
                            <div style={{ flex: 1 }}>
                              <p
                                style={{
                                  margin: '0 0 4px',
                                  fontWeight: 600,
                                  color: colors.text,
                                }}
                              >
                                {comment.username || 'Anonymous'}
                              </p>
                            </div>
                            <p
                              style={{
                                margin: '0 0 8px',
                                fontSize: '0.8rem',
                                color: colors.subtle,
                                whiteSpace: 'nowrap',
                              }}
                            >
                              {formatCommentDate(comment.createdDate)}
                            </p>
                            {showDiscussionMenu && (
                              <DropdownMenu>
                                <DropdownMenuTrigger
                                  render={
                                    <Button
                                      type="button"
                                      variant="ghost"
                                      size="icon-sm"
                                      className="shrink-0"
                                      style={{ color: colors.muted }}
                                      aria-label="Comment actions"
                                    />
                                  }
                                >
                                  <MoreVertical className="size-4" />
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end">
                                  {canReportComment && (
                                    <DropdownMenuItem
                                      onClick={() => openReportDialog(comment)}
                                    >
                                      Report
                                    </DropdownMenuItem>
                                  )}
                                  {canReportComment && canDeleteComment && (
                                    <DropdownMenuSeparator />
                                  )}
                                  {canDeleteComment && (
                                    <DropdownMenuItem
                                      variant="destructive"
                                      onClick={() =>
                                        isSolutionBeta
                                          ? handleDeleteSolutionBeta(comment)
                                          : handleDeleteComment(comment)
                                      }
                                    >
                                      {isSolutionBeta
                                        ? 'Delete Solution Beta'
                                        : 'Delete Comment'}
                                    </DropdownMenuItem>
                                  )}
                                </DropdownMenuContent>
                              </DropdownMenu>
                            )}
                          </div>
                          {isSolutionBeta ? (
                            <details>
                              <summary
                                style={{
                                  color: colors.primary,
                                  fontSize: '0.9rem',
                                  textDecoration: 'underline',
                                  cursor: 'pointer',
                                }}
                              >
                                Watch Beta
                              </summary>
                              <div style={{ marginTop: '10px' }}>
                                <video
                                  controls
                                  preload="metadata"
                                  style={{
                                    width: '100%',
                                    maxWidth: '360px',
                                    borderRadius: '8px',
                                    border: `1px solid ${colors.border}`,
                                    background: '#000',
                                  }}
                                >
                                  <source
                                    src={mediaUrl}
                                    type={inferVideoMimeType(mediaUrl)}
                                  />
                                  Your browser does not support the video tag.
                                </video>
                              </div>
                            </details>
                          ) : (
                            <p
                              style={{
                                margin: 0,
                                color: colors.text,
                                lineHeight: 1.5,
                              }}
                            >
                              {discussionContent}
                            </p>
                          )}
                        </article>
                      );
                    })(),
                  )}
                </div>
              ) : (
                <p style={{ color: colors.subtle, marginBottom: '24px' }}>
                  No comments yet. Be the first to discuss this problem!
                </p>
              )}

              {/* Add Comment Form */}
              <div
                style={{
                  display: 'flex',
                  gap: '16px',
                  alignItems: 'flex-start',
                }}
              >
                <div
                  style={{
                    ...card.surface,
                    padding: '20px',
                    borderRadius: '8px',
                    marginBottom: '16px',
                    flex: 1,
                  }}
                >
                  <p
                    style={{
                      margin: '0 0 12px',
                      fontWeight: 600,
                      color: colors.text,
                    }}
                  >
                    Add a Comment or Solution Beta
                  </p>
                  <div
                    style={{
                      display: 'flex',
                      gap: '8px',
                      marginBottom: '12px',
                    }}
                  >
                    <button
                      type="button"
                      onClick={() => setEntryMode('comment')}
                      style={{
                        ...buttons.secondary,
                        ...(entryMode === 'comment'
                          ? {
                              border: `1px solid ${colors.primary}`,
                              color: colors.primary,
                              background: colors.accentSoft,
                            }
                          : {}),
                      }}
                    >
                      Add Comment
                    </button>
                    <button
                      type="button"
                      onClick={() => setEntryMode('file')}
                      style={{
                        ...buttons.secondary,
                        ...(entryMode === 'file'
                          ? {
                              border: `1px solid ${colors.primary}`,
                              color: colors.primary,
                              background: colors.accentSoft,
                            }
                          : {}),
                      }}
                    >
                      Submit Beta
                    </button>
                  </div>

                  {entryMode === 'comment' ? (
                    <>
                      <textarea
                        value={commentText}
                        onChange={(e) => setCommentText(e.target.value)}
                        placeholder="Write a comment here!"
                        style={{
                          width: '100%',
                          minHeight: '100px',
                          padding: '12px',
                          border: `1px solid ${
                            commentOverLimit ? colors.danger : colors.muted
                          }`,
                          borderRadius: '6px',
                          fontFamily,
                          fontSize: '0.875rem',
                          resize: 'vertical',
                          marginBottom: '8px',
                        }}
                      />
                      <p
                        style={{
                          margin: '0 0 12px',
                          fontSize: '0.8rem',
                          color: commentOverLimit
                            ? colors.danger
                            : colors.subtle,
                          textAlign: 'right',
                        }}
                      >
                        {commentCharCount}/250
                      </p>
                    </>
                  ) : (
                    <div style={{ marginBottom: '12px' }}>
                      <input
                        ref={fileInputRef}
                        id="solution-beta-file-input"
                        type="file"
                        accept="video/mp4,video/webm"
                        onChange={(e) => {
                          setSolutionFile(e.target.files?.[0] || null);
                          setUploadStatus(null);
                        }}
                        disabled={!isSignedIn}
                        style={{ display: 'none' }}
                      />
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '10px',
                          flexWrap: 'wrap',
                        }}
                      >
                        <label
                          htmlFor="solution-beta-file-input"
                          style={{
                            ...buttons.secondary,
                            padding: '6px 10px',
                            fontSize: '0.78rem',
                            lineHeight: 1.1,
                            borderRadius: '999px',
                            borderColor: colors.border,
                            background: colors.surfaceAlt,
                            color: colors.text,
                            cursor: 'pointer',
                            margin: 0,
                          }}
                        >
                          Choose Video
                        </label>
                        <span
                          style={{
                            fontSize: '0.8rem',
                            color: solutionFile ? colors.text : colors.subtle,
                            maxWidth: '100%',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                          }}
                        >
                          {solutionFile
                            ? solutionFile.name
                            : 'No file selected'}
                        </span>
                      </div>
                      <p
                        style={{
                          margin: '8px 0 0',
                          fontSize: '0.8rem',
                          color: colors.subtle,
                        }}
                      >
                        Allowed file types: .mp4, .webm
                      </p>
                      {uploadStatus && (
                        <div
                          style={{
                            marginTop: '10px',
                            fontSize: '0.85rem',
                            color:
                              uploadStatus.type === 'success'
                                ? colors.primary
                                : colors.danger,
                          }}
                        >
                          {uploadStatus.message}
                          {uploadStatus.publicURL && (
                            <>
                              {' '}
                              <a
                                href={uploadStatus.publicURL}
                                target="_blank"
                                rel="noreferrer"
                              >
                                View file
                              </a>
                            </>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                  <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    {entryMode === 'comment' ? (
                      <button
                        type="button"
                        onClick={handlePostComment}
                        disabled={!canPostComment}
                        style={{
                          ...buttons.primary,
                          opacity: !canPostComment ? 0.6 : 1,
                          cursor: !canPostComment ? 'not-allowed' : 'pointer',
                        }}
                      >
                        {submittingComment ? 'Posting...' : 'Post Comment'}
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={handleUploadSolutionBeta}
                        disabled={
                          !isSignedIn || !solutionFile || uploadingSolution
                        }
                        style={{
                          ...buttons.primary,
                          opacity: !solutionFile || uploadingSolution ? 0.6 : 1,
                          cursor:
                            !solutionFile || uploadingSolution
                              ? 'not-allowed'
                              : 'pointer',
                        }}
                      >
                        {uploadingSolution
                          ? 'Uploading...'
                          : 'Upload Solution Beta'}
                      </button>
                    )}
                  </div>
                </div>
                {/* Aggregate Perceived Grade Card */}
                <div
                  style={{
                    ...card.surface,
                    padding: '16px',
                    borderRadius: '8px',
                    textAlign: 'center',
                    minWidth: '160px',
                    marginBottom: '16px',
                  }}
                >
                  <p
                    style={{
                      margin: '0 0 12px',
                      fontWeight: 600,
                      color: colors.text,
                    }}
                  >
                    Suggest Difficulty
                  </p>
                  <p
                    style={{
                      margin: '0 0 10px',
                      fontSize: '1.625rem',
                      fontWeight: 700,
                      color: colors.text,
                    }}
                  >
                    {perceivedGrade || 'N/A'}
                  </p>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '6px',
                      marginBottom: '10px',
                    }}
                  >
                    <select
                      value={perceivedGrade}
                      onChange={(e) => setPerceivedGrade(e.target.value)}
                      disabled={!isSignedIn}
                      style={{
                        fontSize: '0.75rem',
                        padding: '4px 6px',
                        border: `1px solid ${colors.borderHairline}`,
                        borderRadius: '6px',
                        fontFamily,
                        background: colors.surfaceAlt,
                        color: colors.text,
                        cursor: 'pointer',
                      }}
                    >
                      {[
                        'VB',
                        'V0',
                        'V1',
                        'V2',
                        'V3',
                        'V4',
                        'V5',
                        'V6',
                        'V7',
                        'V8',
                        'V9',
                        'V10',
                        'V11',
                        'V12',
                        'V13',
                        'V14',
                        'V15',
                        'V16',
                        'V17',
                      ].map((g) => (
                        <option key={g} value={g}>
                          {g}
                        </option>
                      ))}
                    </select>
                  </div>
                  <button
                    type="button"
                    onClick={handleSuggestPerceivedGrade}
                    disabled={!isSignedIn}
                    style={{
                      ...buttons.primary,
                      width: '100%',
                      fontSize: '0.75rem',
                    }}
                  >
                    Suggest Grade
                  </button>
                </div>
              </div>
            </section>
          </>
        )}
      </div>

      <Dialog
        open={!!reportTarget}
        onOpenChange={(open) => {
          if (!open) closeReportDialog();
        }}
      >
        <DialogContent className="sm:max-w-md">
          <form onSubmit={handleSubmitReport}>
            <DialogHeader>
              <DialogTitle>Report discussion</DialogTitle>
              <DialogDescription>
                Choose a category and explain why this discussion should be
                reviewed.
              </DialogDescription>
            </DialogHeader>
            <div className="grid gap-3 py-3">
              <div className="grid gap-1.5">
                <label
                  htmlFor="report-category"
                  className="text-sm font-medium text-foreground"
                >
                  Category
                </label>
                <select
                  id="report-category"
                  value={reportCategory}
                  onChange={(event) => setReportCategory(event.target.value)}
                  disabled={submittingReport}
                  required
                  aria-label="Report category"
                  className="w-full rounded-md border border-input bg-background px-2 py-1.5 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40"
                >
                  <option value="">Select a category</option>
                  {REPORT_CATEGORIES.map((category) => (
                    <option key={category.value} value={category.value}>
                      {category.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid gap-1.5">
                <label
                  htmlFor="report-reason"
                  className="text-sm font-medium text-foreground"
                >
                  Reason
                </label>
                <textarea
                  id="report-reason"
                  value={reportReason}
                  onChange={(event) => setReportReason(event.target.value)}
                  disabled={submittingReport}
                  required
                  rows={4}
                  placeholder="Describe what is wrong with this discussion."
                  aria-label="Report reason"
                  className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm text-foreground outline-none focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40"
                  style={{
                    borderColor: reportReasonOverLimit
                      ? colors.danger
                      : undefined,
                  }}
                />
                <p
                  className="text-xs text-right"
                  style={{
                    color: reportReasonOverLimit ? colors.danger : undefined,
                  }}
                >
                  {reportReason.length}/{REPORT_REASON_MAX_LENGTH}
                </p>
              </div>
            </div>
            <DialogFooter className="mt-1 gap-2 sm:justify-end">
              <Button
                type="button"
                variant="outline"
                disabled={submittingReport}
                onClick={closeReportDialog}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={!canSubmitReport}
                style={buttons.primary}
              >
                {submittingReport ? 'Submitting…' : 'Submit Report'}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </main>
  );
}
