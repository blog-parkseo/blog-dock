import { useEffect, useState } from 'react'
import { api } from '../api'
import ErrorView from '../components/ErrorView'

// ADMIN-01 관리자 전용 영역. 일반 회원은 403 화면이 보인다
export default function AdminPage() {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api('GET', '/api/admin/ping').then(setData).catch(setError)
  }, [])

  if (error) return <ErrorView status={error.status} />
  if (!data) return null
  return <div className="panel"><h1>{data.message}</h1><p className="hint">회원 제한·신고 처리 같은 관리 기능은 2차에서 만들어요.</p></div>
}
