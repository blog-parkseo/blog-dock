import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { fieldErrors } from '../api'

// AUTH-01 가짜 로그인. 5단계에서 카카오 버튼으로 바꾼다
export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [nickname, setNickname] = useState('')
  const [error, setError] = useState('')

  const submit = async (e) => {
    e.preventDefault()
    try {
      await login(nickname)
      // 로그인 전에 보던 화면으로, 없으면 홈으로. 다른 사이트 주소는 받지 않는다
      const redirect = params.get('redirect')
      navigate(redirect && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/')
    } catch (err) {
      setError(fieldErrors(err).nickname ?? err.message)
    }
  }

  return (
    <form onSubmit={submit}>
      <h1>로그인 (개발용)</h1>
      <label htmlFor="nickname">닉네임</label>
      <input id="nickname" value={nickname} onChange={(e) => setNickname(e.target.value)} />
      {error && <div className="error">{error}</div>}
      <p className="hint">처음 쓰는 닉네임이면 새 회원으로 가입돼요. admin으로 들어가면 관리자예요.</p>
      <button type="submit">로그인</button>
    </form>
  )
}
