export const TutorReferenceResolver = {
  resolve(reference, registry, workspaceState = {}) {
    if (!reference || !registry) {
      return { status: 'INVALID', reference, target: null }
    }

    const { referenceType, targetId, draftVersion } = reference

    // Version-bound mutable check for Writing draft references
    if (referenceType === 'DRAFT') {
      if (draftVersion != null && workspaceState.draftVersion != null) {
        if (Number(draftVersion) !== Number(workspaceState.draftVersion)) {
          return {
            status: 'STALE',
            reference,
            target: null,
            reason: 'VERSION_MISMATCH',
          }
        }
      }
    }

    const target = registry.getTarget(targetId)
    if (!target) {
      return {
        status: 'UNKNOWN_TARGET',
        reference,
        target: null,
      }
    }

    return {
      status: 'ACTIVE',
      reference,
      target,
    }
  },
}
