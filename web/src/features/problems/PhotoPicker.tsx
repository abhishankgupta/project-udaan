import { useId, useState } from 'react'
import { apiUrl, errorMessage } from '../../lib/api'
import { compressImage } from './compressImage'
import { uploadPhoto, type Photo } from './problemApi'

export const MAX_PHOTOS = 4

interface Props {
  photos: Photo[]
  onChange: (photos: Photo[]) => void
  onBusyChange?: (busy: boolean) => void
}

/** Picks, compresses and uploads up to four photos, showing thumbnails as they finish. */
export function PhotoPicker({ photos, onChange, onBusyChange }: Props) {
  const inputId = useId()
  const [uploading, setUploading] = useState(0)
  const [error, setError] = useState<string>()

  async function addFiles(files: FileList | null) {
    if (!files || files.length === 0) return
    setError(undefined)
    const room = MAX_PHOTOS - photos.length
    const chosen = Array.from(files).slice(0, room)
    if (files.length > room) setError(`You can add up to ${MAX_PHOTOS} photos.`)

    setUploading(chosen.length)
    onBusyChange?.(true)
    let current = photos
    for (const file of chosen) {
      try {
        const blob = await compressImage(file)
        const photo = await uploadPhoto(blob, file.name.replace(/\.[^.]+$/, '') + '.jpg')
        current = [...current, photo]
        onChange(current)
      } catch (e) {
        setError(errorMessage(e) ?? 'A photo could not be uploaded.')
      } finally {
        setUploading((n) => n - 1)
      }
    }
    onBusyChange?.(false)
  }

  return (
    <div className="space-y-2">
      <ul className="flex flex-wrap gap-3" aria-label="Photos">
        {photos.map((p, i) => (
          <li key={p.id} className="relative">
            <img src={apiUrl(p.url)} alt={`Photo ${i + 1}`} className="h-24 w-24 rounded-md border border-stone-200 object-cover" />
            <button
              type="button"
              onClick={() => onChange(photos.filter((x) => x.id !== p.id))}
              className="absolute -right-2 -top-2 rounded-full bg-stone-800 px-2 text-xs text-white"
              aria-label={`Remove photo ${i + 1}`}
            >
              ×
            </button>
          </li>
        ))}
        {uploading > 0 && (
          <li className="flex h-24 w-24 items-center justify-center rounded-md border border-dashed border-stone-300 text-xs text-stone-500">
            Uploading…
          </li>
        )}
      </ul>
      {photos.length < MAX_PHOTOS && (
        <div>
          <label
            htmlFor={inputId}
            className="inline-block cursor-pointer rounded-md border border-stone-300 bg-white px-3 py-2 text-sm font-medium hover:bg-stone-100"
          >
            {photos.length === 0 ? 'Add photos' : 'Add more photos'}
          </label>
          <input
            id={inputId}
            type="file"
            accept="image/jpeg,image/png"
            multiple
            className="sr-only"
            onChange={(e) => {
              void addFiles(e.target.files)
              e.target.value = ''
            }}
          />
        </div>
      )}
      {error && <p className="text-sm text-red-700">{error}</p>}
      <p className="text-xs text-stone-500">Up to {MAX_PHOTOS} photos. Location data inside photos is removed when uploaded.</p>
    </div>
  )
}
