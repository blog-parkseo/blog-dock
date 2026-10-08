import { Link } from 'react-router-dom'
import { blogLink } from '../blogUrl'
import { formatDate } from '../format'
import Avatar, { colorOf } from './Avatar'
import SmartLink from './SmartLink'

// 글 목록. 홈은 카드 격자(grid), 블로그 안은 한 줄씩(rows)
// 제목·발행일·카테고리·요약, 대표 이미지는 썸네일로 (BLOG-03, HOME-01)
export default function PostList({ items, showBlog = false, layout = 'rows', empty = '아직 글이 없어요.' }) {
  if (!items.length) return <EmptyState text={empty} />
  if (layout === 'grid') {
    return (
      <ul className="post-grid">
        {items.map((p) => (
          <li key={p.id}>
            <SmartLink to={blogLink(p.blog.address, p.slug)} className="post-card">
              <div className="post-card-cover" style={p.thumbnailUrl ? undefined : { '--cover': colorOf(p.blog.address) }}>
                {p.thumbnailUrl ? <img src={p.thumbnailUrl} alt="" loading="lazy" /> : <span>{[...p.title][0]}</span>}
              </div>
              <div className="post-card-body">
                {p.category && <span className="eyebrow">{p.category.name}</span>}
                <h3>{p.title}</h3>
                <p className="excerpt">{p.excerpt}</p>
                <p className="byline">
                  {showBlog && <><Avatar name={p.blog.name} size={22} /><span className="blog-name">{p.blog.name}</span><span className="dot">·</span></>}
                  <time>{formatDate(p.publishedAt)}</time>
                </p>
              </div>
            </SmartLink>
          </li>
        ))}
      </ul>
    )
  }
  return (
    <ul className="post-list">
      {items.map((p) => (
        <li key={p.id}>
          <SmartLink to={blogLink(p.blog.address, p.slug)} className="post-item">
            <div className="post-text">
              {p.category && <span className="eyebrow">{p.category.name}</span>}
              <h3>{p.title}</h3>
              <p className="excerpt">{p.excerpt}</p>
              <p className="meta">
                {showBlog && <span>{p.blog.name} · </span>}
                <time>{formatDate(p.publishedAt)}</time>
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

export function EmptyState({ text, children }) {
  return (
    <div className="empty">
      <svg width="48" height="48" viewBox="0 0 64 64" aria-hidden="true">
        <g fill="currentColor" opacity=".35">
          <ellipse cx="32" cy="40" rx="12" ry="10" /><ellipse cx="18" cy="26" rx="5" ry="6.5" /><ellipse cx="46" cy="26" rx="5" ry="6.5" />
          <ellipse cx="26" cy="16" rx="4.5" ry="6" /><ellipse cx="38" cy="16" rx="4.5" ry="6" />
        </g>
      </svg>
      <p>{text}</p>
      {children}
    </div>
  )
}

// 블로그 목록 페이지 번호 (한 페이지 10개)
export function Pager({ page, totalPages, toPage }) {
  if (totalPages <= 1) return null
  return (
    <nav className="pager" aria-label="페이지">
      {page > 0 ? <Link className="button" to={toPage(page - 1)}>← 이전</Link> : <span />}
      <span className="pager-count">{page + 1} / {totalPages}</span>
      {page + 1 < totalPages ? <Link className="button" to={toPage(page + 1)}>다음 →</Link> : <span />}
    </nav>
  )
}
