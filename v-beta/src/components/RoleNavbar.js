"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { onAuthStateChanged, signOut } from "firebase/auth";
import { useEffect, useMemo, useState } from "react";

import { auth } from "@/app/firebase";
import { fetchBackendAccount } from "@/lib/fetch-backend-account";

function normalizeRole(roleName) {
  const normalized = (roleName || "").toUpperCase();
  if (normalized.includes("ADMIN")) return "admin";
  if (normalized.includes("CLIMBER") || normalized.includes("SETTER")) return "climberSetter";
  return "climberSetter";
}

/** Navbar is hidden here; do not GET /api/account until user leaves (avoids racing signup POST). */
function isAuthShellPath(pathname) {
  return pathname === "/" || pathname === "/login" || pathname === "/signup";
}

export default function RoleNavbar() {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState(null);
  const [roleType, setRoleType] = useState("guest");

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      if (!currentUser) {
        setRoleType("guest");
      }
    });
    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (!user) {
      return;
    }
    const onAuthShell = isAuthShellPath(pathname);
    if (onAuthShell) {
      return;
    }

    let cancelled = false;

    (async () => {
      try {
        const idToken = await user.getIdToken(true);
        const data = await fetchBackendAccount(idToken);
        if (cancelled) return;
        const raw = typeof data?.role === "string" ? data.role : "";
        setRoleType(normalizeRole(raw));
      } catch (error) {
        console.error("Failed to resolve user role:", error);
        if (!cancelled) {
          setRoleType("climberSetter");
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [user, pathname]);

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
      setRoleType("guest");
      router.push("/login");
    } catch (error) {
      console.error("Logout failed:", error);
    }
  };

  if (isAuthShellPath(pathname)) {
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
