import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { api, fieldErrors } from '../api'
import Logo from '../components/Logo'
import { safeRedirect, withRedirect } from '../safeRedirect'

// AUTH-01 로그인: 아이디·비밀번호 + 카카오(키를 넣었을 때) + 닉네임 로그인(내 맥에서 연습할 때만)
export default function LoginPage() {
  const { me, login, devLogin } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const redirect = safeRedirect(params)
  const [providers, setProviders] = useState(null)
  const [form, setForm] = useState({ loginId: '', password: '' })
  const [nickname, setNickname] = useState('')
  const [errors, setErrors] = useState(params.get('error') === 'kakao' ? { form: '카카오 로그인을 마치지 못했어요. 다시 시도해 주세요.' } : {})

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

      {providers?.kakao && (
        <>
          <div className="divider"><span>또는</span></div>
          <a className="button kakao" href={`/api/auth/kakao?redirect=${encodeURIComponent(redirect)}`}>카카오로 시작하기</a>
        </>
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
