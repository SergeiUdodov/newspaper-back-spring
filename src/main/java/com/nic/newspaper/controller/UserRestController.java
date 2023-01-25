package com.nic.newspaper.controller;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nic.newspaper.config.JwtTokenUtil;
import com.nic.newspaper.entity.Role;
import com.nic.newspaper.entity.User;
import com.nic.newspaper.service.UserService;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@CrossOrigin("http://localhost:8081/")
public class UserRestController {

	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private UserService userService;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@GetMapping("/users")
	public List<User> findAll() {
		return userService.findAll();
	}

	@GetMapping("/users/{UserId}")
	public User getUser(@PathVariable int UserId) {

		User theUser = userService.findById(UserId);

		if (theUser == null) {
			throw new RuntimeException("User id not found - " + UserId);
		}

		return theUser;
	}

	@GetMapping("/userByToken")
	public User getUserByToken(HttpServletRequest request) {

		final String requestTokenHeader = request.getHeader("Authorization");

		String userEmail = null;
		String jwtToken = null;

		if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
			jwtToken = requestTokenHeader.substring(7);
			try {
				userEmail = jwtTokenUtil.getUserEmailFromToken(jwtToken);
			} catch (IllegalArgumentException e) {
				System.out.println("Unable to get JWT Token");
			} catch (ExpiredJwtException e) {
				System.out.println("JWT Token has expired");
			}
		} else {
			logger.warn("JWT Token is null or does not begin with Bearer String");
		}

		return userService.getUserByToken(userEmail);
	}

	@GetMapping("/isUserAdmin")
	public boolean isUserAdmin(HttpServletRequest request) {
		boolean isAdmin = false;
		Collection<Role> userRoles = getUserByToken(request).getRoles();
		for (Role role : userRoles) {
			if ("ROLE_ADMIN".equals(role.getName())) {
				isAdmin = true;
			}
		}
		return isAdmin;
	}

}
