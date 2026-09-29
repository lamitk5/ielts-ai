export const CURSOR_SIZE_MIN = 70
export const CURSOR_SIZE_MAX = 150
export const CURSOR_SIZE_STEP = 5
export const CURSOR_SIZE_DEFAULT = 100

export const CURSOR_COLOR_PRESETS = Object.freeze(['accent', 'champagne', 'ivory', 'sapphire', 'emerald', 'burgundy', 'violet'])

const CURSOR_COLORS = Object.freeze({
  champagne: '#e5c982',
  ivory: '#f5f7fa',
  sapphire: '#9cc4ee',
  emerald: '#8ed9bc',
  burgundy: '#e6a3b4',
  violet: '#c9b2ee',
})

const LEGACY_SIZE_PERCENT = Object.freeze({ small: 80, medium: 100, large: 125 })

function encodeSvg(svg) {
  return `url("data:image/svg+xml,${encodeURIComponent(svg)}")`
}

function hexToRgba(hex, alpha) {
  const normalized = String(hex).replace('#', '')
  const red = Number.parseInt(normalized.slice(0, 2), 16)
  const green = Number.parseInt(normalized.slice(2, 4), 16)
  const blue = Number.parseInt(normalized.slice(4, 6), 16)
  return `rgba(${red}, ${green}, ${blue}, ${alpha})`
}

export function normalizeCursorSizePercent(value) {
  const legacyValue = typeof value === 'string' ? LEGACY_SIZE_PERCENT[value] : undefined
  const numericValue = legacyValue ?? Number(value)
  const safeValue = Number.isFinite(numericValue) ? numericValue : CURSOR_SIZE_DEFAULT
  const clamped = Math.max(CURSOR_SIZE_MIN, Math.min(CURSOR_SIZE_MAX, safeValue))
  return CURSOR_SIZE_MIN + Math.round((clamped - CURSOR_SIZE_MIN) / CURSOR_SIZE_STEP) * CURSOR_SIZE_STEP
}

export function resolveCursorColor(color, accentColor = CURSOR_COLORS.champagne) {
  return color === 'accent' ? accentColor : CURSOR_COLORS[color] ?? accentColor
}

export function getCursorEffectColor(color, alpha = 0.14) {
  return hexToRgba(color, alpha)
}

const outline = '#071426'

const drawArrow = (color) => `<path d="M3 2 21 14 13 16 18 25 14 27 9 18 4 23Z" fill="${color}" stroke="${outline}" stroke-width="1.4" stroke-linejoin="round"/>`
const drawPen = (color) => `<path d="m4 20 3-8 10-10 3 3-10 10Z" fill="${color}" stroke="${outline}" stroke-width="1.2" stroke-linejoin="round"/><path d="m15 5 3 3M4 20l6-2-4-4Z" fill="${outline}"/>`
const drawFeather = (color) => `<path d="M4 25C9 18 10 11 20 3c-2 10-5 17-16 22Z" fill="${color}" stroke="${outline}" stroke-width="1.2"/><path d="M5 23 17 7M8 18l-2-4M11 14 9 10M14 11l-1-4" stroke="${outline}" stroke-width="1" stroke-linecap="round"/>`
const drawPixelScholar = (color) => `<path d="M2 1h3v2h3v2h3v2h3v3h2v4h-4v3h-3v3H6v-4H3v-4H1V4h1Z" fill="${color}" stroke="${outline}" stroke-width="1.2" shape-rendering="crispEdges"/>`

// To add a cursor: add one registry entry, provide its SVG renderer and hotspot,
// add its translated label, then cover it with a cursor settings test.
export const CURSOR_STYLE_REGISTRY = Object.freeze([
  { id: 'default', labelKey: 'cursorDefault', fallbackLabel: 'Mặc định', supportsColor: false, hotspot: [3, 2], render: () => '' },
  { id: 'champagne', labelKey: 'cursorChampagne', fallbackLabel: 'Champagne Gold', supportsColor: true, hotspot: [3, 2], render: drawArrow },
  { id: 'scholar-pen', labelKey: 'cursorScholarPen', fallbackLabel: 'Scholar Pen', supportsColor: true, hotspot: [4, 20], render: drawPen },
  { id: 'en-feather', labelKey: 'cursorEnFeather', fallbackLabel: 'Én Feather', supportsColor: true, hotspot: [4, 24], render: drawFeather },
  { id: 'pixel-scholar', labelKey: 'cursorPixelScholar', fallbackLabel: 'Pixel Scholar', supportsColor: true, hotspot: [2, 1], render: drawPixelScholar },
])

export const CURSOR_STYLE_PRESETS = Object.freeze(CURSOR_STYLE_REGISTRY.map(({ id }) => id))

const assetCache = new Map()

export function createCursorAsset({ style = 'default', sizePercent = CURSOR_SIZE_DEFAULT, size, color = CURSOR_COLORS.champagne } = {}) {
  const definition = CURSOR_STYLE_REGISTRY.find((entry) => entry.id === style)
  if (!definition || style === 'default') return 'auto'

  const percent = normalizeCursorSizePercent(sizePercent ?? size)
  const factor = percent / 100
  const resolvedColor = color || CURSOR_COLORS.champagne
  const cacheKey = `${style}:${percent}:${resolvedColor}`
  if (assetCache.has(cacheKey)) return assetCache.get(cacheKey)

  const width = 24 * factor
  const height = 28 * factor
  const hotspot = definition.hotspot.map((point) => point * factor)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 24 28">${definition.render(resolvedColor)}</svg>`
  const asset = `${encodeSvg(svg)} ${hotspot[0]} ${hotspot[1]}, pointer`
  assetCache.set(cacheKey, asset)
  return asset
}
