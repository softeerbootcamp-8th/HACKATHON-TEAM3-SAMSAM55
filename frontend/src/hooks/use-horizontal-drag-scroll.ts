import { useRef, type MouseEvent, type PointerEvent } from 'react'

type DragState = {
  startX: number
  scrollLeft: number
  moved: boolean
}

function useHorizontalDragScroll() {
  const dragRef = useRef<DragState | null>(null)
  const suppressClickRef = useRef(false)

  const onPointerDown = (event: PointerEvent<HTMLDivElement>) => {
    if (event.pointerType === 'touch') {
      return
    }

    dragRef.current = {
      startX: event.clientX,
      scrollLeft: event.currentTarget.scrollLeft,
      moved: false,
    }
  }

  const onPointerMove = (event: PointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current
    if (!drag) {
      return
    }

    const deltaX = event.clientX - drag.startX
    if (Math.abs(deltaX) < 4) {
      return
    }

    drag.moved = true
    event.currentTarget.setPointerCapture(event.pointerId)
    event.preventDefault()
    event.currentTarget.scrollLeft = drag.scrollLeft - deltaX
  }

  const finishDrag = (event: PointerEvent<HTMLDivElement>) => {
    const drag = dragRef.current
    dragRef.current = null

    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }

    if (!drag?.moved) {
      return
    }

    suppressClickRef.current = true
    window.setTimeout(() => {
      suppressClickRef.current = false
    }, 0)
  }

  const onClickCapture = (event: MouseEvent<HTMLDivElement>) => {
    if (!suppressClickRef.current) {
      return
    }

    event.preventDefault()
    event.stopPropagation()
    suppressClickRef.current = false
  }

  const onPointerCancel = (event: PointerEvent<HTMLDivElement>) => {
    dragRef.current = null
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId)
    }
  }

  return {
    onPointerDown,
    onPointerMove,
    onPointerUp: finishDrag,
    onPointerCancel,
    onClickCapture,
  }
}

export { useHorizontalDragScroll }
