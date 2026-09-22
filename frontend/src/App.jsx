import { Route, Routes } from 'react-router-dom'
import AppLayout from './components/layout/AppLayout'
import HomePage from './pages/HomePage'
import PlaceholderPage from './pages/PlaceholderPage'
import AdminRagPage from './pages/AdminRagPage'
import LoginPage from './pages/LoginPage'
import { AuthProvider } from './features/auth/AuthProvider'
import PracticePage from './pages/PracticePage'

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="admin/rag" element={<AdminRagPage />} />
        <Route
          path="assessment"
          element={
            <PlaceholderPage
              title="Assessment is reserved for a later task."
              description="The assessment entry point is ready without implementing assessment logic yet."
            />
          }
        />
        <Route path="practice/search" element={<PlaceholderPage />} />
        <Route path="practice/:skill" element={<PracticePage />} />
        <Route path="login" element={<LoginPage />} />
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
    </AuthProvider>
  )
}

export default App
