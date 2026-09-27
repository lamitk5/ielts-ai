import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, test } from 'vitest'
import { MemoryRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../features/auth/AuthProvider'
import RequireAdminRoute from '../features/auth/RequireAdminRoute'

function renderRoute(session, initial = '/admin') {
  localStorage.clear()
  if (session) localStorage.setItem('ielts-ai-tutor.session', JSON.stringify(session))
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[initial]}>
        <Routes>
          <Route path="/admin" element={<RequireAdminRoute><div>ADMIN SHELL</div></RequireAdminRoute>} />
          <Route path="/login" element={<div data-testid="login-page">LOGIN</div>} />
          <Route path="/" element={<div data-testid="home-page">HOME</div>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  )
}

afterEach(() => localStorage.clear())

describe('admin route guard', () => {
  test('guest is redirected to login without rendering privileged shell', () => {
    renderRoute(null)
    expect(screen.getByTestId('login-page')).toBeInTheDocument()
    expect(screen.queryByText('ADMIN SHELL')).not.toBeInTheDocument()
  })

  test('learner is redirected away from admin without rendering privileged shell', () => {
    renderRoute({ token: 'learner', user: { id: 'learner-1', role: 'CUSTOMER' } })
    expect(screen.getByTestId('home-page')).toBeInTheDocument()
    expect(screen.queryByText('ADMIN SHELL')).not.toBeInTheDocument()
  })

  test('admin can reach the existing privileged shell', () => {
    renderRoute({ token: 'admin', user: { id: 'admin-1', role: 'ADMIN' } })
    expect(screen.getByText('ADMIN SHELL')).toBeInTheDocument()
  })
})
