import { BrowserRouter, Routes, Route } from 'react-router-dom';
import NavBar from './components/NavBar';
import Landing from './pages/Landing';
import Check from './pages/Check';
import Result from './pages/Result';
import Examples from './pages/Examples';

export default function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen flex flex-col">
        <NavBar />
        <div className="flex-1">
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/check" element={<Check />} />
            <Route path="/result/:id" element={<Result />} />
            <Route path="/examples" element={<Examples />} />
          </Routes>
        </div>
      </div>
    </BrowserRouter>
  );
}
