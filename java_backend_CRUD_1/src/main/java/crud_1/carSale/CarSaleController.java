package crud_1.carSale;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import crud_1.carSale.entity.Car;
import crud_1.carSale.entity.User;
import crud_1.carSale.service.CarService;
import crud_1.carSale.service.TokenService;
import crud_1.carSale.service.UserService;

/**
 * Car controller class for the car sale application that delegates tasks to the
 * service layer depending on the requests received from the user.
 * 
 * @author Pesanth Janaseth Rangaswamy Anitha
 */
@RestController
public class CarSaleController {

	@Autowired
	private CarService carService;

	@Autowired
	private UserService userService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * Logger for the CarSaleController class.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(CarSaleController.class);

	/**
	 * Ceiling on a single deposit. The money is play money, but an unbounded
	 * deposit lets one account render the marketplace meaningless.
	 */
	private static final long MAX_DEPOSIT = 1_000_000L;

	/**
	 * TokenService to generate jwt token for user.
	 */
	private final TokenService tokenService;

	/**
	 * Constructor for CarSaleController class. When the CarSaleController class is
	 * instantiated, the TokenService is injected into the class.
	 * 
	 * @param tokenService
	 */
	public CarSaleController(TokenService tokenService) {
		this.tokenService = tokenService;
	}

	/**
	 * CommandLineRunner that creates the two development accounts, sam and admin,
	 * on startup.
	 * <p>
	 * Both use the password "man", and admin holds ROLE_ADMIN, which can delete
	 * any user. That is fine on a local machine and unacceptable on a reachable
	 * host, so this is off unless app.seed-default-users is explicitly true. It
	 * also re-created the accounts on every boot, meaning deleting them by hand
	 * did not keep them gone.
	 *
	 * @return CommandLineRunner
	 */
	@Bean
	@ConditionalOnProperty(name = "app.seed-default-users", havingValue = "true")
	CommandLineRunner commandLineRunner() {
		return args -> {
			userService.saveUser(new User("sam", passwordEncoder.encode("man"), "ROLE_USER", 1000000));
			userService.saveUser(new User("admin", passwordEncoder.encode("man"), "ROLE_ADMIN", 1000000));
			LOG.warn("Seeded default development accounts. Never enable this on a reachable host.");
		};
	}

	/**
	 * Method to generate jwt token for user. The token service is called to
	 * generate a token based on the authentication object received.
	 * 
	 * @param authentication for the user
	 * @return jwt token string
	 */
	@GetMapping("/jwt-token")
	public String jwtToken(Authentication authentication) {
		LOG.error("USER {} IS PRESENT", authentication.getName());
		LOG.debug("user {} asking for jwt token", authentication.getName());
		String token = tokenService.generateToken(authentication);
		LOG.debug("Token given to user: {}", token);
		return token;
	}

	/**
	 * Method to get user roles. This is used to check if the user is an admin or
	 * not. If so, then they'll have access to all the other users' details.
	 * 
	 * @param principal
	 * @return string containing the user's roles
	 */
	@GetMapping("/basic-auth")
	public String basicAuthentication(Principal principal) {
		return userService.getUserByName(principal.getName()).get().getRoles();
	}

	/**
	 * Method to get all cars via the car service.
	 * 
	 * @return list of all cars present in the car table
	 */
	@GetMapping("/cars")
	public List<Car> getAllCars() {
		return carService.getAllCars();
	}

	/**
	 * Method to get all users from the user table. The user service is called to
	 * filter out the admin user from the list of users so that the admin user
	 * cannot be deleted/be seen by other admin users in the website.
	 * 
	 * @return list of users
	 */
	@GetMapping("/users")
	public List<User> getAllUsers() {
		return userService.getAllUsers().stream().filter(user -> !user.getName().equals("admin")).toList();
	}

	/**
	 * Method to get user by username.
	 * 
	 * @param username
	 * @return user
	 */
	@GetMapping("/users/{username}/money")
	public long getUserMoney(@PathVariable String username) {
		return userService.getUserMoney(username);
	}

	/**
	 * Method to post car to the database.
	 * 
	 * @param car
	 * @param principal
	 * @return car
	 */
	@PostMapping("/addCar")
	public Car addCar(@RequestBody Car car, Principal principal) {
		car.setUser(userService.getUserByName(principal.getName()).get());

		return carService.saveCar(car);
	}

	/**
	 * Method to post user to the database.
	 * 
	 * @param user
	 * @return user
	 */
	@PostMapping("/addUser")
	public ResponseEntity<User> addUser(@RequestBody User user) {

		User newUser = new User(user.getUsername(), passwordEncoder.encode(user.getPassword()), user.getRoles(),
				user.getMoney());

		User addedUser = userService.saveUser(newUser);

		if (addedUser == null) {
			return new ResponseEntity<User>(addedUser, HttpStatus.CONFLICT);
		}
		return new ResponseEntity<User>(addedUser, HttpStatus.CREATED);
	}

