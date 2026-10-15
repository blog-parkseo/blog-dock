import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, fieldErrors } from '../api'
import ErrorView from '../components/ErrorView'
import ImageField from '../components/ImageField'
import CoverArt, { COVER_COLORS, COVER_SCENES, coverColor } from '../components/CoverArt'
import { useBlog } from './blog/BlogLayout'

// BLOG-02 블로그 정보 수정: 이름·소개·프로필 이미지·위쪽 배경. 주소는 보여 주기만 하고 바꿀 수 없다
export default function BlogEditPage() {
  const { address, blog, reload, link } = useBlog()
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: blog.name, description: blog.description ?? '', profileImageUrl: blog.profileImageUrl ?? '', cover: blog.cover ?? 'AUTO' })
  const [errors, setErrors] = useState({})

  if (!blog.isOwner) return <ErrorView status={403} />

  const submit = async (e) => {
    e.preventDefault()
    try {
      await api('PATCH', `/api/blogs/${address}`, form)
      reload()
      navigate(link('').href)
    } catch (err) {
      setErrors({ ...fieldErrors(err), form: err.fields.length ? '' : err.message })
    }
  }
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  return (
    <form onSubmit={submit} className="panel">
      <h2>블로그 정보 수정</h2>
      <label htmlFor="address">블로그 주소</label>
      <input id="address" value={address} disabled />
      <label>프로필 이미지</label>
      <ImageField label="프로필 이미지" value={form.profileImageUrl} onChange={(url) => setForm({ ...form, profileImageUrl: url })} />
      {errors.profileImageUrl && <div className="error">{errors.profileImageUrl}</div>}
      <CoverPicker value={form.cover} address={address} onChange={(cover) => setForm({ ...form, cover })} />
      {errors.cover && <div className="error">{errors.cover}</div>}
      <label htmlFor="name">블로그 이름</label>
      <input id="name" value={form.name} maxLength={40} onChange={set('name')} />
      {errors.name && <div className="error">{errors.name}</div>}
      <label htmlFor="description">소개</label>
      <textarea id="description" rows={3} maxLength={500} value={form.description} onChange={set('description')} />
      {errors.description && <div className="error">{errors.description}</div>}
      {errors.form && <div className="error">{errors.form}</div>}
      <p><button type="submit" className="primary">저장</button></p>
    </form>
  )
}

// 위쪽 배경 고르기: 그림 4가지, 색 6가지, 자동
function CoverPicker({ value, address, onChange }) {
  const option = (code, name, swatch) => (
    <button type="button" key={code} className={`cover-option${value === code ? ' selected' : ''}`}
      aria-pressed={value === code} onClick={() => onChange(code)}>
      {swatch}
      <span>{name}</span>
    </button>
  )
  return (
    <fieldset className="cover-picker">
      <legend>위쪽 배경</legend>
      <p className="hint">그림</p>
      <div className="cover-options scenes">
        {COVER_SCENES.map((s) => option(s.code, s.name, <span className="cover-swatch scene"><CoverArt code={s.code} className="cover-swatch-art" /></span>))}
      </div>
      <p className="hint">색</p>
      <div className="cover-options">
        {option('AUTO', '자동', <span className="cover-swatch auto" style={{ '--cover': coverColor(null, address) }}>A</span>)}
        {COVER_COLORS.map((c) => option(c.code, c.name, <span className="cover-swatch" style={{ '--cover': c.color }} />))}
      </div>
    </fieldset>
  )
}
