import { useState ,Suspense,lazy} from 'react'
import viteLogo from '/vite.svg'
import {Route,Routes} from 'react-router-dom'
const ProtectedRoute = lazy(() => import('./components/auth/ProtectedRoute'));
const LandingPage = lazy(() => import('./pages/LandingPage'));
const LoginPage = lazy(() => import('./pages/LoginPage'));
const SignupPage = lazy(() => import('./pages/SignupPage'));
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const EditorPage = lazy(()=>import('./pages/EditorPage'));
const ViewBookPage = lazy(()=>import('./pages/ViewBookPage'));  
const ProfilePage = lazy(()=>import('./pages/ProfilePage'));
function App() {
  return (
    <>
    <Suspense fallback={<div>Loading...</div>}>
       <Routes>
        <Route path="/" element={<LandingPage/>}></Route>
        <Route path="/login" element={<LoginPage/>}></Route>
        <Route path="/signup" element={<SignupPage/>}></Route>
        <Route
        path="/dashboard"
        element={<ProtectedRoute><DashboardPage/></ProtectedRoute>}>
      </Route>
      <Route path='/editor/:bookId' 
          element={<ProtectedRoute><EditorPage/></ProtectedRoute>} />
      <Route path='/view-book/:bookId' element={<ProtectedRoute><ViewBookPage/></ProtectedRoute>} />
      <Route path='/profile' element={<ProtectedRoute><ProfilePage/></ProtectedRoute>} />
    </Routes>
    </Suspense>
    
    </>
  )
}

export default App
