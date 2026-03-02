import { useEffect, useRef, useState } from 'react';
import './LazyImage.css';

interface LazyImageProps {
  src: string;
  alt: string;
  className?: string;
}

const LazyImage = ({ src, alt, className }: LazyImageProps) => {
  const [isInView, setIsInView] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);
  const [hasError, setHasError] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = wrapperRef.current;
    if (!element) return;

    const observer = new IntersectionObserver(
      entries => {
        if (entries[0].isIntersecting) {
          setIsInView(true);
          observer.disconnect();
        }
      },
      { rootMargin: '120px 0px' }
    );

    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  return (
    <div ref={wrapperRef} className={`lazy-image ${className || ''}`}>
      {!isLoaded && !hasError && <div className="lazy-image__skeleton" />}
      {isInView && !hasError && (
        <img
          src={src}
          alt={alt}
          className={`lazy-image__img ${isLoaded ? 'lazy-image__img--loaded' : ''}`}
          onLoad={() => setIsLoaded(true)}
          onError={() => setHasError(true)}
          loading="lazy"
        />
      )}
      {hasError && <div className="lazy-image__error">图片加载失败</div>}
    </div>
  );
};

export default LazyImage;