	/**
	 * Method to post list of cars to the database.
	 * 
	 * @param carList
	 * @return list of cars
	 */
	@PostMapping("/addAllCars")
	public List<Car> addAllCar(@RequestBody List<Car> carList) {
		return carService.addAllCars(carList);
	}

	/**
	 * Method to get current user's cars.
	 * 
	 * @param principal
	 * @return list of cars
	 */
	@GetMapping("/users/cars")
	public List<Car> getMyCars(Principal principal) {
		return userService.getUserCars(principal.getName());
	}

	/**
	 * Method to get car by id
	 * 
	 * @param carID
	 * @return car
	 */
	@GetMapping("/cars/{id}")
	public Car getCarById(@PathVariable int id) {
		return carService.getCarById(id);
	}

	/**
	 * Method to get car by brand.
	 * 
	 * @param brand
	 * @return car
	 */
	@GetMapping("/cars/{brand}")
	public Car getCarByBrand(@PathVariable String brand) {
		return carService.getCarByBrand(brand);
	}

	/**
	 * Method to get car by color.
	 * 
	 * @param id
	 * @return user
	 */
	@GetMapping("/cars/{color}")
	public Car getCarByColor(@PathVariable String color) {
		return carService.getCarByColor(color);
	}

	/**
	 * Method to get car by type.
	 * 
	 * @param type of car
	 * @return car
	 */
	@GetMapping("/cars/{type}")
	public Car getCarByType(@PathVariable String type) {
		return carService.getCarByType(type);
	}

	/**
	 * Method to get car by age.
	 * 
	 * @param age of car
	 * @return car
	 */
	@GetMapping("/cars/{age}")
	public Car getCarByAge(@PathVariable int age) {
		return carService.getCarByAge(age);
	}

	/**
	 * Method to buy a car. The service debits the buyer, credits the seller,
	 * transfers ownership, and unlists the vehicle in one transaction.
	 *
	 * @param carID          the vehicle being bought
	 * @param principal(i.e. current user)
	 * @return car
	 */
	@PutMapping("/cars/{carID}/buy")
	public Car buyCar(@PathVariable int carID, Principal principal) {
		return carService.buyCar(userService.getUserByName(principal.getName()).get(), carID);
	}

	/**
	 * Method to add funds to user's account.
	 * <p>
	 * Deposits must be positive. This used to accept any signed value, because the
	 * browser paid for a car by depositing a negative amount; that side of a
	 * purchase now happens inside the buy transaction, so a negative deposit has
	 * no legitimate caller and only serves to hand someone else's balance away.
	 *
	 * @param deposit
	 * @param principal
	 * @return money added to user
	 */
	@PatchMapping("/users/add-money/{deposit}")
	public long addMoneyToUser(@PathVariable long deposit, Principal principal) {
		if (deposit <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deposit must be positive");
		}

		if (deposit > MAX_DEPOSIT) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deposit exceeds the maximum");
		}

		return userService.addMoney(principal.getName(), deposit);
	}

	/**
	 * Method to update car details.
	 * 
	 * @param car
	 * @return updated car
	 */
	@PutMapping("/updateCar")
	public Car updateCar(@RequestBody Car car) {
		return carService.updateCar(car);
	}

	/**
	 * Method to sell car
	 * 
	 * @param car ID
	 */
	@PutMapping("/cars/{id}/selling")
	public void sellCar(@PathVariable int id) {
		carService.updateSellingCar(id, true);
	}

	/**
	 * Method to unlist car from market
	 * 
	 * @param car ID
	 */
	@PutMapping("/cars/{id}/unlist")
	public void unlistCar(@PathVariable int id) {
		carService.updateSellingCar(id, false);
	}

	/**
	 * Method to delete car from user's list
	 * 
	 * @param car ID
	 * @return message from carService
	 */
	@DeleteMapping("/deleteCar/{id}")
	public String removeCarFromMyList(@PathVariable int id) {
		return carService.deleteCar(id);
	}

	/**
	 * method to unlist car from user's list and change car's user
	 * 
	 * @param username
	 * @param id
	 * @return updated car
	 */
	@PutMapping("/cars/unlist/users/{username}/cars/{id}")
	public Car unlistCar(@PathVariable String username, @PathVariable int id) {
		return carService.changeCarUser(userService.getUserByName(username).get(), id);
	}

	/**
	 * Method to delete user from the database.
	 * 
	 * @param id
	 * @return deleted user
	 */
	@DeleteMapping("/users/delete/{id}")
	public ResponseEntity<User> deleteUser(@PathVariable int id) {
		User deletedUser = userService.getUserById(id);

		if (deletedUser.getMyCars().isEmpty() == false) {
			deletedUser.getMyCars().forEach(car -> carService.deleteCar(car.getId()));
		}

		userService.deleteUserById(id);

		return new ResponseEntity<User>(deletedUser, HttpStatus.OK);
	}
}
