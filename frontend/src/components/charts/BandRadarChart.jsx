import {
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
} from 'recharts'

function BandRadarChart({ data = [] }) {
  const summary = data
    .map(({ skill, band }) => `${skill}: ${Number.isFinite(band) ? band : 'chưa có dữ liệu'}`)
    .join(' · ')

  return (
    <div className="band-radar-chart" role="img" aria-label="Band ước lượng theo kỹ năng">
      <p className="sr-only">{summary}</p>
      <div className="band-radar-visual" aria-hidden="true">
        <ResponsiveContainer width="100%" height={280}>
          <RadarChart data={data} cx="50%" cy="50%" outerRadius="68%">
            <PolarGrid stroke="rgba(229, 201, 130, 0.18)" />
            <PolarAngleAxis dataKey="skill" tick={{ fill: '#B7C0CF', fontSize: 12 }} />
            <PolarRadiusAxis domain={[0, 9]} tick={false} axisLine={false} />
            <Radar
              name="Band ước lượng"
              dataKey="band"
              stroke="#E5C982"
              fill="#CFAE67"
              fillOpacity={0.28}
            />
          </RadarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

export default BandRadarChart
