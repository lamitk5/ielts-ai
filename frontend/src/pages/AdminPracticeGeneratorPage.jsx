import { useState } from 'react'
import { Sparkles, Layers, FileCheck, ShieldAlert, Plus, RefreshCw } from 'lucide-react'
import PageContainer from '../components/common/PageContainer'
import SectionTitle from '../components/common/SectionTitle'
import GlassCard from '../components/common/GlassCard'
import Button from '../components/common/Button'
import SkeletonBlock from '../components/common/SkeletonBlock'
import GenerationJobTable from '../components/admin/generator/GenerationJobTable'
import NewGenerationWizardModal from '../components/admin/generator/NewGenerationWizardModal'
import { usePracticeGeneratorJobs } from '../features/practice-generator/usePracticeGeneratorJobs'
import { useAuth } from '../features/auth/AuthProvider'
import RagAdminUnlock from '../components/admin/RagAdminUnlock'

export default function AdminPracticeGeneratorPage() {
  const [isWizardOpen, setIsWizardOpen] = useState(false)
  const { user } = useAuth()
  const isAuthenticatedAdmin = user?.role === 'ADMIN'

  const {
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
  } = usePracticeGeneratorJobs()

  const pendingReviewCount = sets.filter((s) => s.state === 'PENDING_REVIEW' || s.state === 'NEEDS_REVISION').length
  const approvedCount = sets.filter((s) => s.state === 'APPROVED').length

  return (
    <PageContainer className="py-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-zinc-800/80 pb-6">
        <div>
          <SectionTitle
            eyebrow="KHÔNG GIAN BIÊN TẬP"
            title="Trình tạo bài luyện bằng AI"
            description="Tạo bài tập IELTS Reading/Listening từ nguồn đã duyệt bản quyền, kiểm định đa tầng tự động và phê duyệt vào ngân hàng đề học viên."
          />
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="secondary"
            onClick={refresh}
            className="py-2 px-3 text-xs flex items-center gap-1.5"
            aria-label="Làm mới"
          >
            <RefreshCw className="w-3.5 h-3.5 text-zinc-400" />
            Làm mới
          </Button>
          <Button
            onClick={() => setIsWizardOpen(true)}
            className="py-2 px-4 text-xs font-bold bg-amber-500 hover:bg-amber-400 text-zinc-950 flex items-center gap-1.5 shadow-lg shadow-amber-500/10"
          >
            <Plus className="w-4 h-4" />
            Tạo bộ đề mới
          </Button>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-800 text-rose-300 text-xs flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 shrink-0 text-rose-400" />
          <span>{error}</span>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <GlassCard className="p-4 border-zinc-800/80 space-y-1">
          <div className="flex items-center justify-between text-zinc-400 text-xs">
            <span>Tổng số bộ đề tạo</span>
            <Layers className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-bold text-zinc-100 font-serif">{sets.length}</div>
          <div className="text-[11px] text-zinc-500">Tất cả trạng thái quy trình</div>
        </GlassCard>

        <GlassCard className="p-4 border-zinc-800/80 space-y-1">
          <div className="flex items-center justify-between text-zinc-400 text-xs">
            <span>Chờ quản trị viên duyệt</span>
            <Sparkles className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-2xl font-bold text-indigo-300 font-serif">{pendingReviewCount}</div>
          <div className="text-[11px] text-zinc-500">Cần quản trị viên phê duyệt</div>
        </GlassCard>

        <GlassCard className="p-4 border-zinc-800/80 space-y-1">
          <div className="flex items-center justify-between text-zinc-400 text-xs">
            <span>Đã xuất bản</span>
            <FileCheck className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-emerald-300 font-serif">{approvedCount}</div>
          <div className="text-[11px] text-zinc-500">Học viên có thể làm bài</div>
        </GlassCard>

        <GlassCard className="p-4 border-zinc-800/80 space-y-1">
          <div className="flex items-center justify-between text-zinc-400 text-xs">
            <span>Nguồn dữ liệu</span>
            <Layers className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl font-bold text-amber-300 font-serif">{sources.length}</div>
          <div className="text-[11px] text-zinc-500">Đã đăng ký bản quyền</div>
        </GlassCard>
      </div>

      {/* Main Table */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-zinc-200 uppercase tracking-wider font-mono">
            Danh sách bộ đề & tác vụ tạo
          </h3>
          <span className="text-xs text-zinc-500">Tự động đồng bộ mỗi 3 giây</span>
        </div>

        {loading && sets.length === 0 ? (
          <SkeletonBlock className="h-48 w-full rounded-xl" label="Đang tải danh sách tác vụ..." />
        ) : (
          <GenerationJobTable jobs={jobs} sets={sets} />
        )}
      </div>

      {/* Creation Wizard Modal */}
      <NewGenerationWizardModal
        isOpen={isWizardOpen}
        onClose={() => setIsWizardOpen(false)}
        sources={sources}
        blueprints={blueprints}
        onRegisterSource={registerSource}
        onExtractBlueprint={extractBlueprint}
        onCreateJob={createJob}
      />
    </PageContainer>
  )
}
