import { useEffect, useState } from "react";
import { listCarForSale, retrieveMyCars, sellCar } from "../api/CarSaleApiService";
import { useAuthContext } from "./security/AuthProvider";
import { useNavigate } from "react-router-dom";

export default function MyCarsComponent(){
    const authContext = useAuthContext();
    const [cars, setCars] = useState([]);
    const navigate = useNavigate();

   
    useEffect(
      () => {
        refreshCars()
      }, []
    )

    function refreshCars(){
      retrieveMyCars().then(response => {
        console.log(response.data);
        setCars(response.data);
        console.log(Array.isArray(cars))
      }).catch(error => console.log(error))
    }

    async function sellCarFunction(id){
      await sellCar(id)
      .then(()=>{
        refreshCars();
      })
      .catch(console.log('error'))
    }

    function updateCarFunction(brand, color, type, age){
      authContext.setBrand(brand);
      authContext.setColor(color);
      authContext.setType(type);
      authContext.setAge(age);
      navigate("/update-car")
    }
   
    return(
        <div className="container">
      <table className="car-list">
        <thead>
          <tr>
            <th>ID</th>
            <th>Brand</th>
            <th>Color</th>
            <th>Type</th>
            <th>Age</th>
            <th>Seller</th>
          </tr>
        </thead>

        <tbody>
          {
            cars.map(
              car => (
                (car.selling == false) ?
                <tr key={car.id}>
                  <td>{car.id}</td>
                  <td>{car.brand}</td>
                  <td>{car.color}</td>
                  <td>{car.type}</td>
                  <td>{car.age}</td>
                  <td>{car.seller}</td>
                <td><button className="car-list-for-sale-btn" onClick={() => sellCarFunction(car.id)}>List for Sale</button></td>
                <td><button className="car-update-btn" onClick={() => updateCarFunction(car.brand, car.color,car.type, car.age)}>Update Car</button></td>
                </tr>
                : <p></p>
              )
            )
          }
        </tbody>
      </table>
      
    </div>
    )
}