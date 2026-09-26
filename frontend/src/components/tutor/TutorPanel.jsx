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
    />
  )
}

export default TutorPanel
