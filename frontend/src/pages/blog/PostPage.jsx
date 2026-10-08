import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { api, fieldErrors, newRequestKey } from '../../api'
import { useAuth } from '../../AuthContext'
import { mainLink, postPermalink } from '../../blogUrl'
import { formatDate, formatDateTime } from '../../format'
import ErrorView from '../../components/ErrorView'
import SmartLink from '../../components/SmartLink'
import { useBlog } from './BlogLayout'

// POST-04 글 상세
export default function PostPage() {
  const { slug } = useParams()
  const { address, link, reload: reloadSidebar } = useBlog()
  const { me } = useAuth()
  const navigate = useNavigate()
  const [post, setPost] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    setPost(null)
    setError(null)
    api('GET', `/api/blogs/${address}/posts/${slug}`).then(setPost).catch(setError)
  }, [address, slug, me?.id])

  if (error) return <ErrorView status={error.status} message={error.status === 404 ? '글을 찾을 수 없어요.' : undefined} />
  if (!post) return null

  const remove = async () => {
    if (!window.confirm('이 글을 지울까요? 댓글과 공감도 함께 지워져요.')) return
    try {
      await api('DELETE', `/api/posts/${post.id}`)
      reloadSidebar()
      navigate(link('').href)
    } catch (err) {
      window.alert(err.message)
    }
  }

  return (
    <article className="post">
      <header className="post-head">
        {post.category && <SmartLink to={link(`category/${post.category.id}`)} className="hint">{post.category.name}</SmartLink>}
        <h1>{post.title}</h1>
        <p className="meta">
          {formatDate(post.publishedAt)}
          {post.visibility === 'PRIVATE' && <span className="badge">비공개</span>}
        </p>
        {/* 수정·삭제는 주인에게만 보인다 */}
        {post.isOwner && (
          <p className="actions">
            <SmartLink to={mainLink(`/write/${post.id}`)}>수정</SmartLink>
            <button className="link danger" onClick={remove}>삭제</button>
          </p>
        )}
      </header>
      {/* 서버에서 허용한 태그만 남긴 HTML이라 그대로 그린다 */}
      <div className="post-body" dangerouslySetInnerHTML={{ __html: post.contentHtml }} />
      {post.tags.length > 0 && (
        <p className="tags">
          {post.tags.map((t) => <SmartLink key={t} to={link(`tag/${encodeURIComponent(t)}`)} className="tag">#{t}</SmartLink>)}
        </p>
      )}
      <div className="reactions">
        <LikeButton post={post} onChange={(r) => setPost({ ...post, liked: r.liked, likeCount: r.likeCount })} />
        <ShareButton url={postPermalink(address, post.slug)} />
      </div>
      <nav className="neighbors">
        {post.prev ? <SmartLink to={link(post.prev.slug)}>← 이전 글: {post.prev.title}</SmartLink> : <span />}
        {post.next ? <SmartLink to={link(post.next.slug)}>다음 글: {post.next.title} →</SmartLink> : <span />}
      </nav>
      <Comments postId={post.id} onCount={(n) => setPost((p) => ({ ...p, commentCount: n }))} count={post.commentCount} />
    </article>
  )
}

function useLoginHref() {
  const location = useLocation()
  return mainLink(`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`)
}

// SOC-01 공감: 누르면 추가, 다시 누르면 취소. 비회원은 로그인으로, 자기 글은 막는다
function LikeButton({ post, onChange }) {
  const { me } = useAuth()
  const navigate = useNavigate()
  const loginHref = useLoginHref()
  const [busy, setBusy] = useState(false)
  const click = async () => {
    if (!me) {
      if (loginHref.external) window.location.href = loginHref.href
      else navigate(loginHref.href)
      return
    }
    setBusy(true)
    try {
      onChange(await api(post.liked ? 'DELETE' : 'POST', `/api/posts/${post.id}/like`))
    } catch (err) {
      window.alert(err.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <button onClick={click} disabled={busy || post.isOwner} aria-pressed={post.liked}
      title={post.isOwner ? '내 글에는 공감할 수 없어요' : undefined} className={post.liked ? 'liked' : ''}>
      {post.liked ? '♥' : '♡'} 공감 {post.likeCount}
    </button>
  )
}

// SOC-02 공유: 바뀌지 않는 글 주소를 복사한다
function ShareButton({ url }) {
  const [done, setDone] = useState(false)
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(url)
    } catch {
      window.prompt('주소를 복사해 주세요', url)
    }
    setDone(true)
    setTimeout(() => setDone(false), 2000)
  }
  return <button onClick={copy}>{done ? '주소를 복사했어요' : '공유 (주소 복사)'}</button>
}

// CMT-01~03 댓글
function Comments({ postId, count, onCount }) {
  const { me } = useAuth()
  const loginHref = useLoginHref()
  const [items, setItems] = useState([])
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const keyRef = useRef(newRequestKey())
  const sending = useRef(false) // 화면이 다시 그려지기 전에 또 눌러도 막는다

  const load = useCallback(async () => {
    const list = await api('GET', `/api/posts/${postId}/comments`)
    setItems(list)
    onCount(list.length)
  }, [postId]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { load() }, [load, me?.id])

  const submit = async (e) => {
    e.preventDefault()
    if (sending.current) return
    sending.current = true
    setBusy(true)
    setError('')
    try {
      // 등록을 연달아 눌러도 같은 요청 키라 한 번만 등록된다
      await api('POST', `/api/posts/${postId}/comments`, { content: text }, { headers: { 'Idempotency-Key': keyRef.current } })
      keyRef.current = newRequestKey()
      setText('')
      await load()
    } catch (err) {
      setError(fieldErrors(err).content ?? err.message)
    } finally {
      sending.current = false
      setBusy(false)
    }
  }

  return (
    <section className="comments">
      <h3>댓글 {count}</h3>
      <ul>
        {items.map((c) => <CommentItem key={c.id} comment={c} onChanged={load} />)}
      </ul>
      {me ? (
        <form onSubmit={submit}>
          <label htmlFor="comment" className="sr-only">댓글</label>
          <textarea id="comment" rows={3} maxLength={1000} value={text} onChange={(e) => setText(e.target.value)}
            placeholder="댓글을 남겨 보세요 (1000자 이하)" />
          {error && <div className="error">{error}</div>}
          <p><button type="submit" className="primary" disabled={busy}>{busy ? '등록 중…' : '등록'}</button></p>
        </form>
      ) : (
        <p className="hint"><SmartLink to={loginHref}>로그인</SmartLink>하고 댓글을 남겨 보세요.</p>
      )}
    </section>
  )
}

function CommentItem({ comment, onChanged }) {
  const [editing, setEditing] = useState(false)
  const [text, setText] = useState(comment.content)
  const [error, setError] = useState('')

  const save = async () => {
    try {
      await api('PATCH', `/api/comments/${comment.id}`, { content: text })
      setEditing(false)
      onChanged()
    } catch (err) {
      setError(fieldErrors(err).content ?? err.message)
    }
  }
  const remove = async () => {
    if (!window.confirm('댓글을 지울까요?')) return
    try {
      await api('DELETE', `/api/comments/${comment.id}`)
      onChanged()
    } catch (err) {
      window.alert(err.message)
    }
  }

  return (
    <li className="comment">
      <p className="meta"><strong>{comment.nickname}</strong> · {formatDateTime(comment.createdAt)}</p>
      {editing ? (
        <>
          <textarea rows={3} maxLength={1000} value={text} onChange={(e) => setText(e.target.value)} aria-label="댓글 수정" />
          {error && <div className="error">{error}</div>}
          <p className="actions"><button onClick={save}>저장</button> <button className="link" onClick={() => { setEditing(false); setText(comment.content) }}>취소</button></p>
        </>
      ) : (
        <p className="comment-body">{comment.content}</p>
      )}
      {!editing && (
        <p className="actions">
          {comment.canEdit && <button className="link" onClick={() => setEditing(true)}>수정</button>}
          {comment.canDelete && <button className="link danger" onClick={remove}>삭제</button>}
        </p>
      )}
    </li>
  )
}
