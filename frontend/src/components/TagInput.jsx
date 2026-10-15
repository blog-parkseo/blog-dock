import { useState } from 'react'

// 태그 입력: Enter나 쉼표로 하나씩 단다. 같은 태그는 한 번만, 최대 10개 (TAG-01)
export default function TagInput({ value, onChange, max = 10 }) {
  const [text, setText] = useState('')
  const [hint, setHint] = useState('')

  const add = (raw) => {
    const name = raw.trim().replace(/^#+/, '').trim()
    setText('')
    if (!name) return
    if (value.some((t) => t.toLowerCase() === name.toLowerCase())) { setHint('이미 단 태그예요'); return }
    if (value.length >= max) { setHint(`태그는 ${max}개까지 달 수 있어요`); return }
    setHint('')
    onChange([...value, name])
  }

  const onKeyDown = (e) => {
    if (e.nativeEvent.isComposing) return // 한글 입력 중에는 기다린다
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault()
      add(text)
    } else if (e.key === 'Backspace' && !text && value.length) {
      onChange(value.slice(0, -1))
    }
  }

  return (
    <div className="tag-input">
      {value.map((t) => (
        <span key={t} className="tag">#{t}
          <button type="button" className="link" aria-label={`${t} 태그 빼기`} onClick={() => onChange(value.filter((x) => x !== t))}>×</button>
        </span>
      ))}
      <input aria-label="태그 입력" value={text} maxLength={30} placeholder={value.length < max ? '태그 입력 후 Enter' : ''}
        onChange={(e) => setText(e.target.value)} onKeyDown={onKeyDown} onBlur={() => text && add(text)} />
      {hint && <div className="hint">{hint}</div>}
    </div>
  )
}
