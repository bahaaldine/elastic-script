import { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Browse Skills - MoltlerHub',
  description: 'Discover 190+ ready-to-use skills for Elasticsearch. Observability, Security, Search, AI, and more.',
  openGraph: {
    title: 'Browse Skills - MoltlerHub',
    description: 'Discover 190+ ready-to-use skills for Elasticsearch. Observability, Security, Search, AI, and more.',
    url: 'https://hub.moltler.dev/skills',
    siteName: 'MoltlerHub',
    type: 'website',
    locale: 'en_US',
  },
  twitter: {
    card: 'summary',
    title: 'Browse Skills - MoltlerHub',
    description: 'Discover 190+ ready-to-use skills for Elasticsearch. Observability, Security, Search, AI, and more.',
  },
};

export default function SkillsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
