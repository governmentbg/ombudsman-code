package com.ib.omb.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.omb.rest.user.UsersServicesRespository;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * Class provides methods required by remote clients using REST style of communication
 * @author ilukov
 */
public class DocuWorkAndroidEndpoint {
    private static Logger LOGGER = LoggerFactory.getLogger(DocuWorkAndroidEndpoint.class.getName());

    // TODO see better way how to build the secret key
    protected static SecretKey SECRET_KEY;
    static{
       try(InputStream hmacSHA256KeyStream = DocuWorkAndroidEndpoint.class.getClassLoader().getResourceAsStream("HmacSHA256.key")) {
            assert hmacSHA256KeyStream != null;
            byte[] targetArray = new byte[hmacSHA256KeyStream.available()];
            int read = hmacSHA256KeyStream.read(targetArray);

            if(read > 0) {
                // bytes is now having base64 encoded secret key
                byte[] keySpec = Base64.getDecoder().decode(targetArray);
                SECRET_KEY = new SecretKeySpec(keySpec, SignatureAlgorithm.HS256.getJcaName());
            }else{
                throw new Exception("No HmacSHA256 key found!!!");
            }
        } catch (Exception e) {
            // LOG
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Method builds JwtToken with given <code>admUser</code> parameter. All REST endpoints are required to invoke this method and
     * setup the new generated jwtToken into the response.
     * @param admUser - user to be used into building JwtToken
     * @return - just created jwtToken as java.lang.String
     * @throws Exception - if there is a problem with database
     */
    protected String buildJwtToken(AdmUser admUser, UsersServicesRespository usersServicesRespository) throws Exception{
        // build JWTToken
        final JwtBuilder jwtBuilder = Jwts.builder()
                .setIssuer("DocuWork") // TODO see what goes here
                .setSubject(admUser.getId().toString()) // id of the user
                .setIssuedAt(Date.from(Instant.now()))// current instant of time
                .setExpiration(Date.from(Instant.now().plus(60, ChronoUnit.MINUTES)))// TODO configuration ?!
                .signWith(
                        SignatureAlgorithm.HS256,// TODO configuration ?!
                        SECRET_KEY.getEncoded()// secret key for encryption
                );

        // build jwt claims
        final Map<Integer, Map<Integer, Boolean>> userAccessMap = new AdmUserDAO(ActiveUser.of(admUser.getId())).findUserAccessMap(admUser.getId());
        admUser.setAccessValues(userAccessMap);
        for (final Map.Entry<Integer, Map<Integer, Boolean>> mapEntry : userAccessMap.entrySet()) {
            String claimKey = mapEntry.getKey().toString();

            JsonObjectBuilder claimValueBuilder = Json.createObjectBuilder();
            for (Map.Entry<Integer, Boolean> claimValueEntry : mapEntry.getValue().entrySet()) {
                Integer claimKeyCode = claimValueEntry.getKey();
                Boolean claimValue = claimValueEntry.getValue();

                claimValueBuilder.add(claimKeyCode.toString(), claimValue);
            }
            jwtBuilder.claim(claimKey, claimValueBuilder.build());
        }

        String jwtToken = jwtBuilder.compact();
        usersServicesRespository.updateUserJwtToken(admUser.getId(), jwtToken);

        return jwtToken;
    }


    /*
     * protected methods
     */

    /**
     * Method returns systemData object from ServletContext instantce
     * @param context - context to be searched in
     * @return - system data instance
     */
    protected SystemData getSystemData(ServletContext context) {
        return (SystemData) context.getAttribute("systemData");
    }

    /**
     * Method extracts jwtToken and searches for user id
     * @param jwtToken - jwtToken to be check for user data
     * @return - return user id
     * @throws Exception - if there is exception
     */
    protected int extractUserIdFromJwtToken(String jwtToken) throws Exception{
        if(jwtToken == null || "".equals(jwtToken) || jwtToken.trim().length() == 0){
            LOGGER.error("No jwtToken !!!");
            throw new Exception("No jwtToken!!!");
        }


        final Claims claims = Jwts.parser().setSigningKey(SECRET_KEY.getEncoded()).parseClaimsJws(jwtToken).getBody();

         // TODO it might be required to mark what happens when jwtToken is expired

        return Integer.parseInt(claims.getSubject());
    }

    protected String buildErrorResponse(ObjectMapper objectMapper, ByteArrayOutputStream outputStream, DocuWorkRestResponse docuWorkRestResponse) {
        try {
            objectMapper.writeValue(outputStream, docuWorkRestResponse);
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // ignore
            return "{\"error\":\"" + DocuWorkRestResponse.STATUS.ERROR.name() + "\", \"message\":\""+ex.getMessage()+"\"}";
        }
    }

    /*
     * end protected methods
     */
}
