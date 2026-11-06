import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, TextareaHTMLAttributes } from 'react'
import { useId } from 'react'

interface FieldProps {
  label: string
  error?: string
  hint?: ReactNode
}

const inputClass =
  'w-full rounded-md border border-stone-300 bg-white px-3 py-2 text-base outline-none focus:border-udaan-600 focus:ring-2 focus:ring-udaan-600/20 aria-[invalid=true]:border-red-600'

export function TextField({ label, error, hint, ...input }: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId()
  return (
    <div className="space-y-1">
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      <input id={id} aria-invalid={error ? true : undefined} aria-describedby={error ? `${id}-error` : undefined} className={inputClass} {...input} />
      {hint && !error && <p className="text-xs text-stone-500">{hint}</p>}
      {error && (
        <p id={`${id}-error`} className="text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  )
}

export function TextArea({ label, error, hint, ...input }: FieldProps & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId()
  return (
    <div className="space-y-1">
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      <textarea id={id} aria-invalid={error ? true : undefined} className={inputClass} rows={3} {...input} />
      {hint && !error && <p className="text-xs text-stone-500">{hint}</p>}
      {error && <p className="text-sm text-red-700">{error}</p>}
    </div>
  )
}

export function Button({
  busy,
  variant = 'primary',
  children,
  ...button
}: ButtonHTMLAttributes<HTMLButtonElement> & { busy?: boolean; variant?: 'primary' | 'secondary' }) {
  const styles =
    variant === 'primary'
      ? 'bg-udaan-600 text-white hover:bg-udaan-700 disabled:bg-stone-400'
      : 'border border-stone-300 bg-white text-stone-800 hover:bg-stone-100 disabled:text-stone-400'
  return (
    <button
      {...button}
      disabled={busy || button.disabled}
      className={`rounded-md px-4 py-2 text-sm font-semibold ${styles} ${button.className ?? ''}`}
    >
      {busy ? 'Please wait…' : children}
    </button>
  )
}

export function FormError({ message }: { message?: string }) {
  if (!message) return null
  return (
    <p role="alert" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
      {message}
    </p>
  )
}
