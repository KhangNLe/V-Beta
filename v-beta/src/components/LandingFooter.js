import Image from 'next/image';
import Link from 'next/link';
import styles from '@/app/page.module.css';

export default function LandingFooter({ reveal = false }) {
  return (
    <>
      <footer
        className={`${styles.footer}${reveal ? ` ${styles.reveal}` : ''}`}
        {...(reveal ? { 'data-reveal': true } : {})}
      >
        <div className={styles.footerLeft}>
          <Link href="/" className={styles.footerBrand}>
            <Image
              src="/VBetaLogo.svg"
              alt="V-Beta logo"
              width={36}
              height={22}
              className={styles.footerBrandLogo}
            />
            <span>V-Beta</span>
          </Link>
          <nav className={styles.footerNav}>
            <Link href="/about">About</Link>
            <Link
              href="https://github.com/KhangNLe/V-Beta"
              target="_blank"
              rel="noopener noreferrer"
            >
              GitHub
            </Link>
          </nav>
        </div>
      </footer>
    </>
  );
}
