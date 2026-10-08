import { useEffect, useRef, useState } from 'react'
import { Navigate, useNavigate, useParams } from 'react-router-dom'
import { api, fieldErrors, newRequestKey } from '../api'
import { useAuth } from '../AuthContext'
import { blogLink } from '../blogUrl'
import Editor from '../components/Editor'
import ErrorView from '../components/ErrorView'
import TagInput from '../components/TagInput'

// 본문 HTML에서 이 서버에 올린 이미지 주소를 고른 순서대로 찾는다 (대표 이미지 고르기)
function imagesIn(html) {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  return [...doc.querySelectorAll('img')].map((i) => i.getAttribute('src')).filter((s) => s?.startsWith('/files/'))
}

// POST-01 작성·발행, POST-02 수정, POST-08 임시저장 이어 쓰기
export default function WritePage() {
  const { postId } = useParams()
  const { me, refresh } = useAuth()
  const navigate = useNavigate()
  const editorRef = useRef(null)
  const requestKey = useRef(newRequestKey())
  const saving = useRef(false) // 화면이 다시 그려지기 전에 또 눌러도 막는다
  const [loaded, setLoaded] = useState(postId ? null : { title: '', contentMarkdown: '', categoryId: null, tags: [], visibility: 'PUBLIC', status: 'DRAFT' })
  const [loadError, setLoadError] = useState(null)
  const [categories, setCategories] = useState([])
  const [form, setForm] = useState(null)
  const [images, setImages] = useState([])
  const [thumb, setThumb] = useState('')
  const [errors, setErrors] = useState({})
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (!me?.blogAddress) return
    api('GET', `/api/blogs/${me.blogAddress}/categories`).then(setCategories)
    if (postId) {
      api('GET', `/api/posts/${postId}/edit`).then(setLoaded).catch(setLoadError)
    }
  }, [postId, me?.blogAddress])

  useEffect(() => {
    if (loaded) {
      setForm({ title: loaded.title, categoryId: loaded.categoryId ?? '', tags: loaded.tags, visibility: loaded.visibility })
      const imgs = loaded.contentHtml ? imagesIn(loaded.contentHtml) : []
      setImages(imgs)
      // 전에 고른 대표 이미지가 첫 이미지가 아니면 다시 골라 둔다 (썸네일 주소: 원본이름_t.확장자)
      const chosen = imgs.find((src) => loaded.thumbnailUrl?.startsWith(src.replace(/\.[^.]+$/, '_t.')))
      if (chosen && chosen !== imgs[0]) setThumb(chosen)
    }
  }, [loaded])

  // AUTH-04: 블로그가 없으면 개설 화면으로
  if (!me.blogAddress) return <Navigate to="/blog/new" replace />
  if (loadError) return <ErrorView status={loadError.status} />
  if (!form) return null

  const published = loaded.status === 'PUBLISHED'

  const save = async (publish) => {
    if (saving.current) return
    saving.current = true
    setBusy(true)
    setErrors({})
    setNotice('')
    const contentHtml = editorRef.current.getHTML()
    const body = {
      title: form.title,
      contentMarkdown: editorRef.current.getMarkdown(),
      contentHtml,
      categoryId: form.categoryId === '' ? null : Number(form.categoryId),
      tags: form.tags,
      visibility: form.visibility,
      publish,
      // POST-07: 고르지 않으면 서버가 본문 첫 이미지를 대표로 쓴다
      thumbnailUrl: thumb && imagesIn(contentHtml).includes(thumb) ? thumb : null,
    }
    try {
      const saved = postId
        ? await api('PUT', `/api/posts/${postId}`, body)
        // 새 글은 요청 키를 붙여서 발행을 연달아 눌러도 하나만 생긴다
        : await api('POST', '/api/posts', body, { headers: { 'Idempotency-Key': requestKey.current } })
      await refresh()
      if (saved.status === 'PUBLISHED') {
        const to = blogLink(saved.blogAddress, saved.slug)
        if (to.external) window.location.href = to.href
        else navigate(to.href)
      } else {
        setNotice('임시저장했어요. 내 글 관리 > 임시저장에서 이어 쓸 수 있어요.')
        if (!postId) navigate(`/write/${saved.id}`, { replace: true })
      }
    } catch (err) {
      const fe = fieldErrors(err)
      setErrors(Object.keys(fe).length ? fe : { form: err.message })
      if (fe.content) editorRef.current.focus()
    } finally {
      saving.current = false
      setBusy(false)
    }
  }

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  return (
    <div className="write panel">
      <h1>{postId ? (published ? '글 수정' : '임시저장 글 이어 쓰기') : '글쓰기'}</h1>
      <label htmlFor="title">제목</label>
      <input id="title" value={form.title} maxLength={100} onChange={set('title')} placeholder="제목 (100자 이하)" />
      {errors.title && <div className="error">{errors.title}</div>}

      <div className="row">
        <div>
          <label htmlFor="category">카테고리</label>
          <select id="category" value={form.categoryId} onChange={set('categoryId')}>
            <option value="">미분류</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          {errors.categoryId && <div className="error">{errors.categoryId}</div>}
        </div>
        <fieldset>
          <legend>공개 범위</legend>
          <label className="inline"><input type="radio" name="visibility" value="PUBLIC" checked={form.visibility === 'PUBLIC'} onChange={set('visibility')} /> 공개</label>
          <label className="inline"><input type="radio" name="visibility" value="PRIVATE" checked={form.visibility === 'PRIVATE'} onChange={set('visibility')} /> 비공개</label>
        </fieldset>
      </div>

      <label>본문</label>
      <Editor ref={editorRef} initialMarkdown={loaded.contentMarkdown}
        onChange={() => setImages(imagesIn(editorRef.current.getHTML()))}
        onImageError={(m) => setErrors({ content: m })} />
      {errors.content && <div className="error">{errors.content}</div>}

      <label>태그 (최대 10개)</label>
      <TagInput value={form.tags} onChange={(tags) => setForm({ ...form, tags })} />
      {errors.tags && <div className="error">{errors.tags}</div>}

      {images.length > 0 && (
        <fieldset className="thumb-picker">
          <legend>대표 이미지</legend>
          <label className="inline"><input type="radio" name="thumb" value="" checked={thumb === ''} onChange={() => setThumb('')} /> 본문 첫 이미지</label>
          {images.map((src) => (
            <label key={src} className="inline">
              <input type="radio" name="thumb" value={src} checked={thumb === src} onChange={() => setThumb(src)} />
              <img src={src} alt="" width={64} height={64} />
            </label>
          ))}
        </fieldset>
      )}

      {errors.form && <div className="error">{errors.form}</div>}
      {notice && <div className="ok">{notice}</div>}
      <p className="actions">
        {!published && <button onClick={() => save(false)} disabled={busy}>임시저장</button>}
        <button className="primary" onClick={() => save(true)} disabled={busy}>
          {busy ? '저장 중…' : published ? '수정 완료' : '발행'}
        </button>
      </p>
    </div>
  )
}
