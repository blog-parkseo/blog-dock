// 로그인 전에 보던 화면으로, 없으면 홈으로. 다른 사이트 주소는 받지 않는다
export function safeRedirect(params) {
  const redirect = params.get('redirect')
  return redirect && redirect.startsWith('/') && !redirect.startsWith('//') && !redirect.includes('\\') ? redirect : '/'
}

// 로그인·회원가입 화면끼리 오갈 때 돌아갈 곳을 그대로 넘긴다
export const withRedirect = (path, params) =>
  params.get('redirect') ? `${path}?redirect=${encodeURIComponent(params.get('redirect'))}` : path
