export const GENERATION_STATES = {
  DRAFT: 'DRAFT',
  GENERATING: 'GENERATING',
  AUTO_VALIDATING: 'AUTO_VALIDATING',
  PENDING_REVIEW: 'PENDING_REVIEW',
  APPROVED: 'APPROVED',
  NEEDS_REVISION: 'NEEDS_REVISION',
  REJECTED: 'REJECTED',
}

export const RIGHTS_STATUSES = {
  PENDING_REVIEW: 'PENDING_REVIEW',
  APPROVED: 'APPROVED',
  RESTRICTED: 'RESTRICTED',
  REJECTED: 'REJECTED',
}

export const VALIDATION_STATUSES = {
  PASS: 'PASS',
  WARNING: 'WARNING',
  FAIL: 'FAIL',
}

export const REVIEW_ACTIONS = {
  APPROVE: 'APPROVE',
  REQUEST_REVISION: 'REQUEST_REVISION',
  REJECT: 'REJECT',
}

export const STATE_LABELS = {
  DRAFT: 'Bản nháp',
  GENERATING: 'Đang tổng hợp AI',
  AUTO_VALIDATING: 'Đang kiểm định tự động',
  PENDING_REVIEW: 'Chờ biên tập viên duyệt',
  APPROVED: 'Đã phê duyệt & xuất bản',
  NEEDS_REVISION: 'Yêu cầu hiệu chỉnh',
  REJECTED: 'Từ chối',
}

export const STATE_BADGE_STYLES = {
  DRAFT: 'bg-zinc-800 text-zinc-300 border-zinc-700',
  GENERATING: 'bg-amber-950/60 text-amber-300 border-amber-800/80 animate-pulse',
  AUTO_VALIDATING: 'bg-sky-950/60 text-sky-300 border-sky-800/80',
  PENDING_REVIEW: 'bg-indigo-950/60 text-indigo-300 border-indigo-800/80',
  APPROVED: 'bg-emerald-950/60 text-emerald-300 border-emerald-800/80',
  NEEDS_REVISION: 'bg-orange-950/60 text-orange-300 border-orange-800/80',
  REJECTED: 'bg-rose-950/60 text-rose-300 border-rose-800/80',
}
