function TutorGroundingBadge({ status, sourceCount = 0 }) {
  const isGrounded = status === 'grounded'
  const isInsufficient = status === 'insufficient_context'

  if (!isGrounded && !isInsufficient) return null

  return (
    <span className={`tutor-grounding-badge ${isGrounded ? 'tutor-grounding-grounded' : ''}`.trim()}>
      {isGrounded ? 'Dựa trên nguồn tham chiếu đã kiểm chứng' : 'Cần thêm ngữ cảnh'}
      {isGrounded ? ` · ${sourceCount} nguồn` : null}
    </span>
  )
}

export default TutorGroundingBadge
