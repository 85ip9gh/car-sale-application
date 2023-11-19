package crud_1.carSale.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TableGenerator;
import lombok.Data;

@SuppressWarnings("serial")
@Data
@Entity(name = "user_details")
public class User implements UserDetails{
	

	@TableGenerator(
		name = "tableGenerator",
		allocationSize = 1
		)
	@Id
	@GeneratedValue(
		strategy = GenerationType.TABLE,
		generator = "tableGenerator"
		)
	int id;
	
	private String name;
	private String password;
	private String roles;

	@OneToMany(mappedBy = "user")
	public List<Car> myCars = new ArrayList<>();
	
	public User() {
		this.name = "sam";
		this.password = "man";
		this.roles = "ROLE_USER";
	}
	
	public User(String name, String password, String roles) {
		this.name = name;
		this.password = password;
		this.roles = roles;
	}
	
	public User(User user) {
		this.name = user.getName();
		this.password = user.getPassword();
		
	}
	
	public User(String name, List<Car> myCars) {
		this.name = name;
		this.myCars = myCars;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Arrays.stream(roles.split(","))
				.map(SimpleGrantedAuthority::new)
				.toList();
				
	}

	@Override
	public String getUsername() {
		return name;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
	
	
}
	