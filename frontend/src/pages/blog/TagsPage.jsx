import { useEffect, useState } from 'react'
import { api } from '../../api'
import SmartLink from '../../components/SmartLink'
import { useBlog } from './BlogLayout'

// TAG-03 블로그에서 쓴 태그 모아 보기
export default function TagsPage() {
  const { address, link } = useBlog()
  const [tags, setTags] = useState(null)
  useEffect(() => { api('GET', `/api/blogs/${address}/tags`).then(setTags) }, [address])
  if (!tags) return null
  return (
    <div>
      <h2>태그</h2>
      {tags.length === 0 ? <p className="empty">아직 태그가 없어요.</p> : (
        <p className="tags">
          {tags.map((t) => (
            <SmartLink key={t.name} to={link(`tag/${encodeURIComponent(t.name)}`)} className="tag">#{t.name} <small>{t.count}</small></SmartLink>
          ))}
        </p>
      )}
    </div>
  )
}
