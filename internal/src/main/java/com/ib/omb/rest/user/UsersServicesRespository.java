package com.ib.omb.rest.user;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

/**
 * Class holds all logged user sessions
 * @author ilukov
 */
@ApplicationScoped
public class UsersServicesRespository {
    private Map<Integer, String> jwtTokenMap = new HashMap<>();

    /**
     * Method registers userId and jwtToken associated with the user
     * @param userId - id of the user
     * @param jwtToken - token of the user
     */
    synchronized void login(int userId, String jwtToken){
        jwtTokenMap.put(userId, jwtToken);
    }

    /**
     * Method checks if user id is present in repository (user is logged in)
     * @param userId - id of the user
     * @return - result of the check
     */
    public synchronized boolean containsUser(int userId){
        return jwtTokenMap.containsKey(userId);
    }

    public synchronized void updateUserJwtToken(int userId, String jwtToken){
        login(userId,jwtToken);
    }

    /**
     * Method unregisters user from the repository
     * @param userid - id of the user
     */
    synchronized void logoff(int userid){
        jwtTokenMap.remove(userid);
    }
}
