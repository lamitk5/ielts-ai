import { useState, useEffect, useCallback } from 'react'
import { practiceGeneratorApi } from './practiceGeneratorApi'

export function usePracticeReviewSet(setId) {
  const [payload, setPayload] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [comparison, setComparison] = useState(null)

  const load = useCallback(async () => {
    if (!setId) return
    setLoading(true)
    setError(null)
    try {
      const data = await practiceGeneratorApi.getSetReviewPayload(setId)
      setPayload(data)
    } catch (err) {
      setError(err.message || 'Không thể tải thông tin kiểm duyệt bài tập.')
    } finally {
      setLoading(false)
    }
  }, [setId])

  useEffect(() => {
    load()
  }, [load])

  const submitReview = async ({ action, feedbackNotes, revisionInstructions, targetItemNumber }) => {
    setSubmitting(true)
    try {
      const res = await practiceGeneratorApi.submitReview(setId, {
        action,
        feedbackNotes,
        revisionInstructions,
        targetItemNumber,
      })
      await load()
      return res
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setSubmitting(false)
    }
  }

  const regenerateItem = async ({ questionId, revisionInstructions, preferredTaskType }) => {
    setSubmitting(true)
    try {
      const res = await practiceGeneratorApi.regenerateItem(setId, {
        questionId,
        revisionInstructions,
        preferredTaskType,
      })
      await load()
      return res
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setSubmitting(false)
    }
  }

  const applyManualEdit = async ({ passage, questions, editNotes }) => {
    setSubmitting(true)
    try {
      const res = await practiceGeneratorApi.applyManualEdit(setId, {
        passage,
        questions,
        editNotes,
      })
      await load()
      return res
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setSubmitting(false)
    }
  }

  const compareVersions = async (v1, v2) => {
    try {
      const diff = await practiceGeneratorApi.compareVersions(setId, v1, v2)
      setComparison(diff)
      return diff
    } catch (err) {
      setError(err.message)
      throw err
    }
  }

  return {
    payload,
    loading,
    error,
    submitting,
    comparison,
    refresh: load,
    submitReview,
    regenerateItem,
    applyManualEdit,
    compareVersions,
    clearComparison: () => setComparison(null),
  }
}
