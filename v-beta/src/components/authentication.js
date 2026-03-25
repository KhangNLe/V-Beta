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
import { API_BASE_URL } from "../app/envExports"; // Import the API base URL from environment variables

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

  // Function to synchronize the Firebase Authentication session with the backend session.
  const syncSessionWithBackend = async () => {
    const currentUser = auth.currentUser; // Get the current user from Firebase Authentication
    if (!currentUser) return; // If there is no user, exit the function

    const idToken = await currentUser.getIdToken(); // Get the ID token for the current user

    const apiBaseURL = API_BASE_URL; // Get the API base URL from environment variables (port 8080 for local development)

    const response = await fetch(`${apiBaseURL}/api/accounts/session`, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        Authorization: `Bearer ${idToken}`, // Include the ID token in the Authorization header
      },
      body: JSON.stringify({}),
    });

    if (!response.ok) {
      console.error("Failed to create backend session", response.statusText); // debug log the error response from the backend
      throw new Error("Failed to create backend session"); // Throw an error if the response is not successful
    }

    return response.json(); // Return the response as JSON, from the backend session creation endpoint
  };

  // Handle user sign-up using email and password using Firebase Authentication
  const handleSignup = async () => {
    try {
      setError("");
      await createUserWithEmailAndPassword(auth, email, password);
      await syncSessionWithBackend(); // Sync the session with the backend after successful sign-up
    } catch (err) {
      setError(err.message);
    }
  };

  // Handle user sign-in using email and password using Firebase Authentication
  const handleSignIn = async () => {
    try {
      await signInWithEmailAndPassword(auth, email, password);
      await syncSessionWithBackend(); // Sync the session with the backend after successful sign-in
    } catch (error) {
      setError(error.message);
    }
  };

  // Handle user sign-in using Google provider with Firebase Authentication
  const handleGoogleSignIn = async () => {
    try {
      await signInWithPopup(auth, googleProvider);
      await syncSessionWithBackend(); // Sync the session with the backend after successful Google sign-in
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
