"use client";

import { API_BASE_URL } from "@/app/envExports";

const STORAGE_KEY = "accountSession";

/**
 * @typedef {Object} AccountSession
 * @property {number | null} id
 * @property {string} username
 * @property {string} roleName
 * @property {string} firebaseUid
 */

/**
 * @param {unknown} account
 * @returns {number | null}
 */
export function getAccountId(account) {
  if (!account || typeof account !== "object") return null;
  const value = /** @type {Record<string, unknown>} */ (account);
  const parsed = Number(value.userId ?? value.id);
  return Number.isFinite(parsed) ? parsed : null;
}

/**
 * @param {unknown} account
 * @returns {string}
 */
export function getAccountRole(account) {
  if (!account || typeof account !== "object") return "";
  const value = /** @type {Record<string, unknown>} */ (account);
  const role = value.role ?? value.roleName;
  return typeof role === "string" ? role : "";
}

/**
 * @param {unknown} raw
 * @param {string} [fallbackUid]
 * @returns {AccountSession | null}
 */
function normalizeAccountSession(raw, fallbackUid = "") {
  if (!raw || typeof raw !== "object") return null;
  const value = /** @type {Record<string, unknown>} */ (raw);
  const id = getAccountId(value);
  const username = typeof value.username === "string" ? value.username : "";
  const roleName = getAccountRole(value);
  const firebaseUid =
    typeof value.firebaseUid === "string" && value.firebaseUid
      ? value.firebaseUid
      : fallbackUid;
  return { id, username, roleName, firebaseUid };
}

/**
 * @param {AccountSession} account
 */
export function storeAccountSession(account) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(account));
}

export function clearStoredAccountSession() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(STORAGE_KEY);
}

/**
 * @returns {AccountSession | null}
 */
export function getStoredAccountSession() {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    return normalizeAccountSession(parsed);
  } catch {
    return null;
  }
}

/**
 * @param {import("firebase/auth").User} currentUser
 * @param {{ username?: string }} [options]
 * @returns {Promise<AccountSession>}
 */
export async function syncAccountSessionWithBackend(currentUser, options = {}) {
  const explicit = options.username;
  const username =
    typeof explicit === "string" && explicit.trim() !== ""
      ? explicit.trim()
      : currentUser.displayName?.trim() ||
        currentUser.email?.split("@")[0] ||
        "user";

  const idToken = await currentUser.getIdToken();
  let response;
  try {
    response = await fetch(`${API_BASE_URL}/api/accounts/session`, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        Authorization: `Bearer ${idToken}`,
      },
      body: JSON.stringify({
        username,
        email: currentUser.email || "",
      }),
    });
  } catch (err) {
    const message = err instanceof Error ? err.message : "Failed to reach backend.";
    throw new Error(`Backend session sync failed: ${message}`);
  }

  if (!response.ok) {
    let detail = "";
    try {
      const text = await response.text();
      detail = text ? `: ${text.slice(0, 200)}` : "";
    } catch {
      // ignore
    }
    throw new Error(`Backend session failed (${response.status} ${response.statusText})${detail}`);
  }

  const raw = await response.json();
  const account = normalizeAccountSession(raw, currentUser.uid);
  if (!account) {
    throw new Error("Invalid backend account session payload");
  }
  storeAccountSession(account);
  return account;
}
