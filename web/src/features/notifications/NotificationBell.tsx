import { Link } from 'react-router'
import { useUnreadCount } from './notificationApi'

export function NotificationBell() {
  const unread = useUnreadCount()
  const count = unread.data?.unreadCount ?? 0
  const label = count === 0 ? 'Notifications' : `Notifications, ${count} unread`
  return (
    <Link to="/notifications" aria-label={label} title={label} className="relative inline-flex items-center text-stone-700 hover:text-stone-900">
      <svg aria-hidden="true" viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5m6 0a3 3 0 1 1-6 0" />
      </svg>
      {count > 0 && (
        <span className="absolute -right-2 -top-2 min-w-[1.1rem] rounded-full bg-udaan-600 px-1 text-center text-[0.65rem] font-bold leading-[1.1rem] text-white">
          {count > 99 ? '99+' : count}
        </span>
      )}
    </Link>
  )
}
