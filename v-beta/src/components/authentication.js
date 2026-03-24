"use client";

// This component handles user authentication using Firebase Authentication.
// It allows users to sign up, log in, and log out using their email and password.
// The component listens for authentication state changes and updates the UI accordingly.

import { useState, useEffect } from "react";
import {
  signInWithEmailAndPassword,
  onAuthStateChanged,
  signOut,
  signInWithPopup,
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
} from "firebase/auth";
import { auth } from "../app/firebase";

export default function Authentication() {
  const [user, setUser] = useState(null);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const googleProvider = new GoogleAuthProvider();

  // Listen for authentication state changes and update the user state accordingly
  useEffect(() => {
    // Subscribe to authentication state changes
    const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
    });

    return () => unsubscribe(); // Clean up the subscription on unmount
  }, []);

  // Handle user sign-up using email and password using Firebase Authentication
  const handleSignup = async () => {
    try {
      setError("");
      await createUserWithEmailAndPassword(auth, email, password);
    } catch (err) {
      setError(err.message);
    }
  };

  // Handle user sign-in using email and password using Firebase Authentication
  const handleSignIn = async () => {
    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (error) {
      setError(error.message);
    }
  };

  // Handle user sign-in using Google provider with Firebase Authentication
  const handleGoogleSignIn = async () => {
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (error) {
      setError(error.message);
    }
  };

  // Handle user sign-out using Firebase Authentication
  const handleSignOut = async () => {
    try {
      await signOut(auth);
    } catch (error) {
      setError(error.message);
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
          {error && <p>{error}</p>}
        </div>
      )}
    </div>
  );
}
