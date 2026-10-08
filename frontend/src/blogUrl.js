// 블로그 주소 만들기.
// VITE_BLOG_DOMAIN(예: blogdock.localhost)을 정하면 블로그를 "주소.blogdock.localhost"로 연다.
// 정하지 않으면 /blog/주소 경로를 쓴다.
export const BLOG_DOMAIN = import.meta.env.VITE_BLOG_DOMAIN || ''

const port = () => (window.location.port ? `:${window.location.port}` : '')

// 지금 블로그 서브도메인에 있으면 그 블로그 주소, 아니면 null
export function hostBlogAddress() {
  if (!BLOG_DOMAIN) return null
  const host = window.location.hostname
  if (!host.endsWith(`.${BLOG_DOMAIN}`)) return null
  const sub = host.slice(0, -(BLOG_DOMAIN.length + 1))
  return sub && !sub.includes('.') ? sub : null
}

const clean = (sub) => String(sub ?? '').replace(/^\/+/, '')

// 블로그 안의 화면 주소. { href, external }: external이면 <a>, 아니면 <Link>로 연다
export function blogLink(address, sub = '') {
  const path = clean(sub)
  if (!BLOG_DOMAIN) return { href: `/blog/${address}${path ? `/${path}` : ''}`, external: false }
  if (hostBlogAddress() === address) return { href: `/${path}`, external: false }
  return { href: `${window.location.protocol}//${address}.${BLOG_DOMAIN}${port()}/${path}`, external: true }
}

// 바뀌지 않는 글 주소 전체 (SOC-02 공유)
export function postPermalink(address, slug) {
  const { href, external } = blogLink(address, slug)
  return external ? href : `${window.location.origin}${href}`
}

// 블로그가 아닌 화면(로그인, 글쓰기, 관리...) 주소. 블로그 서브도메인에서는 메인 주소로 간다
export function mainLink(path) {
  if (!hostBlogAddress()) return { href: path, external: false }
  return { href: `${window.location.protocol}//${BLOG_DOMAIN}${port()}${path}`, external: true }
}
