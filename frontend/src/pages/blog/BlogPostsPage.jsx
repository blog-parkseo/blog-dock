import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { api } from '../../api'
import ErrorView from '../../components/ErrorView'
import PostList, { Pager } from '../../components/PostList'
import { useBlog } from './BlogLayout'

// BLOG-03 블로그 메인 / CAT-02 카테고리별 / TAG-02 태그별 / SRCH-01 검색
export default function BlogPostsPage() {
  const { address, sidebar } = useBlog()
  const { categoryId, tag } = useParams()
  const [params, setParams] = useSearchParams()
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0)
  const q = params.get('q')
  const isSearch = q !== null
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (isSearch && !q.trim()) { setData({ items: [], page: 0, totalPages: 0 }); return }
    const query = new URLSearchParams({ page: String(page) })
    if (categoryId) query.set('category', categoryId)
    if (tag) query.set('tag', tag)
    if (isSearch) query.set('q', q)
    setError(null)
    api('GET', `/api/blogs/${address}/posts?${query}`).then(setData).catch(setError)
  }, [address, categoryId, tag, q, isSearch, page])

  if (error) return <ErrorView status={error.status} />
  if (!data) return null

  let title = '전체 글'
  if (categoryId === 'none') title = '미분류'
  else if (categoryId) title = sidebar.categories.find((c) => String(c.id) === categoryId)?.name ?? '카테고리'
  else if (tag) title = `#${tag}`
  else if (isSearch) title = `'${q}' 검색 결과`

  const toPage = (n) => {
    const next = new URLSearchParams(params)
    next.set('page', String(n))
    return `?${next}`
  }
  return (
    <div>
      <h2>{title} {data.totalElements !== undefined && <small className="hint">{data.totalElements}개</small>}</h2>
      <PostList items={data.items} empty={isSearch ? '검색 결과가 없어요.' : page > 0 ? '이 페이지에는 글이 없어요.' : '아직 글이 없어요.'} />
      <Pager page={data.page} totalPages={data.totalPages} toPage={toPage} />
      {page > 0 && !data.items.length && <button onClick={() => setParams({})}>첫 페이지로</button>}
    </div>
  )
}
