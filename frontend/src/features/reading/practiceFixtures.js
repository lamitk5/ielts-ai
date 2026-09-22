export const practiceFixtures = {
  reading: {
    name: 'Reading',
    description: 'Synthetic practice set · luyện xác định ý chính và chi tiết.',
    setId: 'reading-foundation-01',
    questions: [
      { id: 'reading-q1', prompt: 'The passage describes a new study method. What is its main purpose?', options: ['To replace teachers', 'To improve recall', 'To reduce reading', 'To test speed'] },
      { id: 'reading-q2', prompt: 'Which result did the researchers observe?', options: ['More accurate summaries', 'Longer exams', 'Fewer participants', 'Higher costs'] },
    ],
  },
  listening: {
    name: 'Listening',
    description: 'Synthetic practice set · nghe theo ngữ cảnh và xem lại transcript sau khi trả lời.',
    setId: 'listening-foundation-01',
    questions: [
      { id: 'listening-q1', prompt: 'What time does the library open?', options: ['7:30', '8:00', '8:30', '9:00'] },
      { id: 'listening-q2', prompt: 'Which room is reserved?', options: ['Room 2', 'Room 4', 'Room 6', 'Room 8'] },
    ],
  },
}
