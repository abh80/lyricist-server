package com.lyricist.server.database;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;


public interface UserRepository extends MongoRepository<User, String> {
    @Query("{email :?0}")
    User findUserByEmail(String email);

    @Query("{token :?0}")
    User findUserByToken(String token);

    @Query("{name :  { $regex: ?0 } }")
    List<User> findUserByName(String name);
}
