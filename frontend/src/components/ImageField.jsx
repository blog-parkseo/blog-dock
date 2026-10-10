import { useState } from 'react'
import { uploadImage } from '../api'

// 프로필 이미지 고르기 (AUTH-05, BLOG-02). 업로드 규칙은 서버가 확인한다
export default function ImageField({ value, onChange, label }) {
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const pick = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setBusy(true)
    setError('')
    try {
      onChange((await uploadImage(file)).url)
    } catch (err) {
      setError(err.fields?.[0]?.reason ?? err.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className="image-field">
      <img src={value || '/favicon.svg'} alt="" width={72} height={72} />
      <label className="button">
        {busy ? '올리는 중…' : `${label} 바꾸기`}
        <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" onChange={pick} hidden />
      </label>
      {value && <button type="button" className="link" onClick={() => onChange('')}>기본 이미지로</button>}
      {error && <div className="error">{error}</div>}
    </div>
  )
}
