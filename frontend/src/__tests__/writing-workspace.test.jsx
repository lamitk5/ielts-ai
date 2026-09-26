import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, test, vi } from 'vitest'
import App from '../App'

const renderWriting = () => render(
  <MemoryRouter initialEntries={['/practice/writing']}>
    <App />
  </MemoryRouter>,
)

afterEach(() => {
  vi.unstubAllGlobals()
  window.localStorage.clear()
})

describe('Writing learning workspace', () => {
  test('renders prompt on the left and editor on the right with split workspace regions', () => {
    renderWriting()

    const promptRegion = screen.getByRole('region', { name: 'Đề bài' })
    const editorRegion = screen.getByRole('region', { name: 'Bài viết' })
    expect(promptRegion.compareDocumentPosition(editorRegion) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()

    expect(within(promptRegion).getByRole('combobox', { name: /chọn dạng bài/i })).toBeInTheDocument()
    expect(within(promptRegion).getByRole('heading', { name: /Summarise the information/i })).toBeInTheDocument()
    expect(within(editorRegion).getByRole('textbox', { name: 'Bài viết' })).toBeInTheDocument()
    expect(within(editorRegion).getByText(/Band ước lượng/i)).toBeInTheDocument()
  })

  test('switches Task 1 and Task 2 and updates minimum word guidance', async () => {
    const user = userEvent.setup()
    renderWriting()

    const promptRegion = screen.getByRole('region', { name: 'Đề bài' })
    const taskSelect = within(promptRegion).getByRole('combobox', { name: /chọn dạng bài/i })

    expect(within(promptRegion).getByText(/tối thiểu 150 từ/i)).toBeInTheDocument()

    await user.selectOptions(taskSelect, 'task-2-opinion-01')

    expect(within(promptRegion).getByRole('heading', { name: /Discuss both views and give your own opinion/i })).toBeInTheDocument()
    expect(within(promptRegion).getByText(/tối thiểu 250 từ/i)).toBeInTheDocument()
  })

  test('calculates live word count and displays truthful status', async () => {
    const user = userEvent.setup()
    renderWriting()

    const editorRegion = screen.getByRole('region', { name: 'Bài viết' })
    const textarea = within(editorRegion).getByRole('textbox', { name: 'Bài viết' })

    expect(within(editorRegion).getByText(/0.*từ/i)).toBeInTheDocument()
    expect(within(editorRegion).getByText(/Bản nháp cục bộ|Chưa lưu/i)).toBeInTheDocument()

    await user.type(textarea, 'The chart illustrates the consumption of renewable energy.')
    expect(within(editorRegion).getByText(/8.*từ/i)).toBeInTheDocument()
  })

  test('provides an optional practice timer aid without official exam timer claims', async () => {
    const user = userEvent.setup()
    renderWriting()

    const editorRegion = screen.getByRole('region', { name: 'Bài viết' })
    const timerToggle = within(editorRegion).getByRole('button', { name: /đồng hồ bấm giờ|bật đếm giờ|bắt đầu đếm giờ|thời gian/i })
    expect(timerToggle).toBeInTheDocument()

    await user.click(timerToggle)
    expect(within(editorRegion).getByText(/\d{2}:\d{2}/)).toBeInTheDocument()
  })

  test('preserves typed essay text across mobile tab switching', async () => {
    vi.stubGlobal('matchMedia', vi.fn((query) => ({
      matches: query.includes('max-width: 1023px'),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
    })))
    const user = userEvent.setup()
    renderWriting()

    const editorTab = screen.getByRole('tab', { name: 'Bài viết' })
    await user.click(editorTab)

    const textarea = screen.getByRole('textbox', { name: 'Bài viết' })
    await user.type(textarea, 'Essay paragraph for IELTS Academic Task 1.')

    const promptTab = screen.getByRole('tab', { name: 'Đề bài' })
    await user.click(promptTab)
    expect(screen.getByRole('heading', { name: /Summarise the information/i })).toBeInTheDocument()

    await user.click(editorTab)
    expect(screen.getByRole('textbox', { name: 'Bài viết' })).toHaveValue('Essay paragraph for IELTS Academic Task 1.')
  })

  test('submits authenticated writing response and renders estimated band result', async () => {
    localStorage.setItem('ielts-ai-tutor.session', JSON.stringify({
      token: 'valid-token',
      user: { id: 'u1', email: 'user@example.com' },
    }))

    vi.stubGlobal('fetch', vi.fn().mockImplementation((url, options) => {
      if (url === '/api/practice/writing/submissions') {
        if (options?.method === 'POST') {
          return Promise.resolve({
            ok: true,
            json: async () => ({
              taskId: 'task-1-academic-01',
              overallBandEstimate: 6.5,
              taskAchievementEstimate: 6.5,
              coherenceCohesionEstimate: 6.0,
              lexicalResourceEstimate: 7.0,
              grammaticalAccuracyEstimate: 6.5,
              disclaimer: 'Điểm số do AI ước lượng nhằm mục đích học tập, không phải kết quả thi chính thức.',
            }),
          })
        }
        return Promise.resolve({ ok: true, json: async () => [] })
      }
      return Promise.reject(new Error(`Unhandled request: ${url}`))
    }))

    const user = userEvent.setup()
    renderWriting()

    const textarea = screen.getByRole('textbox', { name: 'Bài viết' })
    const essay = 'The chart illustrates the changes in renewable energy production across five European countries between 2010 and 2020. Overall, substantial growth was observed across all measured nations.'
    await user.type(textarea, essay)

    const submitBtn = screen.getByRole('button', { name: /gửi bài viết/i })
    await user.click(submitBtn)

    expect(await screen.findByText(/Band ước lượng 6.5/i)).toBeInTheDocument()
    expect(screen.getByText(/không phải kết quả thi chính thức/i)).toBeInTheDocument()
  })

  test('displays friendly error when guest learner submits without login', async () => {
    const user = userEvent.setup()
    renderWriting()

    const textarea = screen.getByRole('textbox', { name: 'Bài viết' })
    await user.type(textarea, 'Twenty words or more to bypass the minimum client validation threshold before submitting the essay.')

    const submitBtn = screen.getByRole('button', { name: /gửi bài viết/i })
    await user.click(submitBtn)

    expect(screen.getByRole('alert')).toHaveTextContent(/Đăng nhập để lưu và nhận đánh giá bài viết/i)
  })
})
