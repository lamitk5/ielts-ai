import AnimatedSection from '../common/AnimatedSection'
import SectionTitle from '../common/SectionTitle'
import SkillCard from './SkillCard'

function SkillsSection({ skills }) {
  return (
    <AnimatedSection
      id="skills"
      className="skills-section"
      aria-label="Luyện tập theo 4 kỹ năng"
    >
      <div className="skills-inner">
        <SectionTitle
          eyebrow="4 KỸ NĂNG IELTS"
          title="Luyện tập theo 4 kỹ năng"
          description="Một lộ trình thống nhất cho Reading, Listening, Writing và Speaking — luyện tập, nhận phản hồi và theo dõi tiến bộ trên cùng một nền tảng."
        />
        <div className="skills-grid">
          {skills.map((skill, index) => (
            <SkillCard key={skill.id} skill={skill} index={index} />
          ))}
        </div>
      </div>
    </AnimatedSection>
  )
}

export default SkillsSection
