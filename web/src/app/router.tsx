import { createBrowserRouter } from 'react-router'
import { RequireAuth } from '../features/auth/RequireAuth'
import { ComingSoonPage } from '../pages/ComingSoonPage'
import { ExploreProblemsPage } from '../pages/ExploreProblemsPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { MyProblemsPage } from '../pages/MyProblemsPage'
import { MyProfilePage } from '../pages/MyProfilePage'
import { ProblemDetailPage } from '../pages/ProblemDetailPage'
import { PublicProfilePage } from '../pages/PublicProfilePage'
import { RaiseProblemPage } from '../pages/RaiseProblemPage'
import { RegisterPage } from '../pages/RegisterPage'
import { Layout } from './Layout'

export const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'me', element: <RequireAuth><MyProfilePage /></RequireAuth> },
      { path: 'users/:id', element: <PublicProfilePage /> },
      { path: 'problems', element: <ExploreProblemsPage /> },
      { path: 'problems/new', element: <RequireAuth><RaiseProblemPage /></RequireAuth> },
      { path: 'problems/:code', element: <ProblemDetailPage /> },
      { path: 'me/problems', element: <RequireAuth><MyProblemsPage /></RequireAuth> },
      { path: 'solutions', element: <ComingSoonPage title="Explore Solutions" /> },
      { path: 'organizations', element: <ComingSoonPage title="Organizations" /> },
      { path: '*', element: <ComingSoonPage title="Page not found" /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
