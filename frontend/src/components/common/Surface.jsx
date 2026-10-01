import { createElement } from 'react'

const variants = {
  base: 'surface-base',
  elevated: 'surface-elevated',
  interactive: 'surface-interactive',
}

function Surface({ as = 'div', variant = 'base', children, className = '', ...props }) {
  const surfaceVariant = variants[variant] ?? variants.base
  return createElement(as, { className: `surface ${surfaceVariant} ${className}`.trim(), ...props }, children)
}

export default Surface
