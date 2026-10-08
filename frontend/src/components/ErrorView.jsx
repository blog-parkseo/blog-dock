import { Link, useNavigate } from 'react-router-dom'

// COM-02: 404·403·그 밖의 오류를 구분해서 알리고 돌아갈 길을 준다
const MESSAGES = {
  403: '이 화면을 볼 권한이 없어요.',
  404: '찾는 페이지가 없어요.',
}

export default function ErrorView({ status, message }) {
  const navigate = useNavigate()
  return (
    <div className="panel narrow center error-view">
      <h2 className="error-code">{status === 403 || status === 404 ? status : '오류'}</h2>
      <p>{message ?? MESSAGES[status] ?? '잠시 후 다시 시도해 주세요.'}</p>
      <button onClick={() => navigate(-1)}>이전 화면</button>{' '}
      <Link to="/" className="button primary">홈으로</Link>
    </div>
  )
}
