import React, { createContext, useContext, useState } from 'react';
import { basicAuthentication, logout } from '../../api/CarSaleApiService';
import { apiClient } from '../../api/ApiBaseURL';
import { useNavigate } from 'react-router-dom';

const AuthContext = createContext();

export const useAuthContext = () => useContext(AuthContext)

export default function AuthProvider({ children }) {

    const [user, setUser] = useState(null);
    const [authenticated, setAuthenticated] = useState(false);
    const [token, setToken] = useState();
    const [type, setType] = useState();
    const [brand, setBrand] = useState();
    const [color, setColor] = useState();
    const [age, setAge] = useState();
    const [carID, setCarID] = useState();

     async function login(username, password){

        const basicAuthToken = 'Basic ' + window.btoa(username + ":" + password)

       const loginResponse = await basicAuthentication(basicAuthToken).then(
            (response) =>{
              console.log(response);
              if(response.status === 200){
                setAuthenticated(true);
                setUser(username);
                setToken(basicAuthToken);
                
                apiClient.interceptors.request.use(
                    (config) => {
                        console.log('intercepting')
                        config.headers.Authorization = basicAuthToken
                        return config
                      }
                )   
                console.log("check2");  
              }else{
                console.log("check3");
                setAuthenticated(false);
                setUser(null);
                setToken(null);
              }
            } 
              ).catch((error) => {
                console.log(error);
              } )
              .finally(response =>
                {return response}
              )
          return loginResponse;
        }

    function logoutFunction(){
      logout().then(
        setAuthenticated(false),
        setUser(null),
        setToken(null)
      ).catch(error => console.log(error));
     
    }
  
      return (
          <AuthContext.Provider value={{ user, login, logout, authenticated, setAuthenticated, token, type, setType, brand, setBrand, age, setAge, color, setColor, carID, setCarID }}>
              {children}
          </AuthContext.Provider>
      )
}

    