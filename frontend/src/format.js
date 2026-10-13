// 저장은 UTC, 화면은 한국 시간 (공통 정책 7)
const dateFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
})
const dateTimeFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
})

export const formatDate = (iso) => (iso ? dateFmt.format(new Date(iso)) : '')
export const formatDateTime = (iso) => (iso ? dateTimeFmt.format(new Date(iso)) : '')

// "방금 전", "3시간 전", "1일 전". 한 달이 넘으면 날짜로 보여 준다
export function timeAgo(iso) {
  if (!iso) return ''
  const sec = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000)
  if (sec < 60) return '방금 전'
  if (sec < 3600) return `${Math.floor(sec / 60)}분 전`
  if (sec < 86400) return `${Math.floor(sec / 3600)}시간 전`
  if (sec < 86400 * 30) return `${Math.floor(sec / 86400)}일 전`
  return formatDate(iso)
}
