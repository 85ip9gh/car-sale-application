package crud_1.carSale.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import crud_1.carSale.entity.Car;
import crud_1.carSale.entity.User;
import crud_1.carSale.repository.CarRepository;
import crud_1.carSale.repository.UserRepository;

@Service
public class CarService {

	@Autowired
	private CarRepository carRepository;

	@Autowired
	private UserRepository userRepository;

	public Car saveCar(Car car) {
		return carRepository.save(car);
	}

	/**
	 * Completes a purchase: debits the buyer, credits the seller, transfers
	 * ownership, and takes the vehicle off the market, all in one transaction.
	 * <p>
	 * The money used to move in a second call the browser made after this one, so
	 * calling this endpoint directly transferred the car for free, and the seller
	 * was never paid at all. Both sides of the payment belong here, where the
	 * client cannot skip either one.
	 *
	 * @param buyer the authenticated purchaser
	 * @param carId the vehicle being bought
	 * @return the transferred vehicle
	 */
	@Transactional
	public Car buyCar(User buyer, int carId) {
		Car car = carRepository.findById(carId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Car not found"));

		if (!car.getSelling()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Car is not for sale");
		}

		User seller = car.getUser();

		if (seller != null && seller.getId() == buyer.getId()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot buy your own car");
		}

		// Read the buyer through the repository rather than trusting the detached
		// copy the security context carries, whose balance may be stale.
		User purchaser = userRepository.findById(buyer.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));

		if (purchaser.getMoney() < car.getPrice()) {
			throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Insufficient funds");
		}

		purchaser.setMoney(purchaser.getMoney() - car.getPrice());
		userRepository.save(purchaser);

		if (seller != null) {
			seller.setMoney(seller.getMoney() + car.getPrice());
			userRepository.save(seller);
		}

		car.setUser(purchaser);
		car.setSelling(false);

		return carRepository.save(car);
	}

	public List<Car> addAllCars(List<Car> listOfCars){
		return carRepository.saveAll(listOfCars);
	}
	
	public List<Car> saveAllCars() {
		return carRepository.saveAll(carRepository.findAll());
	}
	
	public List<Car> getAllCars(){
		return carRepository.findAll();
	}
	
	public void updateSellingCar(int id, boolean sell) {
		
		Car currentCar = carRepository.findById(id).get();
		
		currentCar.setSelling(sell);
		carRepository.save(currentCar);
	}
	
	public Car getCarById(int id) {
		return carRepository.findById(id).orElse(null);
	}
	
	public Car getCarByBrand(String brand) {
		return carRepository.findByBrand(brand);
	}
	
	public Car getCarByColor(String color) {
		return carRepository.findByColor(color);
	}
	
	public Car getCarByType(String type) {
		return carRepository.findByType(type);
	}
	
	public Car getCarByAge(int age) {
		return carRepository.findByAge(age);
	}
	
	public String deleteCar(int id) {
		carRepository.deleteById(id);
		return "removed car with id: " + id;
	}
	
	public Car updateCar(Car car) {
		Car currentCar = carRepository.findById(car.getId()).orElse(null);
		currentCar.setAge(car.getAge());
		currentCar.setBrand(car.getBrand());
		currentCar.setColor(car.getColor());
		currentCar.setType(car.getType());
		currentCar.setPrice(car.getPrice());		
		return carRepository.save(currentCar);
	}
	
	
	
	
	
	
	
	
}
