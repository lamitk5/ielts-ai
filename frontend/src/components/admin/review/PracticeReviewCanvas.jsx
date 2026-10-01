import { useState } from 'react'
import SimilarityInspectorBanner from './SimilarityInspectorBanner'
import PassageReviewPane from './PassageReviewPane'
import QuestionReviewPane from './QuestionReviewPane'
import ValidationReportDrawer from './ValidationReportDrawer'
import ReviewActionToolbar from './ReviewActionToolbar'
import VersionDiffModal from './VersionDiffModal'

export default function PracticeReviewCanvas({
  payload,
  onApprove,
  onRequestRevision,
  onReject,
  onRegenerateQuestion,
  onCompareVersions,
  comparison,
  submitting,
}) {
  const [hoveredQuestion, setHoveredQuestion] = useState(null)
  const [isDiffOpen, setIsDiffOpen] = useState(false)

  if (!payload) return null

  const { practiceSet, currentVersion, validationResults = [], versionHistory = [] } = payload

  const passage =
    typeof currentVersion?.passageContent === 'string'
      ? JSON.parse(currentVersion.passageContent)
      : currentVersion?.passageContent

  const questions =
    typeof currentVersion?.questionsPayload === 'string'
      ? JSON.parse(currentVersion.questionsPayload)
      : currentVersion?.questionsPayload || []

  return (
    <div className="space-y-6">
      {/* Similarity & Novelty Heuristic Header Banner */}
      <SimilarityInspectorBanner validationResults={validationResults} />

      {/* Split-Screen Canvas: Left Passage, Right Questions */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 min-h-[550px] items-stretch">
        <div className="h-full">
          <PassageReviewPane
            passage={passage}
            activeEvidence={hoveredQuestion?.evidenceSpan}
            hoveredQuestionId={hoveredQuestion?.id}
          />
        </div>
        <div className="h-full">
          <QuestionReviewPane
            questions={questions}
            onHoverQuestion={setHoveredQuestion}
            onRegenerateQuestion={onRegenerateQuestion}
            regenerating={submitting}
          />
        </div>
      </div>

      {/* Multi-Tier Validation Report Breakdown Drawer */}
      <ValidationReportDrawer validationResults={validationResults} />

      {/* Action Toolbar */}
      <ReviewActionToolbar
        state={practiceSet?.state}
        versionNumber={currentVersion?.versionNumber || 1}
        onApprove={onApprove}
        onRequestRevision={onRequestRevision}
        onReject={onReject}
        onOpenDiff={() => setIsDiffOpen(true)}
        submitting={submitting}
      />

      {/* Version Diff Modal */}
      <VersionDiffModal
        isOpen={isDiffOpen}
        onClose={() => setIsDiffOpen(false)}
        versionHistory={versionHistory}
        onCompare={onCompareVersions}
        comparison={comparison}
      />
    </div>
  )
}
