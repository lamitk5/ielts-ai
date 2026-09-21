export const skillCards = [
  {
    id: 'reading',
    name: 'Reading',
    description: 'Luyện đọc theo dạng đề và hỏi AI vì sao đáp án đúng hoặc sai.',
    metric: '40 bộ đề mẫu',
  },
  {
    id: 'listening',
    name: 'Listening',
    description: 'Luyện nghe theo cấu trúc IELTS và xem giải thích theo ngữ cảnh.',
    metric: '32 bộ đề mẫu',
  },
  {
    id: 'writing',
    name: 'Writing',
    description: 'Task 1 & 2 với môi trường viết, band ước lượng và phản hồi theo rubric.',
    metric: '24 chủ đề mẫu',
  },
  {
    id: 'speaking',
    name: 'Speaking',
    description: 'Mô phỏng Part 1–3, sẵn sàng cho STT và phản hồi AI ở phase sau.',
    metric: '18 chủ đề mẫu',
  },
]

export const tutorGroundedDemo = {
  content: 'Mình sẽ giải thích dựa trên rubric và ngữ cảnh bài đang luyện.',
  grounding: { status: 'grounded', sourceCount: 2 },
  citations: [
    { sourceId: 'demo-rubric-01', title: 'Rubric Writing Task 2', section: 'Task Response' },
    { sourceId: 'demo-guide-01', title: 'Hướng dẫn cải thiện lập luận', section: 'Phát triển ý' },
  ],
}

export const tutorInsufficientDemo = {
  content: 'Chưa đủ thông tin để trả lời chắc chắn. Hãy cung cấp câu hỏi, đoạn văn hoặc bài làm liên quan.',
  grounding: { status: 'insufficient_context', sourceCount: 0 },
  citations: [],
}

export const memberDemo = {
  user: {
    firstName: 'Đăng',
    targetBand: 7,
    examDate: '2026-12-20',
  },
  progress: [
    { skill: 'Reading', band: 6.5 },
    { skill: 'Listening', band: 7.0 },
    { skill: 'Writing', band: 6.0 },
    { skill: 'Speaking', band: 6.5 },
  ],
  mistakes: [
    {
      label: 'Article usage',
      frequency: 8,
      skill: 'Writing',
      hint: 'Kiểm tra a/an/the theo danh từ đếm được.',
    },
    {
      label: 'Subject–verb agreement',
      frequency: 5,
      skill: 'Writing',
      hint: 'Soát chủ ngữ số ít trước khi chia động từ.',
    },
    {
      label: 'Weak topic sentence',
      frequency: 3,
      skill: 'Writing',
      hint: 'Nêu rõ luận điểm chính ngay đầu đoạn.',
    },
  ],
}

export const guestDemo = {
  user: null,
  progress: null,
  mistakes: [],
}

export const commonMistakes = memberDemo.mistakes
