import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api } from './api'

// 지금 로그인한 사람(me)을 모든 화면에서 쓸 수 있게 한다
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [me, setMe] = useState(null)       // null이면 비회원
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    try {
      setMe(await api('GET', '/api/me'))
    } catch {
      setMe(null) // 401이면 비회원
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])

  const login = async (nickname) => setMe(await api('POST', '/api/auth/dev-login', { nickname }))
  const logout = async () => { await api('POST', '/api/auth/logout'); setMe(null) }

  return (
    <AuthContext.Provider value={{ me, loading, refresh, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
