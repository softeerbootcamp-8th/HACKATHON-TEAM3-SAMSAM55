import { useRef, type TouchEvent } from 'react'

const SWIPE_THRESHOLD = 40

function useHorizontalSwipe(onSwipeLeft: () => void, onSwipeRight: () => void) {
  const touchStartRef = useRef<{ x: number; y: number } | null>(null)

  const onTouchStart = (event: TouchEvent) => {
    const touch = event.touches[0]
    if (!touch) {
      return
    }

    touchStartRef.current = { x: touch.clientX, y: touch.clientY }
  }

  const onTouchEnd = (event: TouchEvent) => {
    const start = touchStartRef.current
    const touch = event.changedTouches[0]
    touchStartRef.current = null

    if (!start || !touch) {
      return
    }

    const deltaX = touch.clientX - start.x
    const deltaY = touch.clientY - start.y
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

  const onTouchCancel = () => {
    touchStartRef.current = null
  }

  return { onTouchStart, onTouchEnd, onTouchCancel }
}

export { useHorizontalSwipe }
