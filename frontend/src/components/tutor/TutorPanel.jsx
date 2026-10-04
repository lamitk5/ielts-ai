import TutorShell, { SHELL_STATES } from './TutorShell'

function TutorPanel({
  messages,
  loading,
  onClose,
  onSend,
  inputRef,
  context,
  onClearContext,
  onCancel,
  onRetry,
  state = SHELL_STATES.STANDARD,
  onStateChange,
  attachment,
  attachments,
  onAttachmentSelected,
  onAttachmentError,
  onAttachmentsSelected,
  onRemoveAttachment,
  onRetryAttachment,
  suggestions,
}) {
  return (
    <TutorShell
      state={state}
      context={context}
      messages={messages}
      loading={loading}
      onClose={onClose}
      onSend={onSend}
      onCancel={onCancel}
      onRetry={onRetry}
      onStateChange={onStateChange}
      onClearContext={onClearContext}
      inputRef={inputRef}
      attachment={attachment}
      attachments={attachments}
      onAttachmentSelected={onAttachmentSelected}
      onAttachmentsSelected={onAttachmentsSelected}
      onAttachmentError={onAttachmentError}
      onRemoveAttachment={onRemoveAttachment}
      onRetryAttachment={onRetryAttachment}
      suggestions={suggestions}
    />
  )
}

export default TutorPanel
