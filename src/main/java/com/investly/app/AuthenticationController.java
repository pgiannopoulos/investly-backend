package com.investly.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
public class AuthenticationController {

    private final UserRepository userRepository;

    public AuthenticationController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String login (
            @RequestParam(defaultValue = "suckme@gmail.com") String email,
            @RequestParam(name = "password", required = false) String password
    ) {
        UserEntity user = new UserEntity();

        user.setEmail(email);
        user.setPassword("12345");
        user.setBirthDate(OffsetDateTime.now());

        return userRepository.save(user).getBirthDate().toString();
    }


}

