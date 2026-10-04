import React from 'react'

export default function SpeakingResultShell({
  submission,
  review,
  audioUrl,
  isLoading = false,
  error = null,
}) {
  if (isLoading) {
    return (
      <div className="animate-pulse space-y-6 rounded-3xl border border-amber-500/20 bg-slate-900/80 p-6 backdrop-blur-xl">
        <div className="h-8 w-48 rounded-lg bg-amber-500/10" />
        <div className="h-24 w-full rounded-2xl bg-slate-800/40" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="h-32 rounded-2xl bg-slate-800/40" />
          <div className="h-32 rounded-2xl bg-slate-800/40" />
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-3xl border border-rose-500/30 bg-rose-950/20 p-6 text-slate-100 backdrop-blur-xl">
        <div className="flex items-center space-x-3 text-rose-400">
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <h3 className="font-serif text-lg font-semibold">Không thể tải đánh giá Speaking</h3>
        </div>
        <p className="mt-2 text-sm text-slate-300">
          {error?.message || 'Có lỗi xảy ra khi tải đánh giá Speaking.'}
        </p>
      </div>
    )
  }

  const isGraded = review && review.overallBand != null

  return (
    <div className="space-y-6" data-testid="speaking-result-shell">
      {/* Overall Band Card */}
      <div className="relative overflow-hidden rounded-3xl border border-amber-500/30 bg-gradient-to-br from-slate-900/90 via-amber-950/20 to-slate-900/90 p-6 shadow-2xl backdrop-blur-xl">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <span className="text-xs font-semibold uppercase tracking-wider text-amber-400">
              Kết quả Speaking
            </span>
            <h2 className="font-serif text-3xl font-bold text-slate-100">
              {isGraded ? (
                <>
                  Band Overall: <span className="text-amber-300">{review.overallBand.toFixed(1)}</span>
                </>
              ) : (
                <span className="text-amber-200/90">Đang chờ chấm / Chưa có đánh giá</span>
              )}
            </h2>
            <p className="mt-1 text-xs text-amber-200/70 italic">
              {isGraded
                ? 'Đánh giá bởi giám khảo chuyên môn — Không phải điểm thi IELTS chính thức.'
                : 'Bài nói đã được lưu trữ an toàn; không tự động tạo điểm phát âm hay STT giả.'}
            </p>
          </div>
        </div>
      </div>

      {/* Audio & Transcript Box */}
      <div className="rounded-3xl border border-slate-800 bg-slate-900/60 p-6 backdrop-blur-xl">
        <h3 className="font-serif text-lg font-bold text-slate-100 mb-4">Bài nộp của bạn</h3>
        {audioUrl ? (
          <div className="mb-4">
            <p className="text-xs font-medium text-slate-400 mb-2">Bản ghi âm:</p>
            <audio controls className="w-full" src={audioUrl} data-testid="speaking-audio-player">
              Trình duyệt của bạn không hỗ trợ phát audio.
            </audio>
          </div>
        ) : null}

        <div>
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-medium text-slate-400">Nội dung câu trả lời (Transcript):</p>
            <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-amber-300">
              Nguồn: {submission?.transcriptSource || 'MANUAL'}
            </span>
          </div>
          <div className="rounded-2xl border border-slate-800 bg-slate-950/50 p-4 text-sm text-slate-200 whitespace-pre-wrap leading-relaxed">
            {submission?.transcript || 'Chưa có nội dung văn bản cho bài nói này.'}
          </div>
        </div>
      </div>

      {/* Criteria Breakdown if Graded */}
      {isGraded ? (
        <div className="rounded-3xl border border-slate-800 bg-slate-900/60 p-6 backdrop-blur-xl">
          <h3 className="font-serif text-xl font-bold text-slate-100 mb-4">
            Chi tiết 4 tiêu chí chấm điểm
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="rounded-2xl border border-slate-800 bg-slate-800/40 p-4 text-center">
              <span className="text-xs text-slate-400">Fluency & Coherence</span>
              <p className="text-2xl font-bold text-amber-300 mt-1">
                {review.fluencyCoherence != null ? review.fluencyCoherence.toFixed(1) : '-'}
              </p>
            </div>
            <div className="rounded-2xl border border-slate-800 bg-slate-800/40 p-4 text-center">
              <span className="text-xs text-slate-400">Lexical Resource</span>
              <p className="text-2xl font-bold text-amber-300 mt-1">
                {review.lexicalResource != null ? review.lexicalResource.toFixed(1) : '-'}
              </p>
            </div>
            <div className="rounded-2xl border border-slate-800 bg-slate-800/40 p-4 text-center">
              <span className="text-xs text-slate-400">Grammatical Range</span>
              <p className="text-2xl font-bold text-amber-300 mt-1">
                {review.grammaticalRange != null ? review.grammaticalRange.toFixed(1) : '-'}
              </p>
            </div>
            <div className="rounded-2xl border border-slate-800 bg-slate-800/40 p-4 text-center">
              <span className="text-xs text-slate-400">Pronunciation</span>
              <p className="text-2xl font-bold text-amber-300 mt-1">
                {review.pronunciation != null ? review.pronunciation.toFixed(1) : '-'}
              </p>
            </div>
          </div>

          {review.reviewerFeedback ? (
            <div className="mt-6 rounded-2xl border border-amber-500/20 bg-amber-950/10 p-5">
              <h4 className="font-serif text-sm font-semibold text-amber-300 mb-2">Nhận xét của Giám khảo</h4>
              <p className="text-xs text-slate-200 leading-relaxed whitespace-pre-wrap">{review.reviewerFeedback}</p>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
