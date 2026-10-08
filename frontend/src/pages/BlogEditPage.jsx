import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, fieldErrors } from '../api'
import ErrorView from '../components/ErrorView'

// BLOG-02 블로그 정보 수정. 주소는 보여 주기만 하고 바꿀 수 없다
export default function BlogEditPage() {
  const { address } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState(null)
  const [errors, setErrors] = useState({})
  const [loadError, setLoadError] = useState(null)

  useEffect(() => {
    api('GET', `/api/blogs/${address}`)
      .then((b) => (b.isOwner
        ? setForm({ name: b.name, description: b.description ?? '' })
        : setLoadError({ status: 403 })))
      .catch(setLoadError)
  }, [address])

  if (loadError) return <ErrorView status={loadError.status} />
  if (!form) return null

  const submit = async (e) => {
    e.preventDefault()
    try {
      await api('PATCH', `/api/blogs/${address}`, form)
      navigate(`/blog/${address}`)
    } catch (err) {
      setErrors(fieldErrors(err))
      if (!err.fields.length) setLoadError(err)
    }
  }

  return (
    <form onSubmit={submit}>
      <h1>블로그 정보 수정</h1>
      <label>블로그 주소</label>
      <input value={address} disabled />
      <label htmlFor="name">블로그 이름</label>
      <input id="name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
      {errors.name && <div className="error">{errors.name}</div>}
      <label htmlFor="description">소개</label>
      <textarea id="description" rows={3} value={form.description}
        onChange={(e) => setForm({ ...form, description: e.target.value })} />
      {errors.description && <div className="error">{errors.description}</div>}
      <p><button type="submit">저장</button></p>
    </form>
  )
}
