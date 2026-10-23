/** Production origins must be supplied by the owner of the deployed site. */
export const PRODUCTION_ORIGIN = process.env.MAVIMUSIC_SITE_ORIGIN || '';
const PAGES_ORIGIN = process.env.MAVIMUSIC_PAGES_ORIGIN || '';
const DEV_ORIGIN = 'http://localhost:5173';
const STORE_BUILD = process.env.MAVIMUSIC_STORE_BUILD === '1';

/** Exact origins enforced by the bridge, content scripts and network rules. */
export const SITE_ORIGINS: string[] = [...new Set((
  process.env.MAVIMUSIC_SITE_ORIGINS ??
  [PRODUCTION_ORIGIN, PAGES_ORIGIN, ...(STORE_BUILD ? [] : [DEV_ORIGIN])].filter(Boolean).join(',')
).split(',').map((o) => o.trim().replace(/\/+$/, '')).filter(Boolean))];

if (STORE_BUILD && (SITE_ORIGINS.length === 0 || SITE_ORIGINS.some((o) => new URL(o).protocol !== 'https:'))) {
  throw new Error('Configure the HTTPS MaviMusic site origins before building a store extension');
}

/**
 * Match patterns can't carry a port, so `http://localhost:5173` becomes
 * `http://localhost/*`. The exact origin, port included, is still enforced at
 * runtime against SITE_ORIGINS.
 */
export function matchPatternsFor(origins: string[]): string[] {
  return [...new Set(origins.map((o) => {
    const url = new URL(o);
    return `${url.protocol}//${url.hostname}/*`;
  }))];
}
