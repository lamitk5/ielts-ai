import { useState } from 'react'
import { X, ArrowRight, ArrowLeft, Check, Sparkles, AlertCircle } from 'lucide-react'
import Button from '../../common/Button'
import GlassCard from '../../common/GlassCard'
import SourceRegistrationTab from './SourceRegistrationTab'
import BlueprintSelectionTab from './BlueprintSelectionTab'

const RIGHTS_LABELS = { APPROVED: 'Đã duyệt', PENDING_REVIEW: 'Chờ duyệt', RESTRICTED: 'Hạn chế', REJECTED: 'Từ chối' }

export default function NewGenerationWizardModal({
  isOpen,
  onClose,
  sources = [],
  blueprints = [],
  onRegisterSource,
  onExtractBlueprint,
  onCreateJob,
}) {
  const [step, setStep] = useState(1) // 1: Source, 2: Blueprint, 3: Confirm
  const [selectedSource, setSelectedSource] = useState(null)
  const [selectedBlueprint, setSelectedBlueprint] = useState(null)
  const [domainTopic, setDomainTopic] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  if (!isOpen) return null

  const handleLaunch = async () => {
    if (!selectedSource) {
      setError('Vui lòng chọn nguồn tài liệu đã duyệt.')
      return
    }
    setError('')
    setSubmitting(true)
    try {
      await onCreateJob({
        sourceId: selectedSource.id,
        blueprintId: selectedBlueprint?.id || null,
        skill: selectedSource.skill || 'READING',
        domainTopic: domainTopic.trim() || undefined,
      })
      onClose()
    } catch (err) {
      const genericProviderError = err?.code === 'AI_PROVIDER_ERROR'
        || err?.message === 'Trợ giảng AI chưa thể trả lời lúc này.'
      setError(genericProviderError
        ? 'Không thể tạo bài bằng AI lúc này. Vui lòng thử lại.'
        : (err.message || 'Không thể tạo bài bằng AI lúc này. Vui lòng thử lại.'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-2xl">
        <GlassCard className="generator-modal max-h-[calc(100vh-2rem)] overflow-y-auto p-6 border shadow-2xl space-y-6">
          {/* Header */}
          <div className="generator-modal-header flex items-center justify-between border-b pb-4">
            <div>
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-amber-400" />
                <h3 className="generator-modal-title text-base font-serif font-bold">Khởi tạo đề thi IELTS AI mới</h3>
              </div>
              <p className="generator-modal-subtitle text-xs mt-0.5">
                Quy trình 3 bước: Nguồn dữ liệu &rarr; Cấu trúc Blueprint &rarr; AI tạo và kiểm tra tự động
              </p>
            </div>
            <button
              onClick={onClose}
              className="generator-modal-close p-1 rounded hover:bg-black/10"
              aria-label="Đóng"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Stepper indicator */}
          <div className="flex items-center justify-between px-6">
            <div className={`generator-step-label flex items-center gap-2 ${step >= 1 ? 'is-active' : 'is-inactive'}`}>
              <div
                  className={`generator-step-number w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                  step >= 1 ? 'is-active' : 'is-inactive'
                }`}
              >
                1
              </div>
              <span className="text-xs font-medium">Nguồn học liệu</span>
            </div>
            <div className={`generator-step-divider h-px flex-1 mx-4 ${step >= 2 ? 'is-active' : 'is-inactive'}`} />
            <div className={`generator-step-label flex items-center gap-2 ${step >= 2 ? 'is-active' : 'is-inactive'}`}>
              <div
                  className={`generator-step-number w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                  step >= 2 ? 'is-active' : 'is-inactive'
                }`}
              >
                2
              </div>
                <span className="text-xs font-medium">Bản thiết kế</span>
            </div>
            <div className={`generator-step-divider h-px flex-1 mx-4 ${step >= 3 ? 'is-active' : 'is-inactive'}`} />
            <div className={`generator-step-label flex items-center gap-2 ${step >= 3 ? 'is-active' : 'is-inactive'}`}>
              <div
                  className={`generator-step-number w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${
                  step >= 3 ? 'is-active' : 'is-inactive'
                }`}
              >
                3
              </div>
              <span className="text-xs font-medium">Xác nhận</span>
            </div>
          </div>

          {error && (
            <div role="alert" aria-live="polite" className="generator-error flex items-center gap-2 p-3 text-xs rounded-lg">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Step 1: Source */}
          {step === 1 && (
            <div className="space-y-4">
              <SourceRegistrationTab
                sources={sources}
                onSelectExisting={(source) => {
                  setSelectedSource(source)
                  setStep(2)
                }}
                onRegistered={onRegisterSource}
              />
            </div>
          )}

          {/* Step 2: Blueprint */}
          {step === 2 && (
            <div className="space-y-4">
              <BlueprintSelectionTab
                source={selectedSource}
                blueprints={blueprints}
                selectedBlueprint={selectedBlueprint}
                onSelect={(bp) => {
                  setSelectedBlueprint(bp)
                  setStep(3)
                }}
                onExtractFromSource={onExtractBlueprint}
              />
            </div>
          )}

          {/* Step 3: Confirmation */}
          {step === 3 && (
            <div className="space-y-4">
              <div className="generator-confirm-card p-4 rounded-xl border space-y-3">
                <div className="generator-confirm-title text-xs font-semibold flex items-center gap-1.5">
                  <Check className="w-4 h-4 text-emerald-400" />
                  Sẵn sàng tạo bài với cấu hình:
                </div>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <span className="generator-field-label">Nguồn tài liệu:</span>
                    <p className="generator-field-value font-medium">{selectedSource?.title}</p>
                  </div>
                  <div>
                    <span className="generator-field-label">Trạng thái bản quyền:</span>
                    <p className="generator-field-value generator-status-approved font-medium">{RIGHTS_LABELS[selectedSource?.rightsStatus] || selectedSource?.rightsStatus}</p>
                  </div>
                  <div>
                    <span className="generator-field-label">Bản thiết kế mục tiêu:</span>
                    <p className="generator-field-value font-medium">
                      {selectedBlueprint ? selectedBlueprint.title : 'Tự động trích xuất từ nguồn'}
                    </p>
                  </div>
                  <div>
                    <span className="generator-field-label">Band mục tiêu:</span>
                    <p className="generator-field-value generator-band-value font-medium">
                      Band {selectedBlueprint?.targetBand || '7.5'}
                    </p>
                  </div>
                </div>

                <div>
                  <label className="generator-field-label block text-xs mb-1">Chủ đề mở rộng (Tùy chọn)</label>
                  <input
                    type="text"
                    value={domainTopic}
                    onChange={(e) => setDomainTopic(e.target.value)}
                    placeholder="VD: Environmental biotechnology in ocean conservation"
                    className="generator-field-input w-full border rounded px-3 py-1.5 text-xs focus:outline-none focus:border-amber-500"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Footer controls */}
          <div className="generator-modal-footer flex justify-between items-center pt-2 border-t">
            {step > 1 ? (
              <Button
                variant="secondary"
                onClick={() => setStep(step - 1)}
                className="py-1.5 px-3 text-xs flex items-center gap-1"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                Quay lại
              </Button>
            ) : (
              <div />
            )}

            {step < 3 ? (
              <Button
                disabled={step === 1 && !selectedSource}
                onClick={() => setStep(step + 1)}
                className="py-1.5 px-3 text-xs flex items-center gap-1"
              >
                Tiếp tục
                <ArrowRight className="w-3.5 h-3.5" />
              </Button>
            ) : (
              <Button
                onClick={handleLaunch}
                disabled={submitting}
                className="py-1.5 px-4 text-xs flex items-center gap-1.5 bg-amber-500 hover:bg-amber-400 text-zinc-950 font-bold"
              >
                <Sparkles className="w-3.5 h-3.5" />
                {submitting ? 'Đang khởi động...' : 'Bắt đầu tạo bài AI'}
              </Button>
            )}
          </div>
        </GlassCard>
      </div>
    </div>
  )
}
