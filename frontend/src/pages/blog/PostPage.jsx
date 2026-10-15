import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { api, fieldErrors, newRequestKey } from '../../api'
import { useAuth } from '../../AuthContext'
import { mainLink, postPermalink } from '../../blogUrl'
import { formatDate, formatDateTime } from '../../format'
import ErrorView from '../../components/ErrorView'
import SmartLink from '../../components/SmartLink'
import Avatar from '../../components/Avatar'
import SubscribeButton from '../../components/SubscribeButton'
import { VisibilityBadge } from '../../components/PostList'
import { useBlog } from './BlogLayout'

// POST-04 글 상세
export default function PostPage() {
  const { slug } = useParams()
  const { address, link, blog, setSubscription, reload: reloadSidebar } = useBlog()
  const { me } = useAuth()
  const navigate = useNavigate()
  const [post, setPost] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    api('GET', `/api/blogs/${address}/posts/${slug}`).then(setPost).catch(setError)
  }, [address, slug])

  useEffect(() => {
    setPost(null)
    setError(null)
    load()
  }, [load, me?.id])

  if (error) return <ErrorView status={error.status} message={error.status === 404 ? '글을 찾을 수 없어요.' : undefined} />
  if (!post) return null
  if (post.locked) {
    // POST-12: 구독하면 바로 본문을 다시 불러온다
    return <LockedPost post={post} blog={blog} address={address}
      onSubscribe={(s) => { setSubscription(s); if (s.subscribed) load() }} />
  }

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
        {(post.theme || post.category) && <p className="post-labels">
          {post.theme && <SmartLink to={mainLink(`/?theme=${post.theme.code}`)} className="theme-chip">{post.theme.emoji} {post.theme.label}</SmartLink>}
          {post.category && <SmartLink to={link(`category/${post.category.id}`)} className="eyebrow">{post.category.name}</SmartLink>}
        </p>}
        <h1>{post.title}</h1>
        <p className="meta">
          <Avatar name={post.blog.name} size={26} />
          <span>{post.blog.name}</span><span className="dot">·</span>
          <time>{formatDate(post.publishedAt)}</time>
          <VisibilityBadge visibility={post.visibility} />
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
      {/* SUB-01: 글을 다 읽은 자리에서도 구독할 수 있게 */}
      <section className="author-box">
        <SmartLink to={link('')} className="author">
          <Avatar name={blog.name} src={blog.profileImageUrl} size={48} />
          <span>
            <strong>{blog.name}</strong>
            <small>구독자 {blog.subscriberCount}명</small>
          </span>
        </SmartLink>
        <SubscribeButton blog={blog} address={address} onChange={setSubscription} />
      </section>
      <nav className="neighbors">
        {post.prev ? <Neighbor post={post.prev} to={link(post.prev.slug)} label="← 이전 글" /> : <span />}
        {post.next ? <Neighbor post={post.next} to={link(post.next.slug)} label="다음 글 →" /> : <span />}
      </nav>
      <Comments postId={post.id} onCount={(n) => setPost((p) => ({ ...p, commentCount: n }))} count={post.commentCount} />
    </article>
  )
}

