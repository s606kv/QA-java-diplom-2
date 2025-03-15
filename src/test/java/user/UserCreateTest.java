package user;

import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import net.datafaker.Faker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static service.Utilities.checkNegativeResponse;

public class UserCreateTest {
    // поля класса
    private Faker faker;
    private String email;
    private String password;
    private String username;
    private Response userCreatingResponse;
    private User user;
    private UserAPI userAPI;
    private String accessToken;

    @Before
    public void setUp () {
        userAPI = new UserAPI();
        // фейковые данные
        faker = new Faker();
        email = faker.internet().emailAddress();
        password = faker.internet().password();
        username = faker.name().username();
        // создаётся пользователь
        user = new User(email, password, username);
        userCreatingResponse = userAPI.userCreating(user);
        // получение токена
        accessToken = userAPI.getAccessToken(userCreatingResponse);
    }

    @Test
    @DisplayName("Проверка создания пользователя с валидными данными во всех обязательных полях.")
    @Description("Проверяется возможность создать пользователя с валидными данными во всех обязательных полях.")
    public void createUserWithAllRequiredFieldsTest () {
        /// Проверка статуса и тела ответа
        userCreatingResponse.then().assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "user.email", equalTo(user.getEmail()),
                        "user.name", equalTo(user.getName()),
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue()
                );
    }

    @Test
    @DisplayName("Негативная проверка повторного создания пользователя, уже существующего в бд.")
    @Description("Проверяется возможность создать пользователя, с теми же самыми данными, с которыми он уже был ранее создан.")
    public void createSameUserTest () {
        System.out.println("\uD83D\uDD35 Попытка создать пользователя с теми же данными.\n");

        // отправляем повторный запрос с теми же данными
        Response secondResponse = userAPI.userCreating(user);

        // проверили статус и тело ответа
        checkNegativeResponse(secondResponse, SC_FORBIDDEN, false, "User already exists");
    }

    @After /// Удаляем пользователя
    public void postconditions () {
        Response deleteUserResponse = userAPI.deleteUser(accessToken);
        // проверка статуса и тела ответа
        deleteUserResponse.then()
                .assertThat()
                .statusCode(SC_ACCEPTED)
                .body(
                        "success", equalTo(true),
                        "message", equalTo("User successfully removed")
                );
    }
}
