import './App.css'
import { Route, Routes } from 'react-router-dom'
import { MainPage } from './views/pages/MainPage'

function App() {
  return (
    <Routes>
      <Route path="*" element={<MainPage />} />
    </Routes>
  )
}

export default App
