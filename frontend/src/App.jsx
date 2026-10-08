import { Link, Route, Routes, useNavigate } from 'react-router-dom'
import { useAuth } from './AuthContext'
import RequireLogin from './components/RequireLogin'
import ErrorView from './components/ErrorView'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import BlogCreatePage from './pages/BlogCreatePage'
import BlogPage from './pages/BlogPage'
import BlogEditPage from './pages/BlogEditPage'
import WritePage from './pages/WritePage'
import AdminPage from './pages/AdminPage'

function Header() {
  const { me, logout } = useAuth()
  const navigate = useNavigate()

  // AUTH-04: 블로그가 없으면 개설 화면, 있으면 글쓰기로
  const goWrite = () => navigate(me?.blogAddress ? '/write' : '/blog/new')

  return (
    <header>
      <Link to="/">블로그독</Link>
      <span className="spacer" />
      {me ? (
        <>
          {me.blogAddress && <Link to={`/blog/${me.blogAddress}`}>내 블로그</Link>}
          <button onClick={goWrite}>글쓰기</button>
          <span>{me.nickname}님</span>
          <button onClick={async () => { await logout(); navigate('/') }}>로그아웃</button>
        </>
      ) : (
        <Link to="/login">로그인</Link>
      )}
    </header>
  )
}

export default function App() {
  return (
    <>
      <Header />
      <main>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/blog/new" element={<RequireLogin><BlogCreatePage /></RequireLogin>} />
          <Route path="/blog/:address" element={<BlogPage />} />
          <Route path="/blog/:address/edit" element={<RequireLogin><BlogEditPage /></RequireLogin>} />
          <Route path="/write" element={<RequireLogin><WritePage /></RequireLogin>} />
          <Route path="/admin" element={<RequireLogin><AdminPage /></RequireLogin>} />
          <Route path="*" element={<ErrorView status={404} />} />
        </Routes>
      </main>
    </>
  )
}
