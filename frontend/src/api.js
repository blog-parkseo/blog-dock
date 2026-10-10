// 백엔드 API를 부르는 함수. 오류면 ApiError를 던진다 (COM-02).
export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message ?? '잠시 후 다시 시도해 주세요')
    this.status = status
    this.code = body?.code
    this.fields = body?.fields ?? []
  }
}

// CSRF: 서버가 준 XSRF-TOKEN 쿠키 값을 헤더로 돌려보낸다
function csrfToken() {
  const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  return m ? decodeURIComponent(m[1]) : null
}

async function request(method, path, { body, form, headers = {} } = {}) {
  const h = { ...headers }
  if (method !== 'GET') {
    const token = csrfToken()
    if (token) h['X-XSRF-TOKEN'] = token
  }
  if (body !== undefined) h['Content-Type'] = 'application/json'
  let res
  try {
    res = await fetch(path, {
      method,
      credentials: 'include', // 세션 쿠키를 같이 보낸다
      headers: h,
      body: form ?? (body !== undefined ? JSON.stringify(body) : undefined),
    })
  } catch {
    throw new ApiError(0, { message: '서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요' })
  }
  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  if (!res.ok) throw new ApiError(res.status, data)
  return data
}

export const api = (method, path, body, options = {}) => request(method, path, { body, ...options })

// 이미지 한 장 올리기 → { id, url, thumbUrl }
export function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  return request('POST', '/api/images', { form })
}

// 항목별 오류 문구를 { address: '...', name: '...' } 모양으로 바꾼다
export function fieldErrors(err) {
  const map = {}
  for (const f of err?.fields ?? []) map[f.field] = f.reason
  return map
}

// 연달아 눌러도 한 번만 처리되게 보내는 요청 키
export const newRequestKey = () =>
  crypto.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`.slice(0, 36)
