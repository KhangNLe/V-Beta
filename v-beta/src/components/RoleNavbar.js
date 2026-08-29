"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { useEffect, useMemo, useState } from "react";

import { auth } from "@/app/firebase";
import NotificationBell from "@/components/NotificationBell";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { getAccountRole } from "@/lib/accountSession";

function normalizeRole(roleName) {
  const normalized = (roleName || "").toUpperCase();
  if (normalized.includes("ADMIN")) return "admin";
  if (normalized.includes("CLIMBER") || normalized.includes("SETTER")) return "climberSetter";
  return "climberSetter";
}

/** Navbar is hidden here; do not GET /api/account until user leaves (avoids racing signup POST). */
function isAuthShellPath(pathname) {
  return (
    pathname === "/" ||
    pathname === "/login" ||
    pathname === "/signup" ||
    pathname === "/forgot-password" ||
    pathname === "/verify-email"
  );
}

function isGuestAllowedPath(pathname) {
  if (pathname === "/main-page") return true;
  return /^\/wall\/[^/]+(?:\/problem\/[^/]+)?$/.test(pathname);
}

export default function RoleNavbar() {
  const pathname = usePathname();
  const router = useRouter();
  const [theme, setTheme] = useState("light");
  const { user, account, ready } = useRequireAuth({
    skip: isAuthShellPath(pathname),
    allowGuest: isGuestAllowedPath(pathname),
  });

  useEffect(() => {
    const storedTheme = window.localStorage.getItem("theme");
    const nextTheme =
      storedTheme === "light" || storedTheme === "dark"
        ? storedTheme
        : window.matchMedia("(prefers-color-scheme: dark)").matches
          ? "dark"
          : "light";

    setTheme(nextTheme);
    document.documentElement.setAttribute("data-theme", nextTheme);
    document.documentElement.style.colorScheme = nextTheme;
  }, []);

  const roleType = useMemo(() => {
    if (!user || !ready) return "guest";
    return normalizeRole(getAccountRole(account));
  }, [user, account, ready]);

  const navItems = useMemo(() => {
    if (!user || roleType === "guest") {
      return [
        { href: "/login", label: "Login" },
        { href: "/signup", label: "Sign Up" },
        { href: "/main-page", label: "Gym" },
      ];
    }

    if (roleType === "admin") {
      return [
        { href: "/accounts", label: "All Accounts" },
        { href: "/reports", label: "Reports" },
        { href: "/logbook", label: "Logbook" },
        { href: "/account", label: "Account" },
        { href: "/main-page", label: "Gym" },
      ];
    }

    return [
      { href: "/account", label: "Account" },
      { href: "/main-page", label: "Gym" },
    ];
  }, [roleType, user]);

  const handleLogout = async () => {
    try {
      await signOut(auth);
      router.push("/");
    } catch (error) {
      console.error("Logout failed:", error);
    }
  };

  const handleThemeToggle = () => {
    const nextTheme = theme === "dark" ? "light" : "dark";
    setTheme(nextTheme);
    window.localStorage.setItem("theme", nextTheme);
    document.documentElement.setAttribute("data-theme", nextTheme);
    document.documentElement.style.colorScheme = nextTheme;
  };

  if (!ready || isAuthShellPath(pathname)) {
    return null;
  }

  return (
    <nav className="role-navbar" aria-label="Primary navigation">
      <div className="role-navbar__inner">
        <span className="role-navbar__link role-navbar__link--disabled">V-Beta</span>

        <ul className="role-navbar__links role-navbar__links--right">
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            return (
              <li key={item.href + item.label}>
                <Link
                  href={item.href}
                  className={`role-navbar__link ${isActive ? "role-navbar__link--active" : ""}`}
                >
                  {item.label}
                </Link>
              </li>
            );
          })}
          {user && roleType !== "guest" && (
            <li>
              <NotificationBell user={user} />
            </li>
          )}
          {user && roleType !== "guest" && (
            <li>
              <span
                className="role-navbar__link role-navbar__link--action"
                role="button"
                tabIndex={0}
                onClick={handleLogout}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    handleLogout();
                  }
                }}
              >
                Logout
              </span>
            </li>
          )}
          <li>
            <button
              type="button"
              className="role-navbar__theme-toggle"
              onClick={handleThemeToggle}
              aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} mode`}
              aria-pressed={theme === "dark"}
            >
              {theme === "dark" ? (
                <svg
                  className="role-navbar__theme-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <circle cx="12" cy="12" r="4" stroke="currentColor" strokeWidth="2" />
                  <path
                    d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                  />
                </svg>
              ) : (
                <svg
                  className="role-navbar__theme-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  xmlns="http://www.w3.org/2000/svg"
                  aria-hidden="true"
                >
                  <path
                    d="M21 12.79A9 9 0 1 1 11.21 3c-.13.6-.21 1.22-.21 1.86a9 9 0 0 0 10 8.93Z"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
              )}
            </button>
          </li>
        </ul>
      </div>
    </nav>
  );
}
