import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import ToastProvider from "../components/ToastProvider";
import RoleNavbar from "../components/RoleNavbar";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata = {
  title: "V-Beta",
  description: "V-Beta is a platform for creating and sharing beta solutions to problems.",
  icons: {
    icon: "/VBetaLogo.svg",
    shortcut: "/VBetaLogo.svg",
    apple: "/VBetaLogo.svg",
  },
  appleWebApp: {
    capable: true,
    title: "V-Beta",
    statusBarStyle: "default",
  },
  formatDetection: {
    telephone: false,
  },
};

export const viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f4f4f5" },
    { media: "(prefers-color-scheme: dark)", color: "#0a1120" },
  ],
};

export default function RootLayout({ children }) {
  return (
    <html lang="en" className={`${geistSans.variable} ${geistMono.variable}`}>
      <body>
        <ToastProvider>
          <RoleNavbar />
          {children}
        </ToastProvider>
      </body>
    </html>
  );
}
