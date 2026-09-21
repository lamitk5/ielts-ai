import { Route, Routes } from 'react-router-dom'
import Navbar from './components/layout/Navbar'
import Footer from './components/layout/Footer'
import HomePage from './pages/HomePage'
import PlaceholderPage from './pages/PlaceholderPage'

function App() {
  return (
    <div className="app-shell">
      <Navbar />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="*" element={<PlaceholderPage />} />
      </Routes>
      <Footer />
    </div>
  )
}

export default App
