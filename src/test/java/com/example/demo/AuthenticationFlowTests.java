package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowTests {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createUserHashesPasswordAndOmitsItFromResponse() throws Exception {
		mockMvc.perform(post("/api/user/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "alice",
						  "password": "password1",
						  "confirmPassword": "password1"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void createUserRejectsShortPassword() throws Exception {
		mockMvc.perform(post("/api/user/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "bob",
						  "password": "short",
						  "confirmPassword": "short"
						}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createUserRejectsDuplicateUsernameAndMismatchedConfirmation() throws Exception {
		createUser("duplicate-user", "password1");

		mockMvc.perform(post("/api/user/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "duplicate-user",
						  "password": "password1",
						  "confirmPassword": "password1"
						}
						"""))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/user/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "mismatch-user",
						  "password": "password1",
						  "confirmPassword": "password2"
						}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void loginReturnsJwtAuthorizationHeader() throws Exception {
		createUser("carol", "password1");

		mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "carol",
						  "password": "password1"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(header().string("Authorization", org.hamcrest.Matchers.startsWith("Bearer ")));
	}

	@Test
	void protectedEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/item"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userCanReadOwnProfileButNotAnotherUsersProfile() throws Exception {
		createUser("dave", "password1");
		createUser("erin", "password1");
		String token = login("dave", "password1");

		mockMvc.perform(get("/api/user/dave")
				.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("dave"));

		mockMvc.perform(get("/api/user/erin")
				.header("Authorization", token))
				.andExpect(status().isForbidden());
	}

	@Test
	void userCanReadOwnProfileByIdButNotAnotherUsersId() throws Exception {
		long ivanId = createUserAndReturnId("ivan", "password1");
		long janeId = createUserAndReturnId("jane", "password1");
		String token = login("ivan", "password1");

		mockMvc.perform(get("/api/user/id/" + ivanId)
				.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("ivan"));

		mockMvc.perform(get("/api/user/id/" + janeId)
				.header("Authorization", token))
				.andExpect(status().isForbidden());
	}

	@Test
	void userCanModifyOwnCartButNotAnotherUsersCart() throws Exception {
		createUser("frank", "password1");
		createUser("grace", "password1");
		String token = login("frank", "password1");

		mockMvc.perform(post("/api/cart/addToCart")
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "frank",
						  "itemId": 1,
						  "quantity": 2
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.total").value(5.98));

		mockMvc.perform(post("/api/cart/addToCart")
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "grace",
						  "itemId": 1,
						  "quantity": 1
						}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void userCanRemoveItemsFromOwnCart() throws Exception {
		createUser("kate", "password1");
		String token = login("kate", "password1");

		mockMvc.perform(post("/api/cart/addToCart")
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "kate",
						  "itemId": 1,
						  "quantity": 2
						}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/cart/removeFromCart")
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "kate",
						  "itemId": 1,
						  "quantity": 1
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.total").value(2.99));
	}

	@Test
	void authenticatedUserCanBrowseItems() throws Exception {
		createUser("lucy", "password1");
		String token = login("lucy", "password1");

		mockMvc.perform(get("/api/item")
				.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Round Widget"));

		mockMvc.perform(get("/api/item/1")
				.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Round Widget"));

		mockMvc.perform(get("/api/item/name/Round Widget")
				.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].description").value("A widget that is round"));

		mockMvc.perform(get("/api/item/name/DoesNotExist")
				.header("Authorization", token))
				.andExpect(status().isNotFound());
	}

	@Test
	void userCanSubmitAndReadOwnOrderHistory() throws Exception {
		createUser("henry", "password1");
		String token = login("henry", "password1");

		mockMvc.perform(post("/api/cart/addToCart")
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "henry",
						  "itemId": 2,
						  "quantity": 1
						}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/order/submit/henry")
				.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.username").value("henry"))
				.andExpect(jsonPath("$.total").value(1.99));

		mockMvc.perform(get("/api/order/history/henry")
				.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].user.username").value("henry"));
	}

	private void createUser(String username, String password) throws Exception {
		mockMvc.perform(post("/api/user/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "%s",
						  "password": "%s",
						  "confirmPassword": "%s"
						}
						""".formatted(username, password, password)))
				.andExpect(status().isOk());
	}

	private long createUserAndReturnId(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/user/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "%s",
						  "password": "%s",
						  "confirmPassword": "%s"
						}
						""".formatted(username, password, password)))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode body = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
		return body.get("id").asLong();
	}

	private String login(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "%s",
						  "password": "%s"
						}
						""".formatted(username, password)))
				.andExpect(status().isOk())
				.andReturn();
		return result.getResponse().getHeader("Authorization");
	}
}
