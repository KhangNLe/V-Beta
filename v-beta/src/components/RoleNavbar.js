"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { useMemo } from "react";

import { auth } from "@/app/firebase";
import { useRequireAuth } from "@/hooks/useRequireAuth";

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

export default function RoleNavbar() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, account, ready } = useRequireAuth({
    skip: isAuthShellPath(pathname),
  });

  const roleType = useMemo(() => {
    if (!user || !ready) return "guest";
    return normalizeRole(account?.roleName);
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
        </ul>
      </div>
    </nav>
  );
}
