import styles from "./page.module.css";
import Authentication from "../components/authentication";

export default function Home() {
  return (
    <div className={styles.page}>
      <main className={styles.main}>
        {/* Firebase Authentication */}
        <Authentication />
      </main>
    </div>
  );
}
