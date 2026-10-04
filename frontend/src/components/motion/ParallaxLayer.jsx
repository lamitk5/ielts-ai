import { motion, useScroll, useTransform } from 'framer-motion'
import { useEffectiveReducedMotion } from '../../features/preferences/PreferenceProvider'

function ParallaxLayer({ children, distance = 18, className = '', ...props }) {
  const prefersReducedMotion = useEffectiveReducedMotion()
  const { scrollYProgress } = useScroll()
  const y = useTransform(scrollYProgress, [0, 1], [0, distance])
  const layerClassName = `parallax-layer ${className}`.trim()

  if (prefersReducedMotion) {
    return (
      <div className={layerClassName} {...props}>
        {children}
      </div>
    )
  }

  return (
    <motion.div className={layerClassName} style={{ y }} {...props}>
      {children}
    </motion.div>
  )
}

export default ParallaxLayer
