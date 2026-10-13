import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { fieldErrors } from '../api'
import Logo from '../components/Logo'
import { safeRedirect, withRedirect } from '../safeRedirect'

// AUTH-01 회원가입. 가입하면 바로 로그인된다
export default function SignupPage() {
  const { me, signup } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const redirect = safeRedirect(params)
  const [form, setForm] = useState({ loginId: '', password: '', passwordConfirm: '', nickname: '' })
  const [errors, setErrors] = useState({})

  useEffect(() => { if (me) navigate(redirect, { replace: true }) }, [me, navigate, redirect])

  const change = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    // 비밀번호 확인은 화면에서만 검사한다
    if (form.password !== form.passwordConfirm) {
      return setErrors({ passwordConfirm: '비밀번호가 서로 달라요' })
    }
    try {
      await signup({ loginId: form.loginId, password: form.password, nickname: form.nickname })
    } catch (err) {
      setErrors({ ...fieldErrors(err), form: err.fields.length ? '' : err.message })
    }
  }

  return (
    <div className="login panel narrow">
      <div className="center"><Logo size={48} /></div>
      <h1 className="center">회원가입</h1>
      <form onSubmit={submit}>
        <label htmlFor="loginId">아이디</label>
        <input id="loginId" autoComplete="username" value={form.loginId} maxLength={20} onChange={change('loginId')} />
        <p className="hint">영문 소문자로 시작하고, 영문 소문자·숫자로 4~20자예요. 나중에 바꿀 수 없어요.</p>
        {errors.loginId && <div className="error">{errors.loginId}</div>}

        <label htmlFor="password">비밀번호</label>
        <input id="password" type="password" autoComplete="new-password" value={form.password} maxLength={64}
               onChange={change('password')} />
        <p className="hint">영문과 숫자를 섞어 8~64자예요.</p>
        {errors.password && <div className="error">{errors.password}</div>}

        <label htmlFor="passwordConfirm">비밀번호 확인</label>
        <input id="passwordConfirm" type="password" autoComplete="new-password" value={form.passwordConfirm} maxLength={64}
               onChange={change('passwordConfirm')} />
        {errors.passwordConfirm && <div className="error">{errors.passwordConfirm}</div>}

        <label htmlFor="nickname">닉네임</label>
        <input id="nickname" value={form.nickname} maxLength={30} onChange={change('nickname')} />
        {errors.nickname && <div className="error">{errors.nickname}</div>}

        {errors.form && <div className="error">{errors.form}</div>}
        <button type="submit" className="primary">가입하기</button>
      </form>
      <p className="hint center">이미 회원이면 <Link to={withRedirect('/login', params)}>로그인</Link></p>
    </div>
  )
}
