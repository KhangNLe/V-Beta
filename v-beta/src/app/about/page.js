import Link from 'next/link';
import LandingFooter from '@/components/LandingFooter';
import styles from '../page.module.css';

export const metadata = {
  title: 'About | V-Beta',
  description:
    'V-Beta is a web app for indoor climbers to find gym problems, share beta, and keep that knowledge tied to a wall even after it is reset.',
};

export default function AboutPage() {
  return (
    <div className={styles.page}>
      <main className={styles.landing}>
        <section className={styles.aboutHeader}>
          <span className={styles.eyebrow}>About</span>
          <h1>V-Beta</h1>
          <p>
            A web app for indoor climbers to find gym problems, share beta, and
            keep that knowledge tied to a wall even after it is reset.
          </p>
        </section>

        <section className={styles.aboutSections}>
          <article>
            <h2>Why it exists</h2>
            <p>
              Climbers usually swap beta in person or in disappearing videos.
              V-Beta stores comments and solution videos on the problem, with
              grades, wall sections, and a moderation loop for the gym. The
              knowledge stays with the wall, not in a group chat that gets
              buried after the next reset.
            </p>
          </article>

          <article>
            <h2>Built for the Co-op</h2>
            <p>
              V-Beta started as a full-stack capstone for community use at{' '}
              <a
                href="https://www.mnclimbingcoop.com/"
                target="_blank"
                rel="noopener noreferrer"
              >
                Minnesota Climbing Cooperative
              </a>
              , the volunteer-run bouldering gym in the Thorp Building in NE
              Minneapolis. Guests can browse. Signed-in climbers contribute
              beta. Admins keep the space in shape.
            </p>
          </article>

          <article>
            <h2>Who it&apos;s for</h2>
            <div className={styles.aboutRoles}>
              <div className={styles.aboutRole}>
                <h3>Guest</h3>
                <p>Browse walls and problems, and filter or sort by grade.</p>
              </div>
              <div className={styles.aboutRole}>
                <h3>Climber</h3>
                <p>
                  Comment, upload a beta video, suggest a grade, report content,
                  and appeal a removal once.
                </p>
              </div>
              <div className={styles.aboutRole}>
                <h3>Setter</h3>
                <p>Create and manage problems on a wall section.</p>
              </div>
              <div className={styles.aboutRole}>
                <h3>Admin</h3>
                <p>
                  Manage walls and roles, review reports, keep the logbook, and
                  decide appeals.
                </p>
              </div>
            </div>
          </article>

          <article>
            <h2>Contributors</h2>
            <p>
              V-Beta is a personal project, currently maintained only by Khang.
              If you&apos;re interested in helping, the source is on{' '}
              <a
                href="https://github.com/KhangNLe/V-Beta"
                target="_blank"
                rel="noopener noreferrer"
              >
                GitHub
              </a>
              .
            </p>
          </article>
        </section>

        <div className={styles.aboutActions}>
          <Link href="/main-page" className={styles.primary}>
            Browse Problems
          </Link>
          <Link href="/signup" className={styles.secondary}>
            Sign Up
          </Link>
          <Link href="/" className={styles.secondary}>
            Back to Home
          </Link>
        </div>

        <LandingFooter />
      </main>
    </div>
  );
}
