import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from 'vitest'
import LoginPage from '../pages/LoginPage'
import RegisterPage from '../pages/RegisterPage'
import { AuthProvider } from '../features/auth/AuthProvider'
import { PreferenceProvider } from '../features/preferences/PreferenceProvider'
import '../styles/globals.css'

function renderAuth(Page) {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <PreferenceProvider>
          <Page />
        </PreferenceProvider>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('auth form action spacing', () => {
  test.each([
    [LoginPage, 'Chưa có tài khoản?'],
    [RegisterPage, 'Đã có tài khoản?'],
  ])('keeps %s action footer at the approved spacing below the form', (Page, footerText) => {
    renderAuth(Page)

    const footer = screen.getByText(footerText, { exact: false }).closest('.auth-footer-copy')

    expect(footer).not.toBeNull()
    expect(getComputedStyle(footer).marginTop).toBe('2.25rem')
  })
})
