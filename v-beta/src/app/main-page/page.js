export default function MainPage() {
  const problems = ["V3 - Overhang Start", "V5 - Slab Balance"];

  return (
    <main style={{ padding: "24px", fontFamily: "sans-serif" }}>
      <h1>Main Page</h1>
      <p>Bouldering Problems:</p>
      <ul>
        {problems.map((problem) => (
          <li key={problem} style={{ marginBottom: "12px" }}>
            {problem}
          </li>
        ))}
      </ul>
    </main>
  );
}
