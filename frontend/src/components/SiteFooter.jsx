import { useAuth } from '../AuthContext'
import { mainLink } from '../blogUrl'
import Logo from './Logo'
import SmartLink from './SmartLink'

// 모든 화면 맨 아래 푸터: 사이트맵(바로가기 모음) + 푸터 메뉴
export default function SiteFooter() {
  const { me } = useAuth()
  const year = new Date().getFullYear()
  return (
    <footer className="site-footer">
      <div className="site-footer-inner">
        <div className="footer-brand">
          <SmartLink to={mainLink('/')} className="logo"><Logo size={28} /> <span>블로그독</span></SmartLink>
          <p>나만의 이야기를 기록하고 나누는 블로그</p>
        </div>
        <nav className="footer-sitemap" aria-label="사이트맵">
          <div>
            <h4>둘러보기</h4>
            <SmartLink to={mainLink('/')}>홈</SmartLink>
            {me && <SmartLink to={mainLink('/feed')}>구독 피드</SmartLink>}
          </div>
          <div>
            <h4>내 블로그</h4>
            {me?.blogAddress ? (
              <>
                <SmartLink to={mainLink(`/blog/${me.blogAddress}`)}>내 블로그</SmartLink>
                <SmartLink to={mainLink('/write')}>글쓰기</SmartLink>
                <SmartLink to={mainLink('/manage')}>글 관리</SmartLink>
              </>
            ) : (
              <SmartLink to={mainLink(me ? '/blog/new' : '/login')}>블로그 만들기</SmartLink>
            )}
          </div>
          <div>
            <h4>계정</h4>
            {me ? (
              <>
                <SmartLink to={mainLink('/settings')}>회원정보 수정</SmartLink>
                <SmartLink to={mainLink('/notifications')}>알림</SmartLink>
                {me.role === 'ADMIN' && <SmartLink to={mainLink('/admin')}>서비스 관리</SmartLink>}
              </>
            ) : (
              <>
                <SmartLink to={mainLink('/login')}>로그인</SmartLink>
                <SmartLink to={mainLink('/signup')}>회원가입</SmartLink>
              </>
            )}
          </div>
        </nav>
      </div>
      <div className="footer-bottom">
        <nav className="footer-menu" aria-label="푸터 메뉴">
          <SmartLink to={mainLink('/')}>블로그독 홈</SmartLink>
          <a href="https://github.com/blog-parkseo/blog-dock" target="_blank" rel="noreferrer">GitHub</a>
          <button type="button" className="link" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>맨 위로 ↑</button>
        </nav>
        <p>© {year} 블로그독 · 박서현</p>
      </div>
    </footer>
  )
}
