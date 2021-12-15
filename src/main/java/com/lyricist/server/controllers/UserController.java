package com.lyricist.server.controllers;

import com.lyricist.server.database.User;
import com.lyricist.server.database.UserRepository;
import com.lyricist.server.utils.ErrorJson;
import com.lyricist.server.utils.PrivateUser;
import com.lyricist.server.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/v1")
class UserController {

    @Autowired
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @PostMapping(value = "/users")
    ResponseEntity<?> createUser(@RequestBody(required = false) Map<String, Object> body) {

        if (body == null) {
            return new ResponseEntity<>(new ErrorJson("Body cannot be null.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }

        if (body.get("name") == null) {
            return new ResponseEntity<>(new ErrorJson("`name` field cannot be empty.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        } else if (body.get("email") == null) {
            return new ResponseEntity<>(new ErrorJson("`email` field cannot be empty.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        } else if (body.get("password") == null) {
            return new ResponseEntity<>(new ErrorJson("`password` field cannot be empty.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        } else if (!UserUtils.validateEmail((String) body.get("email"))) {
            return new ResponseEntity<>(new ErrorJson("email address is not valid.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        } else if (userRepository.findUserByEmail((String) body.get("email")) != null) {
            return new ResponseEntity<>(new ErrorJson("User with same email address already exists, please login instead.", 403, "Forbidden"), HttpStatus.FORBIDDEN);
        } else if (body.get("image") != null) {
            body.put("image", null);
        }
        String uid = UserUtils.generateUID();
        while (userRepository.findById(uid).isPresent()) {
            uid = UserUtils.generateUID();
        }
        User save = userRepository.save(new User(uid, (String) body.get("name"), new String[]{"user"}, (String) body.get("email"), (String) body.get("image"), (String) body.get("password"), UserUtils.generateToken(uid)));
        return new ResponseEntity<>(new PrivateUser(save), HttpStatus.OK);
    }

    @GetMapping("/users/d/{id}")
    ResponseEntity<?> getUser(@PathVariable String id) {
        if (id == null) {
            return new ResponseEntity<>(new ErrorJson("`id` path variable is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
        Optional<User> user = userRepository.findById(id);
        if (!user.isPresent()) {
            return new ResponseEntity<>(new ErrorJson("No user found.", 404, "Bad Request"), HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(user.get(), HttpStatus.OK);
    }

    @GetMapping("/users/search")
    ResponseEntity<?> searchUser(@RequestParam Map<String, String> query) {
        if (query == null)
            return new ResponseEntity<>(new ErrorJson("`q` param is missing", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (query.get("q").isEmpty()) {
            return new ResponseEntity<>(new ErrorJson("`q` param is missing", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
        List<User> users = userRepository.findUserByName(query.get("q"));
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/users/me")
    ResponseEntity<?> getMe(@RequestHeader(required = false) Map<String, String> headers) {
        if (headers == null) {
            return new ResponseEntity<>(new ErrorJson("`authorization` field is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        } else if (headers.get("authorization") == null) {
            return new ResponseEntity<>(new ErrorJson("`authorization` field is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
        String authHeader = (String) headers.get("authorization");
        if (authHeader.toLowerCase().startsWith("bearer")) {
            if (authHeader.split(" ").length != 2)
                return new ResponseEntity<>(new ErrorJson("Invalid token provided.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
            String token = authHeader.split(" ")[1];
            User user = userRepository.findUserByToken(token);
            if (user != null) {
                return new ResponseEntity<>(new PrivateUser(user), HttpStatus.OK);
            } else
                return new ResponseEntity<>(new ErrorJson("Invalid token provided.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(new ErrorJson("`authorization` field has an incorrect prefix.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
    }
}
