import { Route, Routes } from 'react-router-dom'
import AppLayout from './components/layout/AppLayout'
import HomePage from './pages/HomePage'
import PlaceholderPage from './pages/PlaceholderPage'
import AdminRagPage from './pages/AdminRagPage'
import AdminPracticeGeneratorPage from './pages/AdminPracticeGeneratorPage'
import AdminPracticeReviewPage from './pages/AdminPracticeReviewPage'
import LoginPage from './pages/LoginPage'
import { AuthProvider } from './features/auth/AuthProvider'
import { PreferenceProvider } from './features/preferences/PreferenceProvider'
import PracticePage from './pages/PracticePage'
import WritingPage from './pages/WritingPage'
import SpeakingPage from './pages/SpeakingPage'
import SearchPage from './pages/SearchPage'
import AssessmentPage from './pages/AssessmentPage'
import RegisterPage from './pages/RegisterPage'
import RequireAdminRoute from './features/auth/RequireAdminRoute'
import { readAdminToken } from './services/ragAdminApi'
import PracticeCatalogPage from './pages/PracticeCatalogPage'
import PracticeDetailPage from './pages/PracticeDetailPage'
import PracticeAttemptPage from './pages/PracticeAttemptPage'
import PracticeResultPage from './pages/PracticeResultPage'
import SubmissionHistoryPage from './pages/SubmissionHistoryPage'
import AdminSubmissionReviewPage from './pages/AdminSubmissionReviewPage'
import DiagnosticPage from './pages/DiagnosticPage'
import ErrorNotebookPage from './pages/ErrorNotebookPage'
import MockTestPage from './pages/MockTestPage'
import SavedPracticesPage from './pages/SavedPracticesPage'
import ProfilePage from './pages/ProfilePage'
import AdminLayout from './components/admin/AdminLayout'
import AdminDashboardPage from './pages/AdminDashboardPage'
import AdminPracticeBankPage from './pages/AdminPracticeBankPage'
import AdminKnowledgePage from './pages/AdminKnowledgePage'
import AdminReviewQueuePage from './pages/AdminReviewQueuePage'
import AdminLearnersPage from './pages/AdminLearnersPage'
import AdminUsagePage from './pages/AdminUsagePage'
import AdminAuditPage from './pages/AdminAuditPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'

function App() {
  return (
    <AuthProvider>
      <PreferenceProvider>
        <Routes>
          <Route path="admin" element={<RequireAdminRoute><AdminLayout /></RequireAdminRoute>}>
            <Route index element={<AdminDashboardPage />} />
            <Route path="generator" element={<AdminPracticeGeneratorPage />} />
            <Route path="generator/sets/:setId" element={<AdminPracticeReviewPage />} />
            <Route path="practices" element={<AdminPracticeBankPage />} />
            <Route path="knowledge" element={<AdminKnowledgePage />} />
            <Route path="knowledge/rag" element={<AdminRagPage />} />
            <Route path="reviews" element={<AdminReviewQueuePage />} />
            <Route path="learners" element={<AdminLearnersPage />} />
            <Route path="usage" element={<AdminUsagePage />} />
            <Route path="audit" element={<AdminAuditPage />} />
          </Route>
          <Route element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route path="admin/rag" element={<RequireAdminRoute allowLegacyToken={Boolean(readAdminToken())}><AdminRagPage /></RequireAdminRoute>} />
          <Route path="admin/practice-generator" element={<RequireAdminRoute><AdminPracticeGeneratorPage /></RequireAdminRoute>} />
          <Route path="admin/practice-generator/sets/:setId" element={<RequireAdminRoute><AdminPracticeReviewPage /></RequireAdminRoute>} />
          <Route path="admin/submissions" element={<RequireAdminRoute><AdminSubmissionReviewPage /></RequireAdminRoute>} />
          <Route path="assessment" element={<AssessmentPage />} />
          <Route path="diagnostic" element={<DiagnosticPage />} />
          <Route path="error-notebook" element={<ErrorNotebookPage />} />
          <Route path="practice" element={<PracticeCatalogPage />} />
          <Route path="practice/saved" element={<SavedPracticesPage />} />
          <Route path="practice/mock-test" element={<MockTestPage />} />
          <Route path="practice/mock-test/:sessionId" element={<MockTestPage />} />
          <Route path="practice/results/:attemptId" element={<PracticeResultPage />} />
          <Route path="practice/history" element={<SubmissionHistoryPage />} />
          <Route path="practice/:skill/:setId/attempt" element={<PracticeAttemptPage />} />
          <Route path="practice/:skill/:setId" element={<PracticeDetailPage />} />
          <Route path="practice/search" element={<SearchPage />} />
          <Route path="practice/writing" element={<WritingPage />} />
          <Route path="practice/speaking" element={<SpeakingPage />} />
          <Route path="practice/:skill" element={<PracticePage />} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="saved" element={<SavedPracticesPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="forgot-password" element={<ForgotPasswordPage />} />
          <Route
            path="*"
            element={
              <PlaceholderPage
                title="Page not found."
                description="This route is not part of the current foundation shell."
              />
            }
          />
          </Route>
        </Routes>
      </PreferenceProvider>
    </AuthProvider>
  )
}

export default App
