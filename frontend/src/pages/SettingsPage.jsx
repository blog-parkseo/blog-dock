import { useState } from 'react'
import { api, fieldErrors } from '../api'
import { useAuth } from '../AuthContext'
import ImageField from '../components/ImageField'

// AUTH-05 회원정보 수정: 닉네임과 프로필 이미지
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
  )
}
