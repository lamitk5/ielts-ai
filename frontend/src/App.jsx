import { Route, Routes } from 'react-router-dom'
import AppLayout from './components/layout/AppLayout'
import HomePage from './pages/HomePage'
import PlaceholderPage from './pages/PlaceholderPage'

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
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
        <Route path="practice/:skill" element={<PlaceholderPage />} />
        <Route
          path="login"
          element={
            <PlaceholderPage
              title="Sign in is reserved for a later task."
              description="Authentication is intentionally out of scope for this frontend shell."
            />
          }
        />
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
  )
}

export default App
