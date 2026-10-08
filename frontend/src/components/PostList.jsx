import { Link } from 'react-router-dom'
import { blogLink } from '../blogUrl'
import { formatDate } from '../format'
import SmartLink from './SmartLink'

// 글 목록 한 묶음: 제목·발행일·카테고리·요약, 대표 이미지는 썸네일로 (BLOG-03, HOME-01)
export default function PostList({ items, showBlog = false, empty = '아직 글이 없어요.' }) {
  if (!items.length) return <p className="empty">{empty}</p>
  return (
    <ul className="post-list">
      {items.map((p) => (
        <li key={p.id}>
          <SmartLink to={blogLink(p.blog.address, p.slug)} className="post-item">
            <div className="post-text">
              <h3>{p.title}</h3>
              <p className="excerpt">{p.excerpt}</p>
              <p className="meta">
                {showBlog && <span>{p.blog.name} · </span>}
                {formatDate(p.publishedAt)}
                {p.category && <span> · {p.category.name}</span>}
                {p.visibility === 'PRIVATE' && <span className="badge">비공개</span>}
              </p>
            </div>
            {p.thumbnailUrl && <img className="thumb" src={p.thumbnailUrl} alt="" loading="lazy" />}
          </SmartLink>
        </li>
      ))}
    </ul>
  )
}

// 블로그 목록 페이지 번호 (한 페이지 10개)
export function Pager({ page, totalPages, toPage }) {
  if (totalPages <= 1) return null
  return (
    <nav className="pager" aria-label="페이지">
      {page > 0 && <Link to={toPage(page - 1)}>이전</Link>}
      <span>{page + 1} / {totalPages}</span>
      {page + 1 < totalPages && <Link to={toPage(page + 1)}>다음</Link>}
    </nav>
  )
}
