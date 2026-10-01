import CommonMistakesPanel from '../learning/CommonMistakesPanel'

function CommonMistakesWidget({ mistakes = [] }) {
  return <CommonMistakesPanel mistakes={mistakes} />
}

export default CommonMistakesWidget
