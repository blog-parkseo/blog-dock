import { Suspense, lazy, useEffect } from 'react'
import { Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import { BLOG_DOMAIN, hostBlogAddress, mainLink } from './blogUrl'
import SmartLink from './components/SmartLink'
import RequireLogin from './components/RequireLogin'
import ErrorView from './components/ErrorView'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import BlogCreatePage from './pages/BlogCreatePage'
import BlogLayout from './pages/blog/BlogLayout'
import BlogPostsPage from './pages/blog/BlogPostsPage'
import PostPage from './pages/blog/PostPage'
import TagsPage from './pages/blog/TagsPage'
import BlogEditPage from './pages/BlogEditPage'
import ManagePage from './pages/ManagePage'
import SettingsPage from './pages/SettingsPage'
import AdminPage from './pages/AdminPage'

// 에디터가 커서 글쓰기 화면에 들어갈 때만 불러온다
const WritePage = lazy(() => import('./pages/WritePage'))

function Header() {
  const { me, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const onBlogHost = Boolean(hostBlogAddress())

  // AUTH-04: 블로그가 없으면 개설 화면, 있으면 글쓰기로
  const writeTo = mainLink(me?.blogAddress ? '/write' : '/blog/new')
  const loginTo = mainLink(`/login?redirect=${encodeURIComponent(onBlogHost ? '/' : location.pathname + location.search)}`)

  return (
    <header className="site-header">
      <SmartLink to={mainLink('/')} className="logo">블로그독</SmartLink>
      <span className="spacer" />
      {me ? (
        <>
          {me.blogAddress && <SmartLink to={mainLink(`/blog/${me.blogAddress}`)}>내 블로그</SmartLink>}
          <SmartLink to={writeTo} className="button primary">글쓰기</SmartLink>
          {me.blogAddress && <SmartLink to={mainLink('/manage')}>관리</SmartLink>}
          {me.role === 'ADMIN' && <SmartLink to={mainLink('/admin')}>서비스 관리</SmartLink>}
          <SmartLink to={mainLink('/settings')} className="me">{me.nickname}님</SmartLink>
          <button className="link" onClick={async () => { await logout(); navigate('/') }}>로그아웃</button>
        </>
      ) : (
        <SmartLink to={loginTo}>로그인</SmartLink>
      )}
    </header>
  )
}

// 블로그 안의 화면들. /blog/주소/... 와 주소.blogdock.localhost/... 가 같이 쓴다
const blogChildren = (
  <>
    <Route index element={<BlogPostsPage />} />
    <Route path="category/:categoryId" element={<BlogPostsPage />} />
    <Route path="tag/:tag" element={<BlogPostsPage />} />
    <Route path="search" element={<BlogPostsPage />} />
    <Route path="tags" element={<TagsPage />} />
    <Route path="edit" element={<RequireLogin><BlogEditPage /></RequireLogin>} />
    <Route path=":slug" element={<PostPage />} />
  </>
)

// 블로그 서브도메인을 켰는데 /blog/주소 로 들어오면 서브도메인으로 옮긴다
function GoToBlogHost() {
  const location = useLocation()
  useEffect(() => {
    const [, , address, ...rest] = location.pathname.split('/')
    window.location.replace(`${window.location.protocol}//${address}.${BLOG_DOMAIN}${window.location.port ? `:${window.location.port}` : ''}/${rest.join('/')}${location.search}`)
  }, [location])
  return null
}

export default function App() {
  const hostAddress = hostBlogAddress()
  return (
    <>
      <Header />
      <main>
        <Suspense fallback={<p className="hint">불러오는 중…</p>}>
        {hostAddress ? (
          <Routes>
            <Route path="/" element={<BlogLayout address={hostAddress} />}>{blogChildren}</Route>
          </Routes>
        ) : (
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/blog/new" element={<RequireLogin><BlogCreatePage /></RequireLogin>} />
            {BLOG_DOMAIN
              ? <Route path="/blog/:address/*" element={<GoToBlogHost />} />
              : <Route path="/blog/:address" element={<BlogLayout />}>{blogChildren}</Route>}
            <Route path="/write" element={<RequireLogin><WritePage /></RequireLogin>} />
            <Route path="/write/:postId" element={<RequireLogin><WritePage /></RequireLogin>} />
            <Route path="/manage" element={<RequireLogin><ManagePage /></RequireLogin>} />
            <Route path="/settings" element={<RequireLogin><SettingsPage /></RequireLogin>} />
            <Route path="/admin" element={<RequireLogin><AdminPage /></RequireLogin>} />
            <Route path="*" element={<ErrorView status={404} />} />
          </Routes>
        )}
        </Suspense>
      </main>
    </>
  )
}
