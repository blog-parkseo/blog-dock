import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { api, fieldErrors } from '../api'
import Logo from '../components/Logo'
import { safeRedirect, withRedirect } from '../safeRedirect'

// AUTH-01 로그인: 아이디·비밀번호 + 카카오·구글(키를 넣었을 때) + 닉네임 로그인(내 맥에서 연습할 때만)
export default function LoginPage() {
  const { me, login, devLogin } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const redirect = safeRedirect(params)
  const [providers, setProviders] = useState(null)
  const [form, setForm] = useState({ loginId: '', password: '' })
  const [nickname, setNickname] = useState('')
  const socialError = { kakao: '카카오', google: '구글' }[params.get('error')]
  const [errors, setErrors] = useState(socialError ? { form: `${socialError} 로그인을 마치지 못했어요. 다시 시도해 주세요.` } : {})

  useEffect(() => { api('GET', '/api/auth/providers').then(setProviders).catch(() => setProviders({ password: true })) }, [])
  useEffect(() => { if (me) navigate(redirect, { replace: true }) }, [me, navigate, redirect])

  const change = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    try {
      await login(form.loginId, form.password)
    } catch (err) {
      // 틀렸을 때는 아이디·비밀번호 중 무엇이 틀렸는지 알려 주지 않는다
      setErrors({ ...fieldErrors(err), form: err.fields.length ? '' : err.message })
      setForm({ ...form, password: '' })
    }
  }

  const submitDev = async (e) => {
    e.preventDefault()
    try {
      await devLogin(nickname)
    } catch (err) {
      setErrors({ dev: fieldErrors(err).nickname ?? err.message })
    }
  }

  return (
    <div className="login panel narrow">
      <div className="center"><Logo size={48} /></div>
      <h1 className="center">블로그독 로그인</h1>

      <form onSubmit={submit}>
        <label htmlFor="loginId">아이디</label>
        <input id="loginId" autoComplete="username" value={form.loginId} maxLength={20} onChange={change('loginId')} />
        {errors.loginId && <div className="error">{errors.loginId}</div>}

        <label htmlFor="password">비밀번호</label>
        <input id="password" type="password" autoComplete="current-password" value={form.password} maxLength={64}
               onChange={change('password')} />
        {errors.password && <div className="error">{errors.password}</div>}

        {errors.form && <div className="error">{errors.form}</div>}
        <button type="submit" className="primary">로그인</button>
      </form>
      <p className="hint center">아직 회원이 아니면 <Link to={withRedirect('/signup', params)}>회원가입</Link></p>

      {(providers?.kakao || providers?.google) && <div className="divider"><span>또는</span></div>}
      {providers?.kakao && (
        <a className="button kakao" href={`/api/auth/kakao?redirect=${encodeURIComponent(redirect)}`}>
          <KakaoMark />카카오로 시작하기
        </a>
      )}
      {providers?.google && (
        <a className="button google" href={`/api/auth/google?redirect=${encodeURIComponent(redirect)}`}>
          <GoogleMark />구글로 시작하기
        </a>
      )}

      {providers?.dev && (
        <form onSubmit={submitDev}>
          <div className="divider"><span>연습용 닉네임 로그인</span></div>
          <label htmlFor="nickname">닉네임</label>
          <input id="nickname" value={nickname} maxLength={30} onChange={(e) => setNickname(e.target.value)} />
          <p className="hint">처음 쓰는 닉네임이면 새 회원으로 가입돼요. 서버에서는 꺼져 있어요.</p>
          {errors.dev && <div className="error">{errors.dev}</div>}
          <button type="submit">닉네임으로 로그인</button>
        </form>
      )}
    </div>
  )
}

// 카카오 말풍선 마크 (카카오 로그인 버튼 디자인 가이드 모양)
function KakaoMark() {
  return (
    <svg className="mark" viewBox="0 0 24 24" aria-hidden="true">
      <path fill="#000" d="M12 3C6.48 3 2 6.48 2 10.78c0 2.77 1.86 5.2 4.66 6.58l-.95 3.48c-.08.3.26.54.52.37l4.15-2.74c.53.06 1.07.09 1.62.09 5.52 0 10-3.48 10-7.78S17.52 3 12 3z" />
    </svg>
  )
}

// 구글 G 마크 (구글 로그인 브랜드 가이드의 네 가지 색)
function GoogleMark() {
  return (
    <svg className="mark" viewBox="0 0 48 48" aria-hidden="true">
      <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
      <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
      <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
      <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
    </svg>
  )
}
