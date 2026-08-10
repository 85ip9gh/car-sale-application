import { useEffect, useState } from "react"
import { buyCar, retrieveCars, retrieveMoney, unlistCar } from "../api/CarSaleApiService";
import { useAuthContext } from "./security/AuthProvider";
import toyota from '../images/toyota.jpg';
import audi from '../images/audi_new.jpg';
import ferrari from '../images/ferrari.jpg';
import lamborghini from '../images/lamborghini.jpg';
import chevrolet from '../images/chevrolet.jpg';
import honda from '../images/honda.jpg';
import mitsubishi from '../images/mitsubishi.jpg';
import BMW from '../images/BMW.jpg';

export default function HomeComponent() {

  const authContext = useAuthContext();
  const [cars, setCars] = useState([]);
  const [money, setMoney] = useState(0);
  const [poor, setPoor] = useState(false);


  function refreshCars() {
    retrieveCars().then(response => {
      setCars(response.data);
    }).catch(error => console.log(error))
  }

  function refreshMoney() {
    retrieveMoney(authContext.user).then(response => {
      console.log(response.data);
      setMoney(response.data);
    }
    ).catch(error => console.log(error))
  }

  async function buyCarFunction(id) {
    try {
      // The API debits the buyer, pays the seller, and unlists the car in one
      // transaction. This used to be paid for here with a negative deposit,
      // which meant calling the buy endpoint directly got the car for free.
      await buyCar(id);
      setPoor(false);

      refreshCars();
      refreshMoney();
    } catch (error) {
      if (error.response && error.response.status === 402) {
        setPoor(true);
      } else {
        console.log(error);
      }
    }
  }

  useEffect(
    () => {
      refreshCars();
      refreshMoney();
    }, []
  )

  function unlistCarFunction(id) {
    unlistCar(id)
      .then(() => {
        refreshCars();
      })
      .catch(error => console.log(error));
  }

  return (
    <div className="container market-container">
      <div className="market-money">
        <h1>Current Balance: ${money.toLocaleString()}</h1>

        {(poor == true) ?
          <div className="market-money-poor">
            <h1>you do not have enough money</h1>
          </div>
          : <></>
        }
      </div>
      <div className="market-container">
        <div className="container-home-title">
          <p className="container-title">Market</p>
          <hr />
        </div>

        <div className="grid-container">
          {
            cars.map(
              car => (
                (car.selling === true) ?
                  <div key={car.id} className="card market-car-card">
                    <div className="img-box">
                      {
                        (car.brand === 'Toyota') ?
                          <img src={toyota} alt="Grey Toyota Sedan" />
                          :
                          ((car.brand === 'Audi')) ?
                            <img src={audi} alt="Black Audi SUV" />
                            :
                            ((car.brand === 'Lamborghini')) ?
                              <img src={lamborghini} alt="Orange Lamborghini Supercar" />
                              :
                              (car.brand == "Honda") ?
                                <img src={honda} alt="Honda Car" />
                                :
                                (car.brand == "Mitsubishi") ?
                                  <img src={mitsubishi} alt="Mitsubishi Car" />
                                  :
                                  (car.brand == "BMW") ?
                                    <img src={BMW} alt="BMW Car" />
                                    :
                                    (car.brand == "Chevrolet") ?
                                      <img src={chevrolet} alt="Chevrolet Car" />
                                      :
                                      <img src={ferrari} alt="Red Ferrari Sportscar" style={{}} />

                      }
                    </div>
                    <div className="card-flex">

                      <div className="inner-card">
                        <div className="inner-card-field">
                          <p className="inner-card-field-name">ID:</p>
                          <p className="inner-card-field-value">
                            {car.id}
                          </p>
                        </div>

                        <div className="inner-card-field">
                          <p className="inner-card-field-name">Brand:</p>
                          <p className="inner-card-field-value">
                            {car.brand}
                          </p>
                        </div>

                        <div className="color-row"><p className="inner-card-field-name">Color:</p><div className="car-color" style={{ backgroundColor: car.color }} ></div></div>

                        <div className="inner-card-field">
                          <p className="inner-card-field-name">Type:</p>
                          <p className="inner-card-field-value">
                            {car.type}
                          </p>
                        </div>

                        <div className="inner-card-field">
                          <p className="inner-card-field-name">Age:</p>
                          <p className="inner-card-field-value">
                            {car.age}
                          </p>
                        </div>

                        <div className="inner-card-field">
                          <p className="inner-card-field-name">Seller:</p>
                          <p className="inner-card-field-value">
                            {car.seller}
                          </p>
                        </div>
                      </div>

                      <div className="market-card-right">
                        <div className="price-wrapper">
                          <p className="market-card-price">
                            ${car.price.toLocaleString()}
                          </p>
                        </div>

                        {(car.seller.toLowerCase() == authContext.user.toLowerCase()) ?
                          <div> <button className="btn market-btn car-unlist-btn " onClick={() => unlistCarFunction(car.id)}>Unlist</button> </div>
                          :
                          <div> <button className="btn market-btn car-buy-btn" onClick={() => {
                            (money < car.price) ?
                              setPoor(true)
                              :
                              buyCarFunction(car.id)
                          }
                          } >Buy</button> </div>
                        }

                      </div>


                    </div>
                  </div>
                  : <></>
              )
            )
          }
        </div>
      </div>
    </div>
  )
}