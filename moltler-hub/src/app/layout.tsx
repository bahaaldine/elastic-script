import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "MoltlerHub - Skills Framework for Elasticsearch",
  description: "Build, share, and run skills on your data. The skills hub for Elasticsearch with 190+ ready-to-use skills for observability, security, search, and AI.",
  keywords: ["elasticsearch", "skills", "moltler", "observability", "security", "search", "AI", "elastic-script"],
  authors: [{ name: "Moltler" }],
  openGraph: {
    title: "MoltlerHub - Skills Framework for Elasticsearch",
    description: "Build, share, and run skills on your data. 190+ ready-to-use skills for observability, security, search, and AI.",
    url: "https://hub.moltler.dev",
    siteName: "MoltlerHub",
    type: "website",
    locale: "en_US",
  },
  twitter: {
    card: "summary_large_image",
    title: "MoltlerHub - Skills Framework for Elasticsearch",
    description: "Build, share, and run skills on your data. 190+ ready-to-use skills for observability, security, search, and AI.",
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        {children}
      </body>
    </html>
  );
}
