const SIZE_FACTORS = Object.freeze({ small: 0.8, medium: 1, large: 1.25 })

export const CURSOR_SIZE_PRESETS = Object.freeze(['small', 'medium', 'large'])
export const CURSOR_COLOR_PRESETS = Object.freeze(['accent', 'champagne', 'ivory', 'sapphire', 'emerald', 'burgundy', 'violet'])

const CURSOR_COLORS = Object.freeze({
  champagne: '#e5c982',
  ivory: '#f5f7fa',
  sapphire: '#9cc4ee',
  emerald: '#8ed9bc',
  burgundy: '#e6a3b4',
  violet: '#c9b2ee',
})

const assetCache = new Map()

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

export function resolveCursorColor(color, accentColor = CURSOR_COLORS.champagne) {
  return color === 'accent' ? accentColor : CURSOR_COLORS[color] ?? accentColor
}

export function getCursorEffectColor(color, alpha = 0.14) {
  return hexToRgba(color, alpha)
}

function cursorDrawing(style, color) {
  const outline = '#071426'
  if (style === 'scholar-pen') {
    return `<path d="m4 20 3-8 10-10 3 3-10 10Z" fill="${color}" stroke="${outline}" stroke-width="1.2" stroke-linejoin="round"/><path d="m15 5 3 3M4 20l6-2-4-4Z" fill="${outline}"/>`
  }
  if (style === 'en-feather') {
    return `<path d="M4 25C9 18 10 11 20 3c-2 10-5 17-16 22Z" fill="${color}" stroke="${outline}" stroke-width="1.2"/><path d="M5 23 17 7M8 18l-2-4M11 14 9 10M14 11l-1-4" stroke="${outline}" stroke-width="1" stroke-linecap="round"/>`
  }
  if (style === 'pixel-scholar') {
    return `<path d="M2 1h3v2h3v2h3v2h3v3h2v4h-4v3h-3v3H6v-4H3v-4H1V4h1Z" fill="${color}" stroke="${outline}" stroke-width="1.2" shape-rendering="crispEdges"/>`
  }
  return `<path d="M3 2 21 14 13 16 18 25 14 27 9 18 4 23Z" fill="${color}" stroke="${outline}" stroke-width="1.4" stroke-linejoin="round"/>`
}

export function createCursorAsset({ style = 'default', size = 'medium', color = CURSOR_COLORS.champagne } = {}) {
  if (style === 'default') return 'auto'
  const factor = SIZE_FACTORS[size] ?? SIZE_FACTORS.medium
  const resolvedColor = color || CURSOR_COLORS.champagne
  const cacheKey = `${style}:${size}:${resolvedColor}`
  if (assetCache.has(cacheKey)) return assetCache.get(cacheKey)

  const width = 24 * factor
  const height = 28 * factor
  const hotspot = style === 'scholar-pen'
    ? [4 * factor, 20 * factor]
    : style === 'en-feather'
      ? [4 * factor, 24 * factor]
      : style === 'pixel-scholar'
        ? [2 * factor, 1 * factor]
        : [3 * factor, 2 * factor]
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 24 28">${cursorDrawing(style, resolvedColor)}</svg>`
  const asset = `${encodeSvg(svg)} ${hotspot[0]} ${hotspot[1]}, pointer`
  assetCache.set(cacheKey, asset)
  return asset
}
