import { BrowserRouter, Route, Routes} from 'react-router-dom';
import HeaderComponent from './components/HeaderComponent';
import HomeComponent from './components/HomeComponent';
import LoginComponent from './components/LoginComponent';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <HeaderComponent/>

        <Routes>
          <Route path='/login' element={<LoginComponent/>} />
          <Route path='/home' element={<HomeComponent/>} />
        </Routes>
    </BrowserRouter>

  );
}

export default App;
