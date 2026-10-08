import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import { api, fieldErrors } from '../api'

const REASONS = {
  FORMAT: '영문 소문자·숫자·하이픈으로 4~32자, 하이픈으로 시작하거나 끝날 수 없어요',
  RESERVED: '쓸 수 없는 주소예요',
  TAKEN: '이미 사용 중인 주소예요',
}

// BLOG-01 블로그 개설
export default function BlogCreatePage() {
  const { me, refresh } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ address: '', name: '', description: '' })
  const [check, setCheck] = useState(null)
  const [errors, setErrors] = useState({})

  if (me.blogAddress) return <Navigate to={`/blog/${me.blogAddress}`} replace />

  const change = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  // 주소 칸에서 나가면 바로 검사한다
  const checkAddress = async () => {
    if (!form.address) return setCheck(null)
    setCheck(await api('GET', `/api/blogs/address-check?address=${encodeURIComponent(form.address)}`))
  }

  const submit = async (e) => {
    e.preventDefault()
    try {
      const blog = await api('POST', '/api/blogs', form)
      await refresh() // 헤더의 "내 블로그"를 바로 보이게
      navigate(`/blog/${blog.address}`)
    } catch (err) {
      setErrors({ ...fieldErrors(err), form: err.fields.length ? '' : err.message })
    }
  }

  return (
    <form onSubmit={submit}>
      <h1>블로그 만들기</h1>
      <label htmlFor="address">블로그 주소</label>
      <input id="address" value={form.address} onChange={change('address')} onBlur={checkAddress} />
      <div className="hint">주소는 나중에 바꿀 수 없어요.</div>
      {check && (check.available
        ? <div className="ok">사용할 수 있는 주소예요</div>
        : <div className="error">{REASONS[check.reason]}</div>)}
      {errors.address && <div className="error">{errors.address}</div>}

      <label htmlFor="name">블로그 이름</label>
      <input id="name" value={form.name} onChange={change('name')} />
      {errors.name && <div className="error">{errors.name}</div>}

      <label htmlFor="description">소개 (선택)</label>
      <textarea id="description" rows={3} value={form.description} onChange={change('description')} />
      {errors.description && <div className="error">{errors.description}</div>}

      {errors.form && <div className="error">{errors.form}</div>}
      <p><button type="submit">만들기</button></p>
    </form>
  )
}
