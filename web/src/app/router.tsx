import { createBrowserRouter } from 'react-router'
import { ComingSoonPage } from '../pages/ComingSoonPage'
import { HomePage } from '../pages/HomePage'
import { Layout } from './Layout'

export const routes = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'problems', element: <ComingSoonPage title="Explore Problems" /> },
      { path: 'problems/new', element: <ComingSoonPage title="Raise a Problem" /> },
      { path: 'solutions', element: <ComingSoonPage title="Explore Solutions" /> },
      { path: 'organizations', element: <ComingSoonPage title="Organizations" /> },
      { path: '*', element: <ComingSoonPage title="Page not found" /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
