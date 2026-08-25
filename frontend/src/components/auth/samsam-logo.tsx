type SamsamLogoProps = {
  variant?: 'onBrand' | 'onLight'
  size?: number
  className?: string
}

// Figma CMP_로고_삼삼오오(node 346:836 / 350:756)에서 받은 정확한 원 4개 좌표를 그대로 옮김.
function SamsamLogo({
  variant = 'onLight',
  size = 56,
  className,
}: SamsamLogoProps) {
  const lobeFill = variant === 'onBrand' ? 'white' : '#92D3D5'

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 88 88"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
    >
      <circle cx="24" cy="24" r="24" fill={lobeFill} />
      <circle cx="64" cy="24" r="24" fill={lobeFill} />
      <circle cx="24" cy="64" r="24" fill={lobeFill} />
      <circle cx="64" cy="64" r="24" fill="#3E9A9D" />
    </svg>
  )
}

export { SamsamLogo }
