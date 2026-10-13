import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { api, fieldErrors } from '../api'
import { useAuth } from '../AuthContext'
import { blogLink } from '../blogUrl'
import { formatDate } from '../format'
import SmartLink from '../components/SmartLink'
import { Pager } from '../components/PostList'

// count: 탭 옆에 보여 줄 글 수의 이름 (/api/me/posts/counts)
const TABS = [
  { key: 'PUBLIC', label: '공개', count: 'publicCount' },
  { key: 'SUBSCRIBERS', label: '구독자 공개', count: 'subscribersCount' },
  { key: 'PRIVATE', label: '비공개', count: 'privateCount' },
  { key: 'DRAFT', label: '임시저장', count: 'draftCount' },
  { key: 'CATEGORIES', label: '카테고리' },
]

// 발행한 글의 공개 범위를 바로 바꾸는 버튼 (POST-06, POST-12)
const VISIBILITIES = [
  { key: 'PUBLIC', label: '공개' },
  { key: 'SUBSCRIBERS', label: '구독자 공개' },
  { key: 'PRIVATE', label: '비공개' },
]

// MNG-01 내 글 관리 + CAT-01·CAT-04 카테고리 관리
export default function ManagePage() {
  const { me } = useAuth()
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') ?? 'PUBLIC'
  const [counts, setCounts] = useState(null)
  // 글을 지우거나 공개 범위를 바꾸면 다시 센다
  const loadCounts = useCallback(() => {
    if (me.blogAddress) api('GET', '/api/me/posts/counts').then(setCounts).catch(() => {})
  }, [me.blogAddress])
  useEffect(() => { loadCounts() }, [loadCounts])
  if (!me.blogAddress) return <Navigate to="/blog/new" replace />
  return (
    <div className="panel">
      <h1>블로그 관리</h1>
      <nav className="tabs">
        {TABS.map((t) => (
          <button key={t.key} className={tab === t.key ? 'active' : ''} onClick={() => setParams({ tab: t.key })}>
            {t.label}{t.count && counts && <span className="tab-count">{counts[t.count]}</span>}
          </button>
        ))}
      </nav>
      {tab === 'CATEGORIES' ? <CategoryManager address={me.blogAddress} /> : <MyPosts status={tab} onChange={loadCounts} />}
    </div>
  )
}

function MyPosts({ status, onChange }) {
  const [params] = useSearchParams()
  const page = Number(params.get('page') ?? 0) || 0
  const [data, setData] = useState(null)

  const load = useCallback(() => {
    api('GET', `/api/me/posts?status=${status}&page=${page}`).then(setData)
  }, [status, page])
  useEffect(() => { load() }, [load])

  if (!data) return null
  const remove = async (p) => {
    if (!window.confirm(`'${p.title || '제목 없음'}' 글을 지울까요?`)) return
    await api('DELETE', `/api/posts/${p.id}`)
    load()
    onChange()
  }
  const changeVisibility = async (p, visibility) => {
    await api('PATCH', `/api/posts/${p.id}/visibility`, { visibility })
    load()
    onChange()
  }
  const toPage = (n) => `?tab=${status}&page=${n}`

  if (!data.items.length) return <p className="empty">{status === 'DRAFT' ? '임시저장한 글이 없어요.' : '글이 없어요.'}</p>
  return (
    <>
      <ul className="manage-list">
        {data.items.map((p) => (
          <li key={p.id}>
            <div>
              {p.status === 'PUBLISHED'
                ? <SmartLink to={blogLink(p.blog.address, p.slug)}><strong>{p.title}</strong></SmartLink>
                : <strong>{p.title || '(제목 없음)'}</strong>}
              <p className="meta">{p.status === 'PUBLISHED' ? formatDate(p.publishedAt) : `마지막 저장 ${formatDate(p.updatedAt)}`}
                {p.category && ` · ${p.category.name}`}</p>
            </div>
            <p className="actions">
              <Link to={`/write/${p.id}`}>{p.status === 'DRAFT' ? '이어 쓰기' : '수정'}</Link>
              {p.status === 'PUBLISHED' && VISIBILITIES.filter((v) => v.key !== p.visibility).map((v) => (
                <button key={v.key} className="link" onClick={() => changeVisibility(p, v.key)}>{v.label}로</button>
              ))}
              <button className="link danger" onClick={() => remove(p)}>삭제</button>
            </p>
          </li>
        ))}
      </ul>
      <Pager page={data.page} totalPages={data.totalPages} toPage={toPage} />
    </>
  )
}

