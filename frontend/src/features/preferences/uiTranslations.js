const translations = {
  vi: {
    navHome: 'Trang chủ', navSkills: '4 kỹ năng', navTutor: 'Trợ giảng AI', navProgress: 'Tiến độ',
    signIn: 'Đăng nhập', account: 'Tài khoản', settings: 'Cài đặt', closeSettings: 'Đóng cài đặt',
    settingsIntro: 'Tùy chỉnh trải nghiệm học của bạn.',
    appearance: 'Giao diện & Hiển thị', motion: 'Chuyển động & Trợ năng', learning: 'Không gian học tập & AI Tutor',
    theme: 'Giao diện', accent: 'Màu nhấn', density: 'Mật độ hiển thị', animation: 'Cho phép hiệu ứng giao diện (Animation)',
    themeDark: 'Tối', themeLight: 'Sáng', themeSystem: 'Theo hệ thống',
    densitySpacious: 'Thoáng', densityDefault: 'Tiêu chuẩn', densityCompact: 'Gọn',
    fontDecrease: 'A-', fontIncrease: 'A+',
    fontSize: 'Cỡ chữ', language: 'Ngôn ngữ', vietnamese: 'Tiếng Việt', english: 'English',
    proactive: 'AI gợi ý chủ động', crossHighlight: 'Bật hiệu ứng sáng vùng lỗi sai (Cross-highlighting)',
    countdown: 'Hiển thị đồng hồ đếm ngược', readingSplit: 'Tỷ lệ chia Reading', writingSplit: 'Tỷ lệ chia Writing',
    reset: 'Khôi phục mặc định', resetLegend: 'Đặt lại',
    heroEyebrow: 'IELTS 4 KỸ NĂNG • AI TUTOR 24/7', heroTitleLine1: 'Bứt phá Band điểm IELTS', heroTitleLine2: 'cùng', heroTitlePhrase: 'Trợ giảng AI', heroTitleLine3: 'Độc quyền', heroDescription: 'Luyện tập Reading, Listening, Writing và Speaking trên một nền tảng duy nhất. Nhận phản hồi theo ngữ cảnh và cải thiện từng kỹ năng cùng trợ giảng AI.',
    assessment: 'Làm bài Test đánh giá năng lực', exploreSkills: 'Khám phá 4 kỹ năng',
    login: 'Đăng nhập', register: 'Tạo tài khoản', loginCopy: 'Tiếp tục lộ trình bốn kỹ năng và xem tiến bộ của bạn.',
    registerCopy: 'Lưu bài luyện và theo dõi tiến bộ bốn kỹ năng trong một lộ trình riêng.',
    displayName: 'Tên hiển thị', password: 'Mật khẩu', confirmPassword: 'Xác nhận mật khẩu',
    showPassword: 'Hiện', hidePassword: 'Ẩn', passwordConfirmationVisibility: 'mật khẩu xác nhận',
    noAccount: 'Chưa có tài khoản?', hasAccount: 'Đã có tài khoản?', createAccount: 'Tạo tài khoản',
    statusIdle: 'Chưa thay đổi', statusLoading: 'Đang tải', statusSaving: 'Đang lưu', statusSynced: 'Đã đồng bộ', statusUnsynced: 'Chưa đồng bộ', statusConflict: 'Đã cập nhật từ tài khoản',
    closeByBackdrop: 'Đóng cài đặt bằng nền', confirmReset: 'Xác nhận khôi phục', cancel: 'Hủy',
  },
  en: {
    navHome: 'Home', navSkills: '4 skills', navTutor: 'AI Tutor', navProgress: 'Progress',
    signIn: 'Sign in', account: 'Account', settings: 'Settings', closeSettings: 'Close settings',
    settingsIntro: 'Customize your learning experience.',
    appearance: 'Appearance & Display', motion: 'Motion & Accessibility', learning: 'Learning Space & AI Tutor',
    theme: 'Theme', accent: 'Accent color', density: 'Display density', animation: 'Allow interface effects (Animation)',
    themeDark: 'Dark', themeLight: 'Light', themeSystem: 'System',
    densitySpacious: 'Spacious', densityDefault: 'Standard', densityCompact: 'Compact',
    fontDecrease: 'A-', fontIncrease: 'A+',
    fontSize: 'Font size', language: 'Language', vietnamese: 'Vietnamese', english: 'English',
    proactive: 'Proactive AI suggestions', crossHighlight: 'Enable error highlighting (Cross-highlighting)',
    countdown: 'Show practice countdown', readingSplit: 'Reading split ratio', writingSplit: 'Writing split ratio',
    reset: 'Restore defaults', resetLegend: 'Reset',
    heroEyebrow: '4 IELTS SKILLS • AI TUTOR 24/7', heroTitleLine1: 'Break through your IELTS band', heroTitleLine2: 'with', heroTitlePhrase: 'your AI Tutor', heroTitleLine3: 'Exclusive', heroDescription: 'Practice Reading, Listening, Writing and Speaking on one focused platform. Get contextual feedback and improve every skill with your AI tutor.',
    assessment: 'Take your placement test', exploreSkills: 'Explore 4 skills',
    login: 'Sign in', register: 'Create account', loginCopy: 'Continue your four-skill journey and see your progress.',
    registerCopy: 'Save practice work and follow your four-skill progress in one focused path.',
    displayName: 'Display name', password: 'Password', confirmPassword: 'Confirm password',
    showPassword: 'Show', hidePassword: 'Hide', passwordConfirmationVisibility: 'confirmation password',
    noAccount: "Don't have an account?", hasAccount: 'Already have an account?', createAccount: 'Create account',
    statusIdle: 'No changes', statusLoading: 'Loading', statusSaving: 'Saving', statusSynced: 'Synced', statusUnsynced: 'Not synced', statusConflict: 'Updated from account',
    closeByBackdrop: 'Close settings with backdrop', confirmReset: 'Confirm reset', cancel: 'Cancel',
  },
}

export function translate(language, key, fallback = key) {
  return translations[language]?.[key] ?? translations.vi[key] ?? fallback
}

export default translations
