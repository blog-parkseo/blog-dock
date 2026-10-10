import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { api, fieldErrors } from '../api'
import Logo from '../components/Logo'

// 로그인 전에 보던 화면으로, 없으면 홈으로. 다른 사이트 주소는 받지 않는다
const safe = (r) => (r && r.startsWith('/') && !r.startsWith('//') && !r.includes('\\') ? r : '/')

// AUTH-01 가입·로그인: 카카오 (키를 넣었을 때) + 개발용 가짜 로그인
export default function LoginPage() {
  const { me, login } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const redirect = safe(params.get('redirect'))
  const [providers, setProviders] = useState(null)
  const [nickname, setNickname] = useState('')
  const [error, setError] = useState(params.get('error') === 'kakao' ? '카카오 로그인을 마치지 못했어요. 다시 시도해 주세요.' : '')

  useEffect(() => { api('GET', '/api/auth/providers').then(setProviders).catch(() => setProviders({ dev: true })) }, [])
  useEffect(() => { if (me) navigate(redirect, { replace: true }) }, [me, navigate, redirect])

  const submit = async (e) => {
    e.preventDefault()
    try {
      await login(nickname)
    } catch (err) {
      setError(fieldErrors(err).nickname ?? err.message)
    }
  }

  return (
    <div className="login panel narrow">
      <div className="center"><Logo size={48} /></div>
      <h1 className="center">블로그독 로그인</h1>
      {error && <div className="error">{error}</div>}
      {providers?.kakao && (
        <a className="button kakao" href={`/api/auth/kakao?redirect=${encodeURIComponent(redirect)}`}>카카오로 시작하기</a>
      )}
      {providers && !providers.kakao && <p className="hint">카카오 로그인은 키를 설정하면 나타나요 (README 참고).</p>}
      {providers?.dev && (
        <form onSubmit={submit}>
          <div className="divider"><span>개발용 로그인</span></div>
          <label htmlFor="nickname">닉네임</label>
          <input id="nickname" value={nickname} maxLength={30} onChange={(e) => setNickname(e.target.value)} />
          <p className="hint">처음 쓰는 닉네임이면 새 회원으로 가입돼요. admin으로 들어가면 관리자예요.</p>
          <button type="submit" className="primary">로그인</button>
        </form>
      )}
    </div>
  )
}
