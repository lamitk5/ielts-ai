const variants = {
  primary: 'button-primary',
  secondary: 'button-secondary',
  ghost: 'button-ghost',
}

const sizes = {
  sm: 'button-sm',
  md: 'button-md',
  lg: 'button-lg',
}

function Button({
  variant = 'primary',
  size = 'md',
  className = '',
  type = 'button',
  children,
  ...props
}) {
  const selectedVariant = variants[variant] ?? variants.primary
  const selectedSize = sizes[size] ?? sizes.md

  return (
    <button
      type={type}
      className={`button btn-liquid ${selectedVariant} ${selectedSize} ${className}`.trim()}
      {...props}
    >
      <span className="button-label">{children}</span>
    </button>
  )
}

export default Button
