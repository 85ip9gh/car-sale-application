import { useNavigate } from "react-router-dom";
import { useAuthContext } from "./security/AuthProvider";
import { useState } from "react";


export default function LoginComponent() {

  const navigate = useNavigate();
  const authContext = useAuthContext();

  const [form, setForm] = useState({
    username: "admin",
    password: "man"
  });

  const [wrongCredentials, setWrongCredentials] = useState(false);
  const [hideLogin, sethideLogin] = useState(false);

  function handleFormChage(event) {
    setForm({
      ...form,
      [event.target.name]: event.target.value
    })
  }

  const submit = async (e) => {
    e.preventDefault();
    const loginReply = await authContext.login(form.username, form.password);
    if (loginReply == 200) {
      sethideLogin(true);
      navigate("/market");
    } else {
      setWrongCredentials(true);
    }
  }


  return (

    <div className="container container-login" >


      {wrongCredentials &&
        <div className="error-msg-box">
          <p className="error-msg">
            {authContext.errorMsg}
          </p>
        </div>
      }

      {!hideLogin &&

        <form method="POST" onSubmit={submit} className="login-container">
          <svg className="login-info-icon" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path d="M12 2c5.514 0 10 4.486 10 10s-4.486 10-10 10-10-4.486-10-10 4.486-10 10-10zm0-2c-6.627 0-12 5.373-12 12s5.373 12 12 12 12-5.373 12-12-5.373-12-12-12zm-.001 5.75c.69 0 1.251.56 1.251 1.25s-.561 1.25-1.251 1.25-1.249-.56-1.249-1.25.559-1.25 1.249-1.25zm2.001 12.25h-4v-1c.484-.179 1-.201 1-.735v-4.467c0-.534-.516-.618-1-.797v-1h3v6.265c0 .535.517.558 1 .735v.999z" /></svg>
          <div className="login-admin-info-wrapper">
            <p className="login-admin-info">
              set username as "admin" for admin priviledges
            </p>
          </div>
          <div className="login-header">
            <p className="form-title">
              Login
            </p>
          </div>
          <div className="form-row">
            <input name="username" type="text" className="input-text" placeholder="Username" value={form.username} onChange={handleFormChage} required></input>
          </div>

          <div className="form-row">
            <input name="password" className="input-password" type="password" placeholder="Password" value={form.password} onChange={handleFormChage} required></input>
          </div>
          <p className="forgot-password">Forgot password?</p>

          <button type="submit" className="btn btn-form">Login</button>
        </form>
      }

    </div>

  )
}