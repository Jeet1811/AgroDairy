import { type ReactNode, useEffect } from 'react'
import { X } from 'lucide-react'
import { createPortal } from 'react-dom'

interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  widthClass?: string
}

export function Modal({ open, onClose, title, children, widthClass = 'max-w-lg' }: ModalProps) {
  useEffect(() => {
    if (!open) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-brand-900/50 p-0 backdrop-blur-sm sm:items-center sm:p-4">
      <div
        className={`max-h-[92vh] w-full ${widthClass} overflow-y-auto rounded-t-2xl bg-white shadow-soft-lg sm:rounded-2xl`}
        role="dialog"
        aria-modal="true"
      >
        <div className="sticky top-0 flex items-center justify-between border-b border-brand-100 bg-white px-5 py-4">
          <h2 className="text-lg font-semibold text-brand-900">{title}</h2>
          <button onClick={onClose} className="rounded-full p-1.5 text-brand-500 hover:bg-brand-50" aria-label="Close">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-5">{children}</div>
      </div>
    </div>,
    document.body,
  )
}
