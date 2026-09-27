import { afterEach, describe, expect, test, vi } from 'vitest'

const originalProxyTarget = process.env.VITE_API_PROXY_TARGET

afterEach(() => {
  if (originalProxyTarget === undefined) delete process.env.VITE_API_PROXY_TARGET
  else process.env.VITE_API_PROXY_TARGET = originalProxyTarget
  vi.resetModules()
})

describe('Vite API proxy configuration', () => {
  test('uses the QA backend target from VITE_API_PROXY_TARGET', async () => {
    process.env.VITE_API_PROXY_TARGET = 'http://127.0.0.1:8081'

    const { default: config } = await import('../../vite.config.js')

    expect(config.server.proxy['/api'].target).toBe('http://127.0.0.1:8081')
  })
})
