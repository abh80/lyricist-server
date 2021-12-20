package com.lyricist.server.controllers;

import com.lyricist.server.Util;
import com.lyricist.server.database.User;
import com.lyricist.server.database.UserRepository;
import com.lyricist.server.utils.ErrorJson;
import com.lyricist.server.utils.PrivateUser;
import com.lyricist.server.utils.UserSessionModel;
import com.lyricist.server.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;

@CrossOrigin
@RestController
@RequestMapping(value = "/api/v1")
class UserController {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    JavaMailSender javaMailSender;
    private final HashMap<String, UserSessionModel> tempUsers = new HashMap<>();
    private final HashMap<String, UserSessionModel> tempUserReset = new HashMap<>();

    private final UserUtils userUtils = new UserUtils();

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 180000)
    void purgeSessions() {
        tempUsers.forEach((String v, UserSessionModel u) -> {
            if ((Instant.now().toEpochMilli() - u.time) >= 1800000) {
                tempUsers.remove(v);
            }
        });
        tempUserReset.forEach((String v, UserSessionModel u) -> {
            if ((Instant.now().toEpochMilli() - u.time) >= 1800000) {
                tempUserReset.remove(v);
            }
        });
    }

    @PostMapping("/s/reset/{id}/verify")
    ResponseEntity<?> verifyReset(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        if (id == null)
            return new ResponseEntity<>(new ErrorJson("`id` path variable is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (tempUserReset.get(id) == null) {
            return new ResponseEntity<>(new ErrorJson("Invalid verification key was provided or the session has expired.", 404, "Not Found"), HttpStatus.NOT_FOUND);
        }
        if (body == null)
            return new ResponseEntity<>(new ErrorJson("body is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (body.get("pin") == null || body.get("pin").isEmpty()) {
            return new ResponseEntity<>(new ErrorJson("`pin` field cannot be blank.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
        if (body.get("password") == null || body.get("password").isEmpty()) {
            return new ResponseEntity<>(new ErrorJson("`password` field should not be blank.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
        UserSessionModel sessionModel = tempUserReset.get(id);

        if (sessionModel.pin.equals(body.get("pin"))) {
            String token = UserUtils.generateToken(sessionModel.user.getId());
            while (userRepository.findUserByToken(token) != null) {
                token = UserUtils.generateToken(sessionModel.user.getId());
            }
            sessionModel.user.setToken(UserUtils.generateToken(token));
            sessionModel.user.setPassword(body.get("password"));
            User save = userRepository.save(sessionModel.user);
            tempUserReset.remove(id);
            return new ResponseEntity<>(new PrivateUser(save), HttpStatus.OK);
        } else {
            sessionModel.addTry();
            if (sessionModel.tries >= 3) {
                tempUserReset.remove(id);
                return new ResponseEntity<>(new ErrorJson("Invalid pin was provided for 3 times and the session has been expired. Try again after a while.", 403, "Forbidden"), HttpStatus.FORBIDDEN);
            }
            return new ResponseEntity<>(new ErrorJson("Invalid pin was provided try again.", 401, "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

    }

    @PostMapping("/s/reset")
    ResponseEntity<?> resetPassword(@RequestBody(required = false) Map<String, String> body) {
        if (body == null)
            return new ResponseEntity<>(new ErrorJson("Body cannot be blank.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (body.get("email") == null || body.get("email").isEmpty())
            return new ResponseEntity<>(new ErrorJson("`email` field cannot be blank.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        String email = body.get("email");
        User user = userRepository.findUserByEmail(email);
        if (user == null)
            return new ResponseEntity<>(new ErrorJson("User not found.", 401, "Unauthorized"), HttpStatus.UNAUTHORIZED);

        String session = UserUtils.generateSession(34);
        while (tempUserReset.get(session) != null) {
            session = UserUtils.generateSession(34);
        }
        String pin = UserUtils.generateSession(18);
        UserSessionModel sessionModel = new UserSessionModel(user, pin);
        if (tempUserReset.containsValue(sessionModel)) {
            tempUserReset.forEach((String v, UserSessionModel u) -> {
                if (u.user.getEmail().equals(user.getEmail())) tempUserReset.remove(v);
            });
        }
        tempUserReset.put(session, sessionModel);
        HashMap<String, String> resp = new HashMap<>();
        resp.put("success", "true");
        resp.put("session", session);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setSubject("Reset your password for Lyricist");
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);
            messageHelper.setTo(user.getEmail());
            messageHelper.setFrom("lyricistms@gmail.com");
            messageHelper.setText(Util.readFile("PasswordResetTemplate.html").replace("${name}", user.getName()).replace("${code}", pin), true);
            javaMailSender.send(message);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorJson("Failed to send email.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @PostMapping("/sessions/{id}/verify")
    ResponseEntity<?> verifySession(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        if (id == null)
            return new ResponseEntity<>(new ErrorJson("`id` path variable is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (tempUsers.get(id) == null)
            return new ResponseEntity<>(new ErrorJson("Invalid session key was provided or the session has expired.", 404, "Not Found"), HttpStatus.NOT_FOUND);
        if (body == null)
            return new ResponseEntity<>(new ErrorJson("body is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (body.get("otp") == null || body.get("otp").isEmpty())
            return new ResponseEntity<>(new ErrorJson("`otp` field cannot be blank.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        UserSessionModel sessionModel = tempUsers.get(id);
        try {
            if (sessionModel.otp == Integer.parseInt(body.get("otp"))) {
                User save = userRepository.save(sessionModel.user);
                tempUsers.remove(id);
                return new ResponseEntity<>(new PrivateUser(save), HttpStatus.OK);
            } else
                return new ResponseEntity<>(new ErrorJson("Invalid otp was provided try again.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            sessionModel.addTry();
            if (sessionModel.tries >= 3) {
                tempUsers.remove(id);
                return new ResponseEntity<>(new ErrorJson("Invalid otp was provided for 3 times and the session has been expired. Try again after a while.", 403, "Forbidden"), HttpStatus.FORBIDDEN);
            }
            return new ResponseEntity<>(new ErrorJson("Invalid otp was provided try again.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/users")
    ResponseEntity<?> createUser(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest request) {
        if (body == null) {
            return new ResponseEntity<>(new ErrorJson("Body cannot be null.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
        if (body.get("captcha_response") == null)
            return new ResponseEntity<>(new ErrorJson("`captcha_response` field cannot be empty.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (!userUtils.verifyCaptchaResponse((String) body.get("captcha_response"), request.getRemoteAddr())) {
            return new ResponseEntity<>(new ErrorJson("Captcha response is invalid.", 401, "Bad Request"), HttpStatus.BAD_REQUEST);
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
        String token = UserUtils.generateToken(uid);
        while (userRepository.findUserByToken(token) != null) {
            token = UserUtils.generateToken(uid);
        }
        User user = new User(uid, (String) body.get("name"), new String[]{"user"}, (String) body.get("email"), (String) body.get("image"), (String) body.get("password"), token);
        int otp = new Random().nextInt(900000) + 100000;
        String session = UserUtils.generateSession();
        while (tempUsers.get(session) != null) {
            session = UserUtils.generateSession();
        }

        try {
            MimeMessage mailMessage = javaMailSender.createMimeMessage();

            mailMessage.setSubject("Your one time login code is: " + otp);
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mailMessage, true);
            mimeMessageHelper.setTo(user.getEmail());
            mimeMessageHelper.setFrom("lyricistms@gmail.com");
            mimeMessageHelper.setText(Util.readFile("OtpTemplate.html").replace("${name}", user.getName()).replace("${code}", Integer.toString(otp)), true);
            javaMailSender.send(mailMessage);
            UserSessionModel sessionModel = new UserSessionModel(user, otp);
            if (tempUsers.containsValue(sessionModel)) {
                tempUsers.forEach((String v, UserSessionModel u) -> {
                    if (u.user.getEmail().equals(user.getEmail())) tempUsers.remove(v);
                });
            }
            tempUsers.put(session, sessionModel);

            HashMap<String, String> resp = new HashMap<>();
            resp.put("success", "true");
            resp.put("session", session);

            return new ResponseEntity<>(resp, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ErrorJson("Invalid email address.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
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
    ResponseEntity<?> searchUser(@RequestParam(required = false) Map<String, String> query) {
        int limit = 5;
        if (query == null)
            return new ResponseEntity<>(new ErrorJson("`q` param is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (query.get("q") == null || query.get("q").isEmpty()) {
            return new ResponseEntity<>(new ErrorJson("`q` param is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
        if (query.get("limit") != null && !query.get("limit").isEmpty()) {
            try {
                limit = Integer.parseInt(query.get("limit"));
            } catch (Exception e) {
                return new ResponseEntity<>(new ErrorJson("`limit` should contain an integer.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
            }
        }

        List<User> users = userRepository.findByNameStartingWith(query.get("q"));
        return new ResponseEntity<>(users.subList(0, limit), HttpStatus.OK);
    }

    @PostMapping("/s/login")
    ResponseEntity<?> login(@RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        if (body == null)
            return new ResponseEntity<>(new ErrorJson("Body cannot be blank.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (body.get("captcha_response") == null)
            return new ResponseEntity<>(new ErrorJson("`captcha_response` field cannot be empty.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (!userUtils.verifyCaptchaResponse(body.get("captcha_response"), request.getRemoteAddr()))
            return new ResponseEntity<>(new ErrorJson("Captcha response is invalid.", 401, "Bad Request"), HttpStatus.BAD_REQUEST);
        if (body.get("email") == null || body.get("email").isEmpty())
            return new ResponseEntity<>(new ErrorJson("`email` field cannot be blank.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        else if (body.get("password") == null || body.get("password").isEmpty())
            return new ResponseEntity<>(new ErrorJson("`password` field cannot be blank.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        User user = userRepository.findUserByCredentials(body.get("email"), body.get("password"));
        if (user == null)
            return new ResponseEntity<>(new ErrorJson("Credentials are invalid.", 401, "Unauthorized"), HttpStatus.UNAUTHORIZED);
        else return new ResponseEntity<>(new PrivateUser(user), HttpStatus.OK);
    }

    @GetMapping("/users/me")
    ResponseEntity<?> getMe(@RequestHeader(required = false) Map<String, String> headers) {
        if (headers == null) {
            return new ResponseEntity<>(new ErrorJson("`authorization` field is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        } else if (headers.get("authorization") == null) {
            return new ResponseEntity<>(new ErrorJson("`authorization` field is missing.", 400, "Bad Request"), HttpStatus.BAD_REQUEST);
        }
        String authHeader = headers.get("authorization");
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
