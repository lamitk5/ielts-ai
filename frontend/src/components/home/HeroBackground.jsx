import libraryPhoto from '../../assets/home-slideshow/library.jpg'

function HeroBackground() {
  return (
    <div
      className="hero-bookshelf-background"
      data-testid="hero-bookshelf-background"
      aria-hidden="true"
      style={{ backgroundImage: `url(${libraryPhoto})` }}
    />
  )
}

export default HeroBackground
