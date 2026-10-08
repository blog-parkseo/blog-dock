import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import ErrorView from '../components/ErrorView'

// BLOG-03 블로그 메인 (글 목록은 2단계에서 붙인다)
export default function BlogPage() {
  const { address } = useParams()
  const [blog, setBlog] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api('GET', `/api/blogs/${address}`).then(setBlog).catch(setError)
  }, [address])

  if (error) return <ErrorView status={error.status} />
  if (!blog) return null

  return (
    <div>
      <img src={blog.profileImageUrl ?? '/favicon.svg'} alt="" width={64} height={64} />
      <h1>{blog.name}</h1>
      {/* 소개가 비면 소개 자리를 숨긴다 (BLOG-02) */}
      {blog.description && <p>{blog.description}</p>}
      {blog.isOwner && <Link to={`/blog/${address}/edit`}>블로그 정보 수정</Link>}
      <p className="hint">아직 글이 없어요.</p>
    </div>
  )
}
