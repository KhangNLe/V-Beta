'use client';

import { useEffect } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import LandingFooter from '@/components/LandingFooter';
import styles from './page.module.css';

const climbingLocations = [
  'Bouldering Project - Minneapolis',
  'Minnesota Climbing Cooperative',
  'Vertical Endeavors - Minneapolis'
];

const sliderPhotos = [
  {
    src: '/landing/0U5A1348.jpg',
    alt: 'Boulderer climbing beneath an overhanging rock with crash pads below',
  },
  {
    src: '/landing/0U5A0798.jpg',
    alt: 'Two climbers on a sunlit limestone wall',
  },
  {
    src: '/landing/0U5A2389.jpg',
    alt: 'Climber topping out on a weathered stone wall',
  },
  {
    src: '/landing/0U5A2558.jpg',
    alt: 'Climber on a wall beside the river while friends float nearby',
  },
];
export default function LandingPage() {
  useEffect(() => {
    const revealedElements = document.querySelectorAll('[data-reveal]');

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;

          entry.target.classList.add(styles.visible);
          observer.unobserve(entry.target);
        });
      },
      {
        threshold: 0.16,
        rootMargin: '0px 0px -8% 0px',
      },
    );

    revealedElements.forEach((element) => observer.observe(element));

    return () => observer.disconnect();
  }, []);

  return (
    <div className={styles.page}>
      <main className={styles.landing}>
        <section
          className={`${styles.hero} ${styles.reveal} ${styles.visible}`}
        >
          <div className={styles.heroContent}>
            <span className={styles.eyebrow}>Climb smarter together</span>
            <h1>V-Beta</h1>
            <p>
              Discover problems, share beta, and learn from a growing community
              of climbers.
            </p>
            <div className={styles.heroActions}>
              <Link href="/login" className={styles.primary}>
                Login
              </Link>
              <Link href="/signup" className={styles.secondary}>
                Sign Up
              </Link>
            </div>
          </div>

          <div className={styles.heroVisual}>
            <div className={styles.heroOverlay}>
              <span>Climb Smarter</span>
              <small>Track progress. Share beta. Learn faster.</small>
            </div>
          </div>
        </section>

        <section className={`${styles.logoStrip} ${styles.reveal}`} data-reveal>
          <p>Climbing Locations</p>
          <div className={styles.logoViewport}>
            <div className={styles.logoTrack}>
              {[...climbingLocations, ...climbingLocations].map(
                (location, index) => (
                  <span
                    key={`${location}-${index}`}
                    className={styles.logoCard}
                  >
                    {location}
                  </span>
                ),
              )}
            </div>
          </div>
        </section>

        <section className={styles.featureGrid}>
          <article
            className={`${styles.featureCopy} ${styles.reveal}`}
            data-reveal
          >
            <h2>Find Your Next Project</h2>
            <p>
              Browse climbing problems by grade, wall section, and hold color to
              find routes that match your level.
            </p>
            <Link href="/main-page" className={styles.primary}>
              Browse Problems
            </Link>
          </article>
          <div
            className={`${styles.featureImage} ${styles.reveal}`}
            data-reveal
          >
            <Image
              src="/landing/20250913_122026.jpg"
              alt="Climber scaling a riverside stone wall"
              fill
              sizes="(max-width: 900px) 100vw, 544px"
              className={styles.featureImagePhoto}
            />
          </div>

          <div
            className={`${styles.featureImage} ${styles.reveal}`}
            data-reveal
          >
            <Image
              src="/landing/0U5A2327.jpg"
              alt="Climber on a tall stone wall above the water"
              fill
              sizes="(max-width: 900px) 100vw, 544px"
              className={styles.featureImagePhoto}
            />
          </div>
          <article
            className={`${styles.featureCopy} ${styles.reveal}`}
            data-reveal
          >
            <h2>Share Beta, Get Better</h2>
            <p>
              Upload videos of your climbs, learn new techniques, and help other
              climbers solve problems faster.
            </p>
            <Link href="/signup" className={styles.primary}>
              Upload Beta
            </Link>
          </article>
        </section>

        <section className={styles.photoGallery}>
          <div
            className={`${styles.galleryIntro} ${styles.reveal}`}
            data-reveal
          >
            <span className={styles.galleryEyebrow}>Community Moments</span>
            <h2>Memorable Climbing Experiences</h2>
            <p>
              V-Beta is built around the sessions that make climbing memorable.
            </p>
          </div>

          <div
            className={`${styles.photoSliderViewport} ${styles.reveal}`}
            data-reveal
          >
            <div className={styles.photoSliderTrack}>
              {[...sliderPhotos, ...sliderPhotos].map((photo, index) => (
                <figure
                  key={`${photo.src}-${index}`}
                  className={styles.photoSlide}
                >
                  <Image
                    src={photo.src}
                    alt={photo.alt}
                    fill
                    sizes="(max-width: 900px) 70vw, 320px"
                    className={styles.photoSlideImage}
                  />
                </figure>
              ))}
            </div>
          </div>
        </section>

        <section className={styles.benefits}>
          <article className={styles.reveal} data-reveal>
            <h3>Track Progress</h3>
            <p>
              Keep a record of problems you&apos;ve completed and monitor your
              climbing growth.
            </p>
          </article>
          <article className={styles.reveal} data-reveal>
            <h3>Join Discussions</h3>
            <p>
              Comment on problems, share tips, and learn from other climbers.
            </p>
          </article>
          <article className={styles.reveal} data-reveal>
            <h3>Role-Based Access</h3>
            <p>
              Admins manage gyms and problems while users contribute beta and
              feedback.
            </p>
          </article>
        </section>

        <LandingFooter reveal />
      </main>
    </div>
  );
}
