import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { Outlet, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { api } from '../../api'
import { useAuth } from '../../AuthContext'
import { blogLink } from '../../blogUrl'
import SmartLink from '../../components/SmartLink'
import ErrorView from '../../components/ErrorView'
import Avatar, { colorOf } from '../../components/Avatar'

const BlogContext = createContext(null)
export const useBlog = () => useContext(BlogContext)

// BLOG-04: 블로그의 모든 화면에 같은 사이드바가 붙는다
export default function BlogLayout({ address: hostAddress }) {
  const params = useParams()
  const address = hostAddress ?? params.address
  const { me } = useAuth()
  const [sidebar, setSidebar] = useState(null)
  const [error, setError] = useState(null)

  const reload = useCallback(() => {
    api('GET', `/api/blogs/${address}/sidebar`).then(setSidebar).catch(setError)
  }, [address])

  // 로그인 상태가 바뀌면 글 수(비공개 포함 여부)가 달라진다
  useEffect(() => { setError(null); reload() }, [reload, me?.id])

  if (error) return <ErrorView status={error.status} message={error.status === 404 ? '없는 블로그예요.' : undefined} />
  if (!sidebar) return null

  const link = (sub) => blogLink(address, sub)
  const { blog } = sidebar
  return (
    <BlogContext.Provider value={{ address, blog, sidebar, reload, link }}>
      <section className="blog-cover" style={{ '--cover': colorOf(address) }}>
        <SmartLink to={link('')} className="blog-cover-inner">
          <Avatar name={blog.name} src={blog.profileImageUrl} size={84} className="blog-avatar" />
          <div>
            <h1 className="blog-title">{blog.name}</h1>
            {/* 소개가 비면 소개 자리를 숨긴다 (BLOG-02) */}
            {blog.description && <p className="blog-desc">{blog.description}</p>}
          </div>
        </SmartLink>
      </section>
      <div className="blog-layout">
        <section className="blog-content"><Outlet /></section>
        <aside className="sidebar">
          <div className="side-card">
            <SearchBox />
          </div>
          <div className="side-card">
            <h4>카테고리</h4>
            <nav className="categories">
              <SmartLink to={link('')}>전체 글 <span>({sidebar.totalCount})</span></SmartLink>
              {sidebar.categories.map((c) => (
                <SmartLink key={c.id} to={link(`category/${c.id}`)}>{c.name} <span>({c.count})</span></SmartLink>
              ))}
              {sidebar.uncategorizedCount > 0 && (
                <SmartLink to={link('category/none')}>미분류 <span>({sidebar.uncategorizedCount})</span></SmartLink>
              )}
            </nav>
          </div>
          <div className="side-card side-links">
            <SmartLink to={link('tags')}># 태그 모아 보기</SmartLink>
            {blog.isOwner && <SmartLink to={link('edit')}>블로그 정보 수정</SmartLink>}
          </div>
        </aside>
      </div>
    </BlogContext.Provider>
  )
}

// SRCH-01: 앞뒤 공백은 빼고, 공백만 입력하면 검색하지 않는다. 검색어는 남겨 둔다
function SearchBox() {
  const { link } = useBlog()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [q, setQ] = useState(params.get('q') ?? '')
  const [hint, setHint] = useState('')
  const submit = (e) => {
    e.preventDefault()
    const keyword = q.trim()
    if (!keyword) { setHint('검색어를 입력해 주세요'); return }
    setHint('')
    navigate(link(`search?q=${encodeURIComponent(keyword)}`).href)
  }
  return (
    <form onSubmit={submit} className="search" role="search">
      <input aria-label="블로그 검색" placeholder="블로그 안에서 검색" value={q} onChange={(e) => setQ(e.target.value)} />
      <button type="submit">검색</button>
      {hint && <div className="error">{hint}</div>}
    </form>
  )
}
