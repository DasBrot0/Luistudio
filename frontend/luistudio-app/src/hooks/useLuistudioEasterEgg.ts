import { useEffect, useRef, useState } from 'react'

const REQUIRED_CLICKS = 6
const RESET_DELAY_MS = 1500
const ANIMATION_DURATION_MS = 2200
const EFFECT_CLASS = 'luistudio-67-effect'

export function useLuistudioEasterEgg() {
  const [isActive, setIsActive] = useState(false)
  const clickCountRef = useRef(0)
  const resetTimerRef = useRef<number | undefined>(undefined)
  const animationTimerRef = useRef<number | undefined>(undefined)

  const handleLogoClick = () => {
    if (isActive) return

    clickCountRef.current += 1

    if (resetTimerRef.current !== undefined) {
      window.clearTimeout(resetTimerRef.current)
    }

    if (clickCountRef.current >= REQUIRED_CLICKS) {
      clickCountRef.current = 0
      setIsActive(true)
      animationTimerRef.current = window.setTimeout(() => setIsActive(false), ANIMATION_DURATION_MS)
      return
    }

    resetTimerRef.current = window.setTimeout(() => {
      clickCountRef.current = 0
    }, RESET_DELAY_MS)
  }

  useEffect(() => {
    document.body.classList.toggle(EFFECT_CLASS, isActive)

    return () => {
      document.body.classList.remove(EFFECT_CLASS)
    }
  }, [isActive])

  useEffect(() => () => {
    if (resetTimerRef.current !== undefined) window.clearTimeout(resetTimerRef.current)
    if (animationTimerRef.current !== undefined) window.clearTimeout(animationTimerRef.current)
  }, [])

  return { handleLogoClick }
}
