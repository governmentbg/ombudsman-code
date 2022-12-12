package com.ib.omb.notifications;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.ib.system.db.JPA;

/**
 * За пращането на нотификации към мобилното
 */
public final class FirebaseClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseClient.class);

	private static final String	FCM_PRIVATE_KEY_FILE	= "indexmessages-firebase-adminsdk-92ppl-4c042bf258.json";
	private static final String	AUTH_SCOPE				= "https://www.googleapis.com/auth/firebase.messaging";
	private static final String	POST_URI				= "https://fcm.googleapis.com/v1/projects/indexmessages/messages:send";

	private static CloseableHttpClient	httpClient;
	private static GoogleCredentials	credentials;

	/**
	 * Изпращане на данни за брой нотификации към Firebase. При формиране на нова нотификация в pushbean бройката се качва с +1. В
	 * останалите случаи в работния плот крайната бройка е ясна и тя се използва.
	 *
	 * @param userId
	 * @param addCount   +- добавяне към текущата бройка там
	 * @param finalCount общата бройка
	 */
	@SuppressWarnings("unchecked")
	public static void sendNotifCount(Integer userId, Integer addCount, Integer finalCount) {
		List<Object> rows = null;
		try {
			rows = JPA.getUtil().getEntityManager().createNativeQuery( //
				"select distinct DEVICE_ID from MOBILE_LOGINS where USER_ID = ?1 order by LOGIN_TIME desc") //
				.setParameter(1, userId).getResultList();
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на deviceToken за userId=" + userId, e);
		}
		if (rows == null || rows.isEmpty()) {
			LOGGER.debug("Не е намерено deviceToken за userId={}", userId);
			return; // няма как да се продължи
		}

		if (httpClient == null || credentials == null) {
			boolean ok = init();
			if (!ok) {
				return; // няма как да се случат нещата
			}
		}

		for (Object deviceToken : rows) { // за всички у-ва с които е логнат потребителя

			// TODO как да се разбере дали у-вото е логнато при нас, но фиребасе го игнорва
			runOnce(userId, addCount, finalCount, (String) deviceToken);
		}
	}

	/**
	 * @param userId
	 * @param addCount
	 * @param finalCount
	 * @param deviceToken
	 * @return
	 */
	static boolean runOnce(Integer userId, Integer addCount, Integer finalCount, String deviceToken) {
		boolean sent = false;
		try { // реалното изпращане на съобщението

			credentials.refreshIfExpired();
			AccessToken accessToken = credentials.getAccessToken();

			Map<String, Object> message = new LinkedHashMap<>();

			Map<String, String> data = new LinkedHashMap<>();
			data.put("sender", "DocuWork");
			data.put("add_count", String.valueOf(addCount)); // ако е null ще влезне стринг "null" и така трябва
			data.put("final_count", String.valueOf(finalCount)); // ако е null ще влезне стринг "null" и така трябва

			message.put("data", data);
			message.put("token", deviceToken);

			Map<String, Object> body = Collections.singletonMap("message", message);

			String jsonBody = new ObjectMapper().writeValueAsString(body);

			HttpPost post = new HttpPost(POST_URI);
			post.setEntity(new StringEntity(jsonBody));
			post.setHeader("Content-Type", "application/json");
			post.setHeader("Authorization", "Bearer " + accessToken.getTokenValue()); // OAuth 2.0

			try (CloseableHttpResponse response = httpClient.execute(post)) {

				if (response.getStatusLine().getStatusCode() == 200) {
					sent = true;
					LOGGER.debug("Успешно изпращане на съобщение към Firebase за userId={}", userId);
				} else {
					LOGGER.error("Грешка при изпращане на съобщение към Firebase: {}", response);
				}
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при изпращане на съобщение към Firebase.", e);
		}
		return sent;
	}

	/** */
	private static boolean init() {
		try (InputStream serviceAccount = Thread.currentThread().getContextClassLoader().getResourceAsStream(FCM_PRIVATE_KEY_FILE)) {

			credentials = ServiceAccountCredentials.fromStream(serviceAccount).createScoped(Arrays.asList(AUTH_SCOPE));

			PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
			cm.setMaxTotal(100); // Increase max total connection to 100
			cm.setDefaultMaxPerRoute(10); // Increase default max connection per route to 10

			RequestConfig config = RequestConfig.custom() // TODO timeout-ите какви да са
				.setConnectTimeout(1 * 1000) //
				.setConnectionRequestTimeout(1 * 1000) //
				.setSocketTimeout(1 * 1000) //
				.build();

			httpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(config).build();

			LOGGER.info("FirebaseClient initialized successfully!");
			return true;

		} catch (Exception e) {
			LOGGER.error("Error !!! FirebaseClient !!! initialization ", e);
		}
		return false;
	}

	/** */
	private FirebaseClient() {
		super();
	}
}
