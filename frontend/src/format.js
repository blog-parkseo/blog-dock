// 저장은 UTC, 화면은 한국 시간 (공통 정책 7)
const dateFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
})
const dateTimeFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
})

export const formatDate = (iso) => (iso ? dateFmt.format(new Date(iso)) : '')
export const formatDateTime = (iso) => (iso ? dateTimeFmt.format(new Date(iso)) : '')
