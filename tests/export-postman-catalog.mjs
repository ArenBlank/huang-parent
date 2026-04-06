import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const rootDir = path.resolve(__dirname, '..')
const collectionPath = path.join(__dirname, 'fitness-platform-full.postman_collection.json')
const outputPath = path.join(rootDir, 'doc', '请求参数目录.md')

function readJson(filePath) {
  let text = fs.readFileSync(filePath, 'utf8')
  if (text.charCodeAt(0) === 0xfeff) {
    text = text.slice(1)
  }
  return JSON.parse(text)
}

function flattenItems(items, acc = []) {
  for (const item of items) {
    if (item.item) {
      flattenItems(item.item, acc)
      continue
    }
    if (item.request) {
      acc.push(item)
    }
  }
  return acc
}

function sanitizeCell(value) {
  return String(value ?? '').replace(/\|/g, '\\|').replace(/\r?\n/g, '<br>')
}

function formatUrl(url) {
  if (!url) return ''
  if (typeof url === 'string') return url
  if (url.raw) return url.raw
  const protocol = url.protocol ? `${url.protocol}://` : ''
  const host = Array.isArray(url.host) ? url.host.join('.') : ''
  const pathname = Array.isArray(url.path) ? `/${url.path.join('/')}` : ''
  return `${protocol}${host}${pathname}`
}

function renderTable(headers, rows) {
  if (!rows.length) return ''
  const head = `| ${headers.join(' | ')} |\n| ${headers.map(() => '---').join(' | ')} |`
  const body = rows.map((row) => `| ${row.map(sanitizeCell).join(' | ')} |`).join('\n')
  return `${head}\n${body}\n`
}

function renderQuery(url) {
  const query = Array.isArray(url?.query) ? url.query.filter((item) => !item.disabled) : []
  if (!query.length) return ''
  return [
    '### 查询参数',
    '',
    renderTable(
      ['参数名', '示例值', '说明'],
      query.map((item) => [item.key, item.value ?? '', item.description ?? ''])
    )
  ].join('\n')
}

function renderHeaders(request) {
  const headers = Array.isArray(request.header) ? request.header.filter((item) => !item.disabled) : []
  if (!headers.length) return ''
  return [
    '### 请求头',
    '',
    renderTable(
      ['名称', '值'],
      headers.map((item) => [item.key, item.value ?? ''])
    )
  ].join('\n')
}

function renderBody(body) {
  if (!body || body.disabled) return ''

  if (body.mode === 'raw' && body.raw) {
    const language = body.options?.raw?.language === 'json' ? 'json' : ''
    return [
      '### 请求体',
      '',
      `\`\`\`${language}`,
      body.raw,
      '```'
    ].join('\n')
  }

  if (body.mode === 'formdata') {
    const rows = (body.formdata || [])
      .filter((item) => !item.disabled)
      .map((item) => [item.key, item.value ?? '', item.type ?? 'text'])
    if (!rows.length) return ''
    return [
      '### 表单数据',
      '',
      renderTable(['字段', '值', '类型'], rows)
    ].join('\n')
  }

  if (body.mode === 'urlencoded') {
    const rows = (body.urlencoded || [])
      .filter((item) => !item.disabled)
      .map((item) => [item.key, item.value ?? ''])
    if (!rows.length) return ''
    return [
      '### 表单编码',
      '',
      renderTable(['字段', '值'], rows)
    ].join('\n')
  }

  return ''
}

const collection = readJson(collectionPath)
const requests = flattenItems(collection.item)

const lines = [
  '# 请求参数目录',
  '',
  '此文件由 `tests/fitness-platform-full.postman_collection.json` 自动生成。',
  '如果 Postman 集合有变更，请重新执行 `node tests/export-postman-catalog.mjs`。',
  '',
  `接口总数：${requests.length}`,
  ''
]

requests.forEach((item, index) => {
  const request = item.request
  const url = formatUrl(request.url)
  lines.push(`## ${String(index + 1).padStart(3, '0')} ${item.name}`)
  lines.push('')
  lines.push(`- 请求方式：\`${request.method}\``)
  lines.push(`- 请求地址：\`${url}\``)
  lines.push('')

  const sections = [
    renderHeaders(request),
    renderQuery(request.url),
    renderBody(request.body)
  ].filter(Boolean)

  if (sections.length) {
    lines.push(sections.join('\n\n'))
    lines.push('')
  }
})

fs.writeFileSync(outputPath, `${lines.join('\n').trimEnd()}\n`, 'utf8')
console.log(`Generated ${path.relative(rootDir, outputPath)}`)
