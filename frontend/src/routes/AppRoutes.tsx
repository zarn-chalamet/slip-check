import { Navigate, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import Accounts from '../pages/Accounts'
import HistoryPage from '../pages/History'
import Login from '../pages/Login'
import Upload from '../pages/Upload'
import Review from '../pages/Review'
import SettingsPage from '../pages/Settings'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<Upload />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/review" element={<Review />} />
        <Route path="/accounts" element={<Accounts />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}