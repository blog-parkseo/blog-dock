// 백엔드 API를 부르는 함수 하나. 오류면 ApiError를 던진다 (COM-02).
export class ApiError extends Error {
  constructor(status, body) {
    super(body?.message ?? '잠시 후 다시 시도해 주세요')
    this.status = status
    this.code = body?.code
    this.fields = body?.fields ?? []
  }
}

export async function api(method, path, body) {
  const res = await fetch(path, {
    method,
    credentials: 'include', // 세션 쿠키를 같이 보낸다
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  })
  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  if (!res.ok) throw new ApiError(res.status, data)
  return data
}

// 항목별 오류 문구를 { address: '...', name: '...' } 모양으로 바꾼다
export function fieldErrors(err) {
  const map = {}
  for (const f of err?.fields ?? []) map[f.field] = f.reason
  return map
}
