package com.lyricist.server.database;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;


public interface UserRepository extends MongoRepository<User, String> {
    @Query("{email :?0}")
    User findUserByEmail(String email);

    @Query("{token :?0}")
    User findUserByToken(String token);
}
