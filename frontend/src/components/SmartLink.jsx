import { Link } from 'react-router-dom'

// 같은 사이트 안이면 <Link>, 다른 주소(다른 블로그 서브도메인 등)면 <a>
export default function SmartLink({ to, children, ...rest }) {
  if (to.external) return <a href={to.href} {...rest}>{children}</a>
  return <Link to={to.href} {...rest}>{children}</Link>
}
