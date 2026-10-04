import { createContext, useContext } from 'react'

export const CrossHighlightContext = createContext(null)

export function useCrossHighlight() {
  return useContext(CrossHighlightContext)
}
