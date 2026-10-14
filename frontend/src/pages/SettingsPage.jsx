import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api, fieldErrors } from '../api'
import { useAuth } from '../AuthContext'
import ImageField from '../components/ImageField'
import { KakaoMark, GoogleMark, GithubMark } from '../components/SocialMarks'

// AUTH-05 회원정보 수정: 닉네임과 프로필 이미지, 소셜 계정 연결
export default function SettingsPage() {
  const { me, refresh } = useAuth()
  const [form, setForm] = useState({ nickname: me.nickname, profileImageUrl: me.profileImageUrl ?? '' })
  const [errors, setErrors] = useState({})
  const [done, setDone] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setDone(false)
    try {
      await api('PATCH', '/api/me', form)
      await refresh()
      setErrors({})
      setDone(true)
    } catch (err) {
      setErrors({ ...fieldErrors(err), form: err.fields.length ? '' : err.message })
    }
  }

  return (
    <>
    <form onSubmit={submit} className="panel narrow">
      <h1>회원정보 수정</h1>
      <label>프로필 이미지</label>
      <ImageField label="프로필 이미지" value={form.profileImageUrl} onChange={(url) => setForm({ ...form, profileImageUrl: url })} />
      {errors.profileImageUrl && <div className="error">{errors.profileImageUrl}</div>}
      <label htmlFor="nickname">닉네임</label>
      <input id="nickname" value={form.nickname} maxLength={30} onChange={(e) => setForm({ ...form, nickname: e.target.value })} />
      {errors.nickname && <div className="error">{errors.nickname}</div>}
      {errors.form && <div className="error">{errors.form}</div>}
      {done && <div className="ok">저장했어요.</div>}
      <p><button type="submit" className="primary">저장</button></p>
    </form>
    <SocialLinks />
    </>
  )
}

const SOCIALS = [
  { key: 'kakao', name: '카카오', Mark: KakaoMark },
  { key: 'google', name: '구글', Mark: GoogleMark },
  { key: 'github', name: '깃허브', Mark: GithubMark },
]

const LINK_ERRORS = {
  used: '이 계정은 이미 다른 블로그 회원이 쓰고 있어서 연결할 수 없어요.',
  already: '같은 곳의 다른 계정이 이미 연결돼 있어요. 먼저 연결을 해제해 주세요.',
  failed: '연결하지 못했어요. 잠시 후 다시 시도해 주세요.',
}

// 계정 연결: 어느 소셜로 로그인해도 같은 회원(같은 블로그)으로 들어온다
function SocialLinks() {
  const [params, setParams] = useSearchParams()
  const [providers, setProviders] = useState(null)
  const [linked, setLinked] = useState([])
  const [message, setMessage] = useState(() => {
    const ok = SOCIALS.find((s) => s.key === params.get('linked'))
    if (ok) return { ok: true, text: `${ok.name} 계정을 연결했어요.` }
    const err = LINK_ERRORS[params.get('linkError')]
    const reason = params.get('reason')
    return err ? { ok: false, text: reason ? `${err} (원인: ${reason})` : err } : null
  })

  const load = () => api('GET', '/api/me/socials').then(setLinked).catch(() => setLinked([]))
  useEffect(() => {
    api('GET', '/api/auth/providers').then(setProviders).catch(() => setProviders({}))
    load()
    if (params.has('linked') || params.has('linkError')) setParams({}, { replace: true })
  }, [])

  const unlink = async (s) => {
    if (!window.confirm(`${s.name} 계정 연결을 해제할까요?`)) return
    try {
      await api('DELETE', `/api/me/socials/${s.key}`)
      setMessage({ ok: true, text: `${s.name} 계정 연결을 해제했어요.` })
      load()
    } catch (err) {
      setMessage({ ok: false, text: err.message })
    }
  }

  const shown = SOCIALS.filter((s) => providers?.[s.key] || linked.some((l) => l.provider === s.key))
  if (!providers || shown.length === 0) return null
  return (
    <section className="panel narrow social-links">
      <h2>계정 연결</h2>
      <p className="hint">연결해 두면 어느 계정으로 로그인해도 지금 블로그로 들어와요.</p>
      <ul>
        {shown.map((s) => {
          const on = linked.find((l) => l.provider === s.key)
          return (
            <li key={s.key}>
              <span className={`social-mark ${s.key}`}><s.Mark /></span>
              <span className="social-name">{s.name}</span>
              {on ? (
                <>
                  <span className="social-state">연결됨</span>
                  <button type="button" onClick={() => unlink(s)}>연결 해제</button>
                </>
              ) : (
                <a className="button" href={`/api/auth/${s.key}/link`}>연결하기</a>
              )}
            </li>
          )
        })}
      </ul>
      {message && <div className={message.ok ? 'ok' : 'error'}>{message.text}</div>}
    </section>
  )
}
