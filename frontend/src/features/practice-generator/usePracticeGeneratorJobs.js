import { useState, useEffect, useCallback } from 'react'
import { practiceGeneratorApi } from './practiceGeneratorApi'

export function usePracticeGeneratorJobs({ pollIntervalMs = 3000 } = {}) {
  const [jobs, setJobs] = useState([])
  const [sources, setSources] = useState([])
  const [blueprints, setBlueprints] = useState([])
  const [sets, setSets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const refresh = useCallback(async () => {
    try {
      setError(null)
      const [jobsData, sourcesData, blueprintsData, setsData] = await Promise.all([
        practiceGeneratorApi.listJobs().catch(() => []),
        practiceGeneratorApi.listSources().catch(() => []),
        practiceGeneratorApi.listBlueprints().catch(() => []),
        practiceGeneratorApi.listSets().catch(() => []),
      ])
      setJobs(jobsData || [])
      setSources(sourcesData || [])
      setBlueprints(blueprintsData || [])
      setSets(setsData || [])
    } catch (err) {
      setError(err.message || 'Không thể làm mới danh sách tác vụ.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
    if (!pollIntervalMs) return
    const interval = setInterval(refresh, pollIntervalMs)
    return () => clearInterval(interval)
  }, [refresh, pollIntervalMs])

  const createJob = async (jobPayload) => {
    setLoading(true)
    try {
      const result = await practiceGeneratorApi.createJob(jobPayload)
      await refresh()
      return result
    } catch (err) {
      setError(err.message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  const registerSource = async (sourcePayload) => {
    try {
      const result = await practiceGeneratorApi.registerSource(sourcePayload)
      await refresh()
      return result
    } catch (err) {
      setError(err.message)
      throw err
    }
  }

  const extractBlueprint = async (extractPayload) => {
    try {
      const result = await practiceGeneratorApi.extractBlueprint(extractPayload)
      await refresh()
      return result
    } catch (err) {
      setError(err.message)
      throw err
    }
  }

  return {
    jobs,
    sources,
    blueprints,
    sets,
    loading,
    error,
    refresh,
    createJob,
    registerSource,
    extractBlueprint,
  }
}
