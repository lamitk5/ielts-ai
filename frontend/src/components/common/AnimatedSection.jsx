import { motion } from 'framer-motion'
import { useEffectiveReducedMotion } from '../../features/preferences/PreferenceProvider'

function AnimatedSection({ children, className = '', delay = 0, ...props }) {
  const prefersReducedMotion = useEffectiveReducedMotion()

  if (prefersReducedMotion) {
    return <section className={className} {...props}>{children}</section>
  }

  return (
    <motion.section
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay }}
      viewport={{ once: true, amount: 0.2 }}
      className={className}
      {...props}
    >
      {children}
    </motion.section>
  )
}

export default AnimatedSection
