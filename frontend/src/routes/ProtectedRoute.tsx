import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../hook/useAuth'
import { Layout } from '../components/layout/Layout'

export function ProtectedRoute() {
  const { seller, loading } = useAuth()

  if (loading) {
    return (
      <div className="grid min-h-screen place-items-center">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-emerald-400 border-t-transparent" />
      </div>
    )
  }
  if (!seller) return <Navigate to="/login" replace />

  return (
    <Layout>
      <Outlet />
    </Layout>
  )
}