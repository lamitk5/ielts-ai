import { Outlet } from 'react-router-dom'
import Footer from './Footer'
import Navbar from './Navbar'

function AppLayout() {
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">Bỏ qua đến nội dung chính</a>
      <Navbar />
      <main id="main-content" className="page-main" tabIndex="-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}

export default AppLayout
