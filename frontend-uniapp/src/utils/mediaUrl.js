const INTERNAL_HOSTS = ['127.0.0.1:9000', '127.0.0.1:9010', 'localhost:9000', 'localhost:9010', 'files.localhost']
const PUBLIC_MINIO = 'https://smartfitness.vip.cpolar.cn/minio'

export const toPublicUrl = (url) => {
  const raw = String(url || '').trim()
  if (!raw) return ''
  if (/^https?:\/\//.test(raw)) {
    for (const host of INTERNAL_HOSTS) {
      if (raw.includes(host)) {
        return raw.replace(new RegExp(`https?://${host.replace(/\./g, '\\.')}(:\\d+)?`), PUBLIC_MINIO)
      }
    }
    return raw
  }
  return raw.startsWith('/') ? PUBLIC_MINIO + raw : PUBLIC_MINIO + '/' + raw
}
