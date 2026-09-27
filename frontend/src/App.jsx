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

function App() {
  return (
    <AuthProvider>
      <PreferenceProvider>
        <Routes>
          <Route element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route path="admin/rag" element={<RequireAdminRoute allowLegacyToken={Boolean(readAdminToken())}><AdminRagPage /></RequireAdminRoute>} />
          <Route path="admin/practice-generator" element={<RequireAdminRoute><AdminPracticeGeneratorPage /></RequireAdminRoute>} />
          <Route path="admin/practice-generator/sets/:setId" element={<RequireAdminRoute><AdminPracticeReviewPage /></RequireAdminRoute>} />
          <Route path="assessment" element={<AssessmentPage />} />
          <Route path="practice/search" element={<SearchPage />} />
          <Route path="practice/writing" element={<WritingPage />} />
          <Route path="practice/speaking" element={<SpeakingPage />} />
          <Route path="practice/:skill" element={<PracticePage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
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