function CategoryManager({ address }) {
  const [items, setItems] = useState(null)
  const [name, setName] = useState('')
  const [error, setError] = useState('')

  const [themes, setThemes] = useState([])
  const load = useCallback(() => { api('GET', `/api/blogs/${address}/categories`).then(setItems) }, [address])
  useEffect(() => { load() }, [load])
  useEffect(() => { api('GET', '/api/home/themes').then(setThemes).catch(() => {}) }, [])
  if (!items) return null

  const run = async (fn) => {
    setError('')
    try { await fn(); load() } catch (err) { setError(fieldErrors(err).name ?? fieldErrors(err).ids ?? err.message) }
  }
  const add = (e) => {
    e.preventDefault()
    run(async () => { await api('POST', `/api/blogs/${address}/categories`, { name }); setName('') })
  }
  const rename = (c) => {
    const next = window.prompt('새 이름 (20자 이하)', c.name)
    if (next && next !== c.name) run(() => api('PATCH', `/api/categories/${c.id}`, { name: next }))
  }
  const remove = (c) => {
    if (window.confirm(`'${c.name}' 카테고리를 지울까요? 이 카테고리의 글은 미분류로 옮겨져요.`)) {
      run(() => api('DELETE', `/api/categories/${c.id}`))
    }
  }
  const move = (index, dir) => {
    const ids = items.map((c) => c.id)
    const j = index + dir
    ;[ids[index], ids[j]] = [ids[j], ids[index]]
    run(() => api('PUT', `/api/blogs/${address}/categories/order`, { ids }))
  }

  return (
    <div>
      <p className="hint">'전체 글'은 맨 위, '미분류'는 맨 아래에 자동으로 보여요. 둘은 바꾸거나 지울 수 없어요.</p>
      <ul className="manage-list">
        {items.map((c, i) => (
          <li key={c.id}>
            <strong>{c.name}</strong>
            <p className="actions">
              {/* HOME-05: 이 카테고리로 글을 쓰면 테마가 미리 골라진다 */}
              <select className="category-theme" aria-label={`${c.name} 기본 테마`} value={c.theme ?? ''}
                onChange={(e) => run(() => api('PATCH', `/api/categories/${c.id}/theme`, { theme: e.target.value || null }))}>
                <option value="">기본 테마 없음</option>
                {themes.map((t) => <option key={t.code} value={t.code}>테마: {t.label}</option>)}
              </select>
              <button className="link" disabled={i === 0} onClick={() => move(i, -1)} aria-label={`${c.name} 위로`}>▲</button>
              <button className="link" disabled={i === items.length - 1} onClick={() => move(i, 1)} aria-label={`${c.name} 아래로`}>▼</button>
              <button className="link" onClick={() => rename(c)}>이름 변경</button>
              <button className="link danger" onClick={() => remove(c)}>삭제</button>
            </p>
          </li>
        ))}
      </ul>
      {!items.length && <p className="empty">아직 카테고리가 없어요.</p>}
      <form onSubmit={add} className="inline-form">
        <input aria-label="새 카테고리 이름" value={name} maxLength={20} onChange={(e) => setName(e.target.value)} placeholder="새 카테고리 이름" />
        <button type="submit">추가</button>
      </form>
      {error && <div className="error">{error}</div>}
    </div>
  )
}
