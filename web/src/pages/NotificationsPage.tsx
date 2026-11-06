import { Link } from 'react-router'
import { Button, FormError } from '../components/shared/Form'
import { useInbox, useMarkRead } from '../features/notifications/notificationApi'
import { errorMessage } from '../lib/api'

const when = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata' })

export function NotificationsPage() {
  const inbox = useInbox()
  const markRead = useMarkRead()

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-bold">Notifications</h1>
        {inbox.data && inbox.data.unreadCount > 0 && (
          <Button variant="secondary" busy={markRead.isPending} onClick={() => markRead.mutate(undefined)}>
            Mark all as read
          </Button>
        )}
      </div>
      {inbox.isPending && <p className="text-stone-500">Loading…</p>}
      {inbox.isError && <FormError message={errorMessage(inbox.error)} />}
      {inbox.data?.items.length === 0 && (
        <p className="text-stone-600">Nothing yet. Follow or support problems to hear about their progress.</p>
      )}
      <ul className="space-y-2">
        {inbox.data?.items.map((n) => (
          <li key={n.id}>
            <Link
              to={n.link}
              onClick={() => !n.read && markRead.mutate([n.id])}
              className={`block rounded-lg border p-3 hover:border-udaan-600 ${n.read ? 'border-stone-200 bg-white' : 'border-udaan-600/40 bg-orange-50'}`}
            >
              <p className={`text-sm ${n.read ? '' : 'font-semibold'}`}>
                {!n.read && <span className="sr-only">Unread: </span>}
                {n.title}
              </p>
              {n.body && <p className="mt-1 line-clamp-2 text-sm text-stone-600">{n.body}</p>}
              <p className="mt-1 text-xs text-stone-500">{when.format(new Date(n.createdAt))}</p>
            </Link>
          </li>
        ))}
      </ul>
      <p className="text-xs text-stone-500">Email and WhatsApp notifications will come later.</p>
    </div>
  )
}