// POST-12 구독하지 않은 사람이 구독자 공개 글을 열었을 때. 제목과 블로그 이름만 보여 준다
function LockedPost({ post, blog, address, onSubscribe }) {
  return (
    <article className="post">
      <header className="post-head">
        <h1>{post.title}</h1>
        <p className="meta">
          <Avatar name={post.blog.name} size={26} />
          <span>{post.blog.name}</span><span className="dot">·</span>
          <time>{formatDate(post.publishedAt)}</time>
          <VisibilityBadge visibility={post.visibility} />
        </p>
      </header>
      <section className="locked">
        <p className="locked-icon" aria-hidden="true">🔒</p>
        <p><strong>구독자만 볼 수 있는 글이에요</strong></p>
        <p className="meta">'{blog.name}'을 구독하면 바로 읽을 수 있어요.</p>
        <SubscribeButton blog={blog} address={address} onChange={onSubscribe} />
      </section>
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
      title={post.isOwner ? '내 글에는 공감할 수 없어요' : undefined} className={`pill${post.liked ? ' liked' : ''}`}>
      <span className="heart">{post.liked ? '♥' : '♡'}</span> 공감 {post.likeCount}
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
  return <button className="pill" onClick={copy}>{done ? '✓ 주소를 복사했어요' : '🔗 공유 (주소 복사)'}</button>
}

// CMT-01~03 댓글 + 비밀 댓글 (글 주인과 쓴 사람만 봄) + 답글 (한 단계, 원래 댓글 밑에 들여쓰기)
function Comments({ postId, count, onCount }) {
  const { me } = useAuth()
  const loginHref = useLoginHref()
  const [items, setItems] = useState([])
  const [text, setText] = useState('')
  const [secret, setSecret] = useState(false)
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
      await api('POST', `/api/posts/${postId}/comments`, { content: text, secret }, { headers: { 'Idempotency-Key': keyRef.current } })
      keyRef.current = newRequestKey()
      setText('')
      setSecret(false)
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
      <ul className="comment-list">
        {items.filter((c) => !c.parentId).map((c) => (
          <CommentItem key={c.id} comment={c} replies={items.filter((r) => r.parentId === c.id)} onChanged={load} />
        ))}
      </ul>
      {me ? (
        <form onSubmit={submit}>
          <label htmlFor="comment" className="sr-only">댓글</label>
          <textarea id="comment" rows={3} maxLength={1000} value={text} onChange={(e) => setText(e.target.value)}
            placeholder="댓글을 남겨 보세요 (1000자 이하)" />
          {error && <div className="error">{error}</div>}
          <p>
            <label className="secret-check">
              <input type="checkbox" checked={secret} onChange={(e) => setSecret(e.target.checked)} /> 🔒 비밀 댓글
            </label>
            <button type="submit" className="primary" disabled={busy}>{busy ? '등록 중…' : '등록'}</button>
          </p>
        </form>
      ) : (
        <p className="comment-login"><SmartLink to={loginHref}>로그인</SmartLink>하고 댓글을 남겨 보세요.</p>
      )}
    </section>
  )
}

function CommentItem({ comment, replies = [], onChanged, onReply }) {
  const { me } = useAuth()
  const [editing, setEditing] = useState(false)
  const [replying, setReplying] = useState(false)
  const [text, setText] = useState(comment.content)
  const [secret, setSecret] = useState(comment.secret)
  const [error, setError] = useState('')
  const isReply = !!comment.parentId
  const hidden = comment.secret && comment.content == null
  // 답글의 답글 버튼은 원래 댓글의 답글 칸을 연다 (답글은 한 단계만)
  const openReply = isReply ? onReply : () => setReplying(true)

  const save = async () => {
    try {
      await api('PATCH', `/api/comments/${comment.id}`, { content: text, secret })
      setEditing(false)
      onChanged()
    } catch (err) {
      setError(fieldErrors(err).content ?? err.message)
    }
  }
  const remove = async () => {
    const ask = !isReply && replies.length ? '댓글을 지우면 달린 답글도 같이 지워져요. 지울까요?' : '댓글을 지울까요?'
    if (!window.confirm(ask)) return
    try {
      await api('DELETE', `/api/comments/${comment.id}`)
      onChanged()
    } catch (err) {
      window.alert(err.message)
    }
  }

  const replyList = !isReply && (replies.length > 0 || replying) && (
    <ul className="reply-list">
      {replies.map((r) => <CommentItem key={r.id} comment={r} onChanged={onChanged} onReply={() => setReplying(true)} />)}
      {replying && (
        <ReplyForm parent={comment} onDone={() => { setReplying(false); onChanged() }} onCancel={() => setReplying(false)} />
      )}
    </ul>
  )

  // 비밀 댓글인데 내용을 못 받으면 글 주인·쓴 사람이 아니라서 가려진 것
  if (hidden) {
    return (
      <li className={`comment secret-hidden${isReply ? ' reply' : ''}`}>
        <Avatar name="🔒" size={isReply ? 30 : 36} />
        <div className="comment-main">
          <p className="comment-head"><strong>{isReply ? '비밀 답글' : '비밀 댓글'}</strong> <time>{formatDateTime(comment.createdAt)}</time></p>
          <p className="comment-body">글쓴이만 볼 수 있는 {isReply ? '답글' : '댓글'}이에요.</p>
          {replyList}
        </div>
      </li>
    )
  }

  return (
    <li className={`comment${isReply ? ' reply' : ''}`}>
      <Avatar name={comment.nickname} src={comment.profileImageUrl} size={isReply ? 30 : 36} />
      <div className="comment-main">
      <p className="comment-head">
        <strong>{comment.nickname}</strong>
        {comment.byPostOwner && <span className="writer-badge">작성자</span>}
        {comment.secret && <span className="secret-badge">🔒 비밀</span>}
        <time>{formatDateTime(comment.createdAt)}</time>
      </p>
      {editing ? (
        <>
          <textarea rows={3} maxLength={1000} value={text} onChange={(e) => setText(e.target.value)} aria-label="댓글 수정" />
          {error && <div className="error">{error}</div>}
          <p className="actions">
            {!isReply && <label className="secret-check"><input type="checkbox" checked={secret} onChange={(e) => setSecret(e.target.checked)} /> 🔒 비밀 댓글</label>}
            <button onClick={save}>저장</button> <button className="link" onClick={() => { setEditing(false); setText(comment.content); setSecret(comment.secret) }}>취소</button>
          </p>
        </>
      ) : (
        <p className="comment-body">{comment.content}</p>
      )}
      {!editing && (
        <p className="actions">
          {me && <button className="link" onClick={openReply}>답글</button>}
          {comment.canEdit && <button className="link" onClick={() => setEditing(true)}>수정</button>}
          {comment.canDelete && <button className="link danger" onClick={remove}>삭제</button>}
        </p>
      )}
      {replyList}
      </div>
    </li>
  )
}

