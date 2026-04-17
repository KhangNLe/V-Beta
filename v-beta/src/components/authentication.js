"use client";

// This component handles user authentication using Firebase Authentication.
// It allows users to sign up, log in, and log out using their email and password.
// The component listens for authentication state changes and updates the UI accordingly.

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import {
  signInWithEmailAndPassword,
  onAuthStateChanged,
  signOut,
  signInWithPopup,
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
} from "firebase/auth";
import { auth } from "../app/firebase";
import {
  clearStoredAccountSession,
  syncAccountSessionWithBackend,
} from "@/lib/accountSession";
import { toast } from "react-toastify";

export default function Authentication() {
  const router = useRouter();
  const [user, setUser] = useState(null);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const googleProvider = new GoogleAuthProvider();

  // Listen for authentication state changes and update the user state accordingly
  useEffect(() => {
    // Subscribe to authentication state changes
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);

      // Redirect to the main page if the user is authenticated
      if (currentUser) {
        router.push("/main-page");
      }
    });

    return () => unsubscribe(); // Clean up the subscription on unmount
  }, []);

  // Function to synchronize the Firebase Authentication session with the backend session.
  const syncSessionWithBackend = async () => {
    const currentUser = auth.currentUser; // Get the current user from Firebase Authentication
    if (!currentUser) return; // If there is no user, exit the function

    return syncAccountSessionWithBackend(currentUser);
  };

  // Handle user sign-up using email and password using Firebase Authentication
  const handleSignup = async () => {
    try {
      await createUserWithEmailAndPassword(auth, email, password);
      await syncSessionWithBackend(); // Sync the session with the backend after successful sign-up
    } catch (err) {
      toast.error(err.message);
    }
  };

  // Handle user sign-in using email and password using Firebase Authentication
  const handleSignIn = async () => {
    try {
      await signInWithEmailAndPassword(auth, email, password);
      await syncSessionWithBackend(); // Sync the session with the backend after successful sign-in
    } catch (error) {
      toast.error(error.message);
    }
  };

  // Handle user sign-in using Google provider with Firebase Authentication
  const handleGoogleSignIn = async () => {
    try {
      await signInWithPopup(auth, googleProvider);
      await syncSessionWithBackend(); // Sync the session with the backend after successful Google sign-in
    } catch (error) {
      toast.error(error.message);
    }
  };

  // Handle user sign-out using Firebase Authentication
  const handleSignOut = async () => {
    try {
      await signOut(auth);
      clearStoredAccountSession();
      router.push("/login");
    } catch (error) {
      toast.error(error.message);
    }
  };

  return (
    <div>
      <h2>Authentication</h2>

      {user ? (
        <div>
          <p>Logged in as: {user.email}</p>
          <button onClick={handleSignOut}>Log out</button>
        </div>
      ) : (
        <div>
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <button onClick={handleSignIn}>Log in</button>
          <button onClick={handleSignup}>Sign up</button>
          <button onClick={handleGoogleSignIn}>Sign in with Google</button>
        </div>
      )}
    </div>
  );
}
