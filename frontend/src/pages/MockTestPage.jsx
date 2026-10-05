import React, { useEffect, useState, useCallback, useRef } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import MockTestShell from '../components/mock/MockTestShell'
import MockTestResult from '../components/mock/MockTestResult'
import GlassCard from '../components/common/GlassCard'
import Button from '../components/common/Button'
import {
  startOrResumeMockSession,
  getMockSession,
  executeMockCommand,
  autosaveMockSectionDraft,
  getMockTestResult,
} from '../services/mockTestApi'

export default function MockTestPage() {
  const { sessionId: routeSessionId } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  const [session, setSession] = useState(null)
  const [currentSectionIndex, setCurrentSectionIndex] = useState(0)
  const [answers, setAnswers] = useState({})
  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState(null)
  const [result, setResult] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [generalError, setGeneralError] = useState(null)

  const revisionRef = useRef(0)
  const answersRef = useRef({})
  answersRef.current = answers

  // Load or start session
  useEffect(() => {
    let isMounted = true

    async function initSession() {
      setIsLoading(true)
      setGeneralError(null)
      try {
        let sess
        if (routeSessionId) {
          sess = await getMockSession(routeSessionId)
        } else {
          sess = await startOrResumeMockSession({ mockTestId: searchParams.get('mockTestId') || undefined })
        }

        if (!isMounted) return
        setSession(sess)
        setCurrentSectionIndex(sess.currentSectionIndex || 0)

        if (sess.status === 'COMPLETED' || sess.status === 'EXPIRED') {
          const res = await getMockTestResult(sess.id)
          if (isMounted) setResult(res)
        }
      } catch (err) {
        if (!isMounted) return
        setGeneralError(err.message || 'Không thể tải phiên thi thử.')
      } finally {
        if (isMounted) setIsLoading(false)
      }
    }

    initSession()
    return () => {
      isMounted = false
    }
  }, [routeSessionId, searchParams])

  // Debounced Autosave
  const triggerAutosave = useCallback(async () => {
    if (!session || session.status !== 'IN_PROGRESS') return
    setIsSaving(true)
    setSaveError(null)
    try {
      const nextRev = revisionRef.current + 1
      await autosaveMockSectionDraft(session.id, currentSectionIndex, {
        answers: answersRef.current,
        expectedRevision: revisionRef.current,
        idempotencyKey: `draft-${session.id}-${currentSectionIndex}-${Date.now()}`,
      })
      revisionRef.current = nextRev
    } catch (err) {
      setSaveError(err.message || 'Lỗi lưu bản nháp')
    } finally {
      setIsSaving(false)
    }
  }, [session, currentSectionIndex])

  const handleAnswerChange = (questionKey, value) => {
    setAnswers((prev) => {
      const next = { ...prev, [questionKey]: value }
      return next
    })
  }

  // Trigger autosave when answers change
  useEffect(() => {
    const timer = setTimeout(() => {
      triggerAutosave()
    }, 1500)
    return () => clearTimeout(timer)
  }, [answers, triggerAutosave])

  const handlePause = async () => {
    if (!session) return
    try {
      const updated = await executeMockCommand(session.id, 'PAUSE')
      setSession(updated)
    } catch (err) {
      setGeneralError(err.message)
    }
  }

  const handleResume = async () => {
    if (!session) return
    try {
      const updated = await executeMockCommand(session.id, 'RESUME')
      setSession(updated)
    } catch (err) {
      setGeneralError(err.message)
    }
  }

  const handleNextSection = async () => {
    if (!session) return
    try {
      await triggerAutosave()
      await executeMockCommand(session.id, 'NEXT_SECTION')
      const nextIndex = currentSectionIndex + 1
      if (session.sections && nextIndex < session.sections.length) {
        setCurrentSectionIndex(nextIndex)
        setAnswers({})
      }
    } catch (err) {
      setGeneralError(err.message)
    }
  }

  const handleSubmitTest = async () => {
    if (!session) return
    try {
      await triggerAutosave()
      const updated = await executeMockCommand(session.id, 'SUBMIT')
      setSession(updated)
      const res = await getMockTestResult(session.id)
      setResult(res)
    } catch (err) {
      setGeneralError(err.message)
    }
  }

  const handleRetake = async () => {
    try {
      const newSess = await startOrResumeMockSession()
      setSession(newSess)
      setResult(null)
      setCurrentSectionIndex(0)
      setAnswers({})
      navigate(`/practice/mock-test/${newSess.id}`)
    } catch (err) {
      setGeneralError(err.message)
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-navy-950 p-4">
        <div className="text-center space-y-3">
          <div className="w-10 h-10 border-4 border-gold-500 border-t-transparent rounded-full animate-spin mx-auto" />
          <p className="font-serif text-slate-600 dark:text-slate-300">Đang chuẩn bị phòng thi thử...</p>
        </div>
      </div>
    )
  }

  if (generalError && !session) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 dark:bg-navy-950 p-4">
        <GlassCard className="p-8 max-w-md w-full text-center space-y-4">
          <h2 className="font-serif text-xl font-bold text-red-600">Không thể bắt đầu bài thi</h2>
          <p className="text-sm text-slate-600 dark:text-slate-300">{generalError}</p>
          <Button variant="primary" onClick={() => navigate('/practice')}>
            Quay lại danh mục
          </Button>
        </GlassCard>
      </div>
    )
  }

  if (result) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-navy-950 py-8">
        <MockTestResult
          result={result}
          onRetake={handleRetake}
          onNavigateCatalog={() => navigate('/practice')}
          onReviewSection={(subId) => navigate(`/practice/results/${subId}`)}
        />
      </div>
    )
  }

  const currentSection = session?.sections?.[currentSectionIndex]
  const skill = (currentSection?.skill || 'READING').toUpperCase()

  return (
    <MockTestShell
      session={session}
      currentSectionIndex={currentSectionIndex}
      onSelectSection={setCurrentSectionIndex}
      onPause={handlePause}
      onResume={handleResume}
      onNextSection={handleNextSection}
      onSubmitTest={handleSubmitTest}
      isSaving={isSaving}
      saveError={saveError}
    >
      <div className="space-y-6">
        {generalError && (
          <div className="p-3 bg-red-500/10 border border-red-500/30 rounded-lg text-sm text-red-600 dark:text-red-300">
            {generalError}
          </div>
        )}

        {/* Section Workspace */}
        <div className="bg-white dark:bg-navy-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-6 shadow-sm min-h-[500px] flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between border-b border-slate-100 dark:border-slate-800 pb-4 mb-6">
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider text-gold-600 dark:text-gold-400">
                  Phần {currentSectionIndex + 1} / {session?.sections?.length || 4}
                </span>
                <h2 className="font-serif text-2xl font-bold text-navy-900 dark:text-slate-100">
                  {skill === 'LISTENING' && 'IELTS Listening Section'}
                  {skill === 'READING' && 'IELTS Reading Section'}
                  {skill === 'WRITING' && 'IELTS Writing Section'}
                  {skill === 'SPEAKING' && 'IELTS Speaking Section'}
                </h2>
              </div>
              <div className="text-right text-xs text-slate-500">
                <span>Thời lượng phần thi: </span>
                <span className="font-semibold text-slate-700 dark:text-slate-300">
                  {Math.floor((currentSection?.timeLimitSeconds || 1800) / 60)} phút
                </span>
              </div>
            </div>

            {/* Content for skill */}
            {skill === 'LISTENING' && (
              <div className="space-y-4">
                <div className="p-4 bg-slate-100 dark:bg-slate-800 rounded-xl flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-gold-500/10 text-gold-500 flex items-center justify-center shrink-0">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
                    </svg>
                  </div>
                  <div>
                    <h4 className="font-medium text-sm text-navy-900 dark:text-slate-100">Audio Track (Mock Listening)</h4>
                    <p className="text-xs text-slate-500">Nghe đoạn băng và điền câu trả lời bên dưới.</p>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
                  {[1, 2, 3, 4].map((qNum) => (
                    <div key={qNum} className="space-y-1.5">
                      <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
                        Câu hỏi {qNum}:
                      </label>
                      <input
                        type="text"
                        value={answers[`q${qNum}`] || ''}
                        onChange={(e) => handleAnswerChange(`q${qNum}`, e.target.value)}
                        placeholder="Nhập câu trả lời..."
                        className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-transparent focus:ring-2 focus:ring-gold-500 focus:outline-none"
                      />
                    </div>
                  ))}
                </div>
              </div>
            )}

            {skill === 'READING' && (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="p-4 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-200 dark:border-slate-700 text-sm leading-relaxed space-y-3 max-h-96 overflow-y-auto">
                  <h3 className="font-serif font-bold text-base text-navy-900 dark:text-gold-200">
                    Academic Reading Passage
                  </h3>
                  <p>
                    The evolution of cognitive neuroscience has fundamentally altered our understanding of memory retention and language acquisition. Recent longitudinal studies demonstrate that immersive feedback loops significantly enhance linguistic proficiency...
                  </p>
                  <p>
                    Furthermore, structured practice under timed conditions conditions the neuro-pathways responsible for real-time synthesis and vocabulary recall.
                  </p>
                </div>

                <div className="space-y-4">
                  <h4 className="font-medium text-sm text-navy-900 dark:text-slate-100">Câu hỏi Reading</h4>
                  {[1, 2, 3, 4].map((qNum) => (
                    <div key={qNum} className="space-y-1.5">
                      <label className="text-xs font-medium text-slate-700 dark:text-slate-300">
                        Câu hỏi {qNum}: Chọn TRUE / FALSE / NOT GIVEN hoặc từ thích hợp:
                      </label>
                      <input
                        type="text"
                        value={answers[`q${qNum}`] || ''}
                        onChange={(e) => handleAnswerChange(`q${qNum}`, e.target.value)}
                        placeholder="Câu trả lời..."
                        className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-transparent focus:ring-2 focus:ring-gold-500 focus:outline-none"
                      />
                    </div>
                  ))}
                </div>
              </div>
            )}

            {skill === 'WRITING' && (
              <div className="space-y-4">
                <div className="p-4 bg-amber-500/5 rounded-xl border border-amber-500/20 text-sm space-y-1">
                  <h4 className="font-medium text-amber-800 dark:text-gold-300">Writing Task Prompt:</h4>
                  <p className="text-slate-700 dark:text-slate-300">
                    Some people believe that university education should focus on graduate employability, while others argue it should focus on academic theory. Discuss both views and give your opinion.
                  </p>
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between items-center text-xs text-slate-500">
                    <span>Soạn thảo bài viết:</span>
                    <span>
                      Số từ:{' '}
                      <span className="font-semibold text-slate-700 dark:text-slate-200">
                        {(answers['essay'] || '').trim().split(/\s+/).filter(Boolean).length}
                      </span>{' '}
                      từ
                    </span>
                  </div>
                  <textarea
                    rows={12}
                    value={answers['essay'] || ''}
                    onChange={(e) => handleAnswerChange('essay', e.target.value)}
                    placeholder="Viết bài luận của bạn tại đây (tối thiểu 250 từ)..."
                    className="w-full p-4 text-sm font-sans rounded-xl border border-slate-300 dark:border-slate-700 bg-transparent focus:ring-2 focus:ring-gold-500 focus:outline-none resize-y"
                  />
                </div>
              </div>
            )}

            {skill === 'SPEAKING' && (
              <div className="space-y-6 text-center py-6">
                <div className="max-w-md mx-auto p-4 bg-indigo-500/5 rounded-xl border border-indigo-500/20 space-y-2">
                  <h4 className="font-medium text-indigo-700 dark:text-indigo-300">Speaking Part 2 Cue Card</h4>
                  <p className="text-sm text-slate-700 dark:text-slate-300">
                    Describe an important decision you made in your academic or professional life. You should say what it was, why you made it, and what the result was.
                  </p>
                </div>

                <div className="space-y-3">
                  <div className="w-16 h-16 mx-auto rounded-full bg-red-500/10 text-red-500 flex items-center justify-center animate-pulse">
                    <svg className="w-8 h-8" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M7 4a3 3 0 016 0v4a3 3 0 11-6 0V4zm4 10.93A7.001 7.001 0 0017 8a1 1 0 10-2 0A5 5 0 015 8a1 1 0 00-2 0 7.001 7.001 0 006 6.93V17H6a1 1 0 100 2h8a1 1 0 100-2h-3v-2.07z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <p className="text-xs text-slate-500">Mic đang hoạt động và ghi âm bài nói của bạn.</p>
                </div>
              </div>
            )}
          </div>

          <div className="pt-6 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
            <span className="text-xs text-slate-400">
              * Câu trả lời được tự động lưu liên tục sau mỗi thao tác.
            </span>
            {currentSectionIndex < (session?.sections?.length || 4) - 1 ? (
              <Button variant="primary" size="md" onClick={handleNextSection}>
                Hoàn thành phần này & Chuyển tiếp →
              </Button>
            ) : (
              <Button variant="primary" size="md" onClick={handleSubmitTest}>
                Hoàn tất & Nộp toàn bộ Mock Test
              </Button>
            )}
          </div>
        </div>
      </div>
    </MockTestShell>
  )
}
