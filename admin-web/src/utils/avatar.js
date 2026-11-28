const TONE_COUNT = 12

export function customerInitials(name) {
  const parts = String(name || '')
    .replace(/[,\-·]/g, ' ')
    .split(/\s+/)
    .filter(Boolean)
    .filter((p) => !/^(ji|pvt|ltd|ltd\.|co|co\.|&|and|the)$/i.test(p))
  if (!parts.length) return '?'
  const a = parts[0][0] || ''
  const b = parts.length > 1 ? parts[parts.length - 1][0] || '' : parts[0][1] || ''
  return (a + b).toUpperCase()
}

export function avatarTone(seed) {
  const s = String(seed || '').trim().toLowerCase()
  if (!s) return 11
  let h = 0
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0
  return Math.abs(h) % TONE_COUNT
}

export function resolvePhotoUrl(path) {
  if (!path) return ''
  if (/^https?:\/\//i.test(path) || path.startsWith('/')) return path
  return `/${path.replace(/^\.\//, '')}`
}
