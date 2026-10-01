export const REPULSION_RADIUS = 156
export const MAX_REPEL_OFFSET = 28
export const MAX_REPEL_VELOCITY = 2.4

export function clampVelocity(vx, vy, maxVelocity = MAX_REPEL_VELOCITY) {
  const speed = Math.hypot(vx, vy)
  if (!Number.isFinite(speed) || speed === 0 || speed <= maxVelocity) return [vx, vy]
  const scale = maxVelocity / speed
  return [vx * scale, vy * scale]
}

export function calculateRepulsion(distanceX, distanceY, radius = REPULSION_RADIUS) {
  const distance = Math.hypot(distanceX, distanceY)
  if (!Number.isFinite(distance) || distance === 0 || distance >= radius) {
    return { x: 0, y: 0, strength: 0 }
  }

  const strength = 1 - distance / radius
  const magnitude = MAX_REPEL_OFFSET * strength
  return {
    x: (-distanceX / distance) * magnitude,
    y: (-distanceY / distance) * magnitude,
    strength,
  }
}

