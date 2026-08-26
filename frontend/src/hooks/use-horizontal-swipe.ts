import { useRef, type PointerEvent } from 'react'

const SWIPE_THRESHOLD = 40

function useHorizontalSwipe(onSwipeLeft: () => void, onSwipeRight: () => void) {
  const swipeStartRef = useRef<{ x: number; y: number } | null>(null)

  const onPointerDown = (event: PointerEvent<HTMLElement>) => {
    swipeStartRef.current = { x: event.clientX, y: event.clientY }
    event.currentTarget.setPointerCapture(event.pointerId)
  }

  const onPointerUp = (event: PointerEvent<HTMLElement>) => {
    const start = swipeStartRef.current
    swipeStartRef.current = null
    event.currentTarget.releasePointerCapture(event.pointerId)

    if (!start) {
      return
    }

    const deltaX = event.clientX - start.x
    const deltaY = event.clientY - start.y
    if (
      Math.abs(deltaX) < SWIPE_THRESHOLD ||
      Math.abs(deltaX) <= Math.abs(deltaY)
    ) {
      return
    }

    if (deltaX < 0) {
      onSwipeLeft()
      return
    }

    onSwipeRight()
  }

  const onPointerCancel = () => {
    swipeStartRef.current = null
  }

  return { onPointerDown, onPointerUp, onPointerCancel }
}

export { useHorizontalSwipe }
