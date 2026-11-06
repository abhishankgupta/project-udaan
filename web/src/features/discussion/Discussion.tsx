import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { Button, FormError } from '../../components/shared/Form'
import { errorMessage } from '../../lib/api'
import { useSession } from '../../lib/session'
import { useComments, useDeleteComment, usePostComment, type CommentView, type DiscussionTarget } from './commentApi'

const when = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata' })

function CommentForm({
  target,
  parentId,
  label,
  onDone,
}: {
  target: DiscussionTarget
  parentId?: string
  label: string
  onDone?: () => void
}) {
  const [body, setBody] = useState('')
  const post = usePostComment(target)

  function submit(e: FormEvent) {
    e.preventDefault()
    if (!body.trim()) return
    post.mutate(
      { body, parentId },
      {
        onSuccess: () => {
          setBody('')
          onDone?.()
        },
      },
    )
  }

  return (
    <form onSubmit={submit} className="space-y-2">
      <label className="sr-only" htmlFor={`comment-${parentId ?? 'new'}`}>
        {label}
      </label>
      <textarea
        id={`comment-${parentId ?? 'new'}`}
        rows={parentId ? 2 : 3}
        maxLength={2000}
        value={body}
        onChange={(e) => setBody(e.target.value)}
        placeholder={parentId ? 'Write a reply…' : 'Share information, ask a question, or suggest an idea…'}
        className="w-full rounded-md border border-stone-300 bg-white px-3 py-2"
      />
      <FormError message={post.isError ? errorMessage(post.error) : undefined} />
      <Button type="submit" busy={post.isPending} disabled={!body.trim()}>
        {label}
      </Button>
    </form>
  )
}

function CommentItem({ comment, target, isReply }: { comment: CommentView; target: DiscussionTarget; isReply?: boolean }) {
  const signedIn = useSession((s) => s.user !== null)
  const [replying, setReplying] = useState(false)
  const remove = useDeleteComment(target)

  return (
    <li className="space-y-2">
      <div className={`rounded-md p-3 ${isReply ? 'bg-stone-50' : 'border border-stone-200 bg-white'}`}>
        {comment.removed ? (
          <p className="text-sm italic text-stone-500">This comment was removed.</p>
        ) : (
          <>
            <p className="text-xs text-stone-500">
              <span className="font-medium text-stone-800">{comment.authorName}</span> · {when.format(new Date(comment.createdAt))}
            </p>
            <p className="mt-1 whitespace-pre-line text-sm">{comment.body}</p>
            <div className="mt-2 flex gap-3 text-xs">
              {signedIn && !isReply && (
                <button type="button" className="text-udaan-700 underline" onClick={() => setReplying((r) => !r)}>
                  Reply
                </button>
              )}
              {comment.viewerIsAuthor && (
                <button
                  type="button"
                  className="text-stone-500 underline"
                  disabled={remove.isPending}
                  onClick={() => {
                    if (window.confirm('Delete this comment?')) remove.mutate(comment.id)
                  }}
                >
                  Delete
                </button>
              )}
            </div>
          </>
        )}
      </div>
      {comment.replies.length > 0 && (
        <ul className="ml-6 space-y-2" aria-label="Replies">
          {comment.replies.map((r) => (
            <CommentItem key={r.id} comment={r} target={target} isReply />
          ))}
        </ul>
      )}
      {replying && (
        <div className="ml-6">
          <CommentForm target={target} parentId={comment.id} label="Post reply" onDone={() => setReplying(false)} />
        </div>
      )}
    </li>
  )
}

export function Discussion({ target }: { target: DiscussionTarget }) {
  const signedIn = useSession((s) => s.user !== null)
  const comments = useComments(target)

  return (
    <section className="space-y-3" aria-labelledby="discussion-heading">
      <h2 id="discussion-heading" className="text-lg font-semibold">
        Discussion
      </h2>
      {comments.isPending && <p className="text-sm text-stone-500">Loading…</p>}
      {comments.isError && <FormError message={errorMessage(comments.error)} />}
      {comments.data?.length === 0 && <p className="text-sm text-stone-600">No comments yet. Start the conversation.</p>}
      <ul className="space-y-3">
        {comments.data?.map((c) => (
          <CommentItem key={c.id} comment={c} target={target} />
        ))}
      </ul>
      {signedIn ? (
        <CommentForm target={target} label="Post comment" />
      ) : (
        <p className="text-sm">
          <Link to={`/login?next=${encodeURIComponent(window.location.pathname)}`} className="text-udaan-700 underline">
            Sign in
          </Link>{' '}
          to join the discussion.
        </p>
      )}
      <p className="text-xs text-stone-500">Keep it constructive. Don't share phone numbers or personal details here.</p>
    </section>
  )
}
