export function ReadingPassagePane({ practiceSet }) {
  const passage = practiceSet.passage
  const paragraphs = Array.isArray(passage?.paragraphs) ? passage.paragraphs : []
  const plainText = typeof passage === 'string' ? passage : typeof passage?.text === 'string' ? passage.text : ''
  const title = passage?.title ?? practiceSet.title ?? 'Bài đọc'

  return (
    <article className="reading-passage" data-reading-target-id={practiceSet.setId ?? practiceSet.id} tabIndex={-1}>
      <p className="reading-pane-kicker">NỘI DUNG ĐỌC</p>
      <h2 className="font-display">{title}</h2>
      {paragraphs.length ? (
        <div className="reading-passage-body">
          {paragraphs.map((paragraph, index) => {
            const text = typeof paragraph === 'string' ? paragraph : paragraph.text
            const targetId = typeof paragraph === 'string' ? `${practiceSet.setId ?? practiceSet.id}-p${index + 1}` : paragraph.id
            return <div className="reading-paragraph" key={targetId ?? index} data-reading-target-id={targetId} tabIndex={-1}>
              <span className="reading-paragraph-number">Đoạn {index + 1}</span>
              <p>{text}</p>
            </div>
          })}
        </div>
      ) : plainText ? <p className="reading-passage-text">{plainText}</p> : (
        <p className="reading-passage-empty">Nội dung đoạn đọc chưa được cung cấp cho bộ đề này. Bạn vẫn có thể luyện trả lời các câu hỏi bên cạnh.</p>
      )}
    </article>
  )
}
