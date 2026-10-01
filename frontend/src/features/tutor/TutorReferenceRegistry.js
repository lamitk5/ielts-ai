const SAFE_ID_PATTERN = /^[a-zA-Z0-9_-]+$/

export class TutorReferenceRegistry {
  constructor() {
    this.targets = new Map()
    this.listeners = new Set()
  }

  register(target) {
    if (!target || typeof target !== 'object') {
      throw new Error('Target must be an object')
    }

    const { targetId } = target
    if (!targetId || typeof targetId !== 'string' || !SAFE_ID_PATTERN.test(targetId)) {
      throw new Error(`Invalid targetId: ${targetId}`)
    }

    this.targets.set(targetId, target)
    this.notify()
    return () => this.unregister(targetId)
  }

  unregister(targetId) {
    if (!targetId) return
    const deleted = this.targets.delete(targetId)
    if (deleted) {
      this.notify()
    }
  }

  getTarget(targetId) {
    if (!targetId) return undefined
    return this.targets.get(targetId)
  }

  getAllTargets() {
    return Array.from(this.targets.values())
  }

  clear() {
    this.targets.clear()
    this.notify()
  }

  subscribe(listener) {
    if (typeof listener !== 'function') return () => {}
    this.listeners.add(listener)
    return () => {
      this.listeners.delete(listener)
    }
  }

  notify() {
    for (const listener of this.listeners) {
      try {
        listener(this.getAllTargets())
      } catch {
        // ignore listener errors
      }
    }
  }
}

export const defaultTutorReferenceRegistry = new TutorReferenceRegistry()