// 답글 쓰는 칸. 비밀 댓글에 다는 답글은 서버가 늘 비밀로 저장한다
function ReplyForm({ parent, onDone, onCancel }) {
  const [text, setText] = useState('')
  const [secret, setSecret] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const keyRef = useRef(newRequestKey())
  const sending = useRef(false)

  const submit = async (e) => {
    e.preventDefault()
    if (sending.current) return
    sending.current = true
    setBusy(true)
    setError('')
    try {
      await api('POST', `/api/comments/${parent.id}/replies`, { content: text, secret },
        { headers: { 'Idempotency-Key': keyRef.current } })
      onDone()
    } catch (err) {
      setError(fieldErrors(err).content ?? err.message)
    } finally {
      sending.current = false
      setBusy(false)
    }
  }

  return (
    <li className="reply-form">
      <form onSubmit={submit}>
        <label htmlFor={`reply-${parent.id}`} className="sr-only">답글</label>
        <textarea id={`reply-${parent.id}`} rows={2} maxLength={1000} value={text} autoFocus
          onChange={(e) => setText(e.target.value)} placeholder={parent.nickname ? `${parent.nickname}님에게 답글 남기기` : '답글 남기기'} />
        {error && <div className="error">{error}</div>}
        <p>
          {parent.secret
            ? <span className="secret-check">🔒 비밀 댓글의 답글이라 비밀로 남아요</span>
            : <label className="secret-check"><input type="checkbox" checked={secret} onChange={(e) => setSecret(e.target.checked)} /> 🔒 비밀 답글</label>}
          <button type="button" className="link" onClick={onCancel}>취소</button>
          <button type="submit" className="primary" disabled={busy}>{busy ? '등록 중…' : '답글 등록'}</button>
        </p>
      </form>
    </li>
  )
}

// 이전·다음 글 칸: 제목(두 줄까지) + 공감·댓글 수·날짜, 오른쪽에 썸네일
function Neighbor({ post, to, label }) {
  return (
    <SmartLink to={to} className="neighbor">
      <div className="neighbor-text">
        <small>{label}</small>
        <span className="neighbor-title">{post.title}</span>
        <p className="neighbor-meta">
          <span aria-label={`공감 ${post.likeCount}`}><HeartIcon />{post.likeCount}</span>
          <span aria-label={`댓글 ${post.commentCount}`}><CommentIcon />{post.commentCount}</span>
          <time>{dotDate(post.publishedAt)}</time>
        </p>
      </div>
      {post.thumbnailUrl && <img className="neighbor-thumb" src={post.thumbnailUrl} alt="" loading="lazy" />}
    </SmartLink>
  )
}

// 2026. 08. 28. → 2026.08.28
const dotDate = (iso) => formatDate(iso).replace(/\.\s*/g, '.').replace(/\.$/, '')

function HeartIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 20.5s-7.5-4.6-9.3-9.4C1.6 8.2 3.5 4.5 7.2 4.5c2 0 3.6 1.1 4.8 2.8 1.2-1.7 2.8-2.8 4.8-2.8 3.7 0 5.6 3.7 4.5 6.6-1.8 4.8-9.3 9.4-9.3 9.4z" />
    </svg>
  )
}

function CommentIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 5.5h16v10.5H10l-4 3.5V16H4z" />
      <path d="M8.5 11h.01M12 11h.01M15.5 11h.01" strokeWidth="2.4" strokeLinecap="round" />
    </svg>
  )
}
