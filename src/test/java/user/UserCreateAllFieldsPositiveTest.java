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

import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class UserCreateAllFieldsPositiveTest {
    // фейковые данные
    private Faker faker = new Faker();
    private String email = faker.internet().emailAddress();
    private String password = faker.internet().password();
    private String name = faker.name().username();

    private Response response;
    private User user;
    private UserAPI userAPI;
    private String accessToken;

    @Before
    public void setUp () {
        userAPI = new UserAPI();
        // создаётся пользователь
        user = new User(email, password, name);
        response = userAPI.userCreating(user);
    }

    @Test
    @DisplayName("Проверка создания пользователя с валидными данными во всех обязательных полях.")
    @Description("Проверяется возможность создать пользователя с валидными данными во всех обязательных полях.")
    public void createUserWithAllRequiredFieldsTest () {
        /// Проверка статуса и тела ответа
        response.then().assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "user.email", equalTo(user.getEmail()),
                        "user.name", equalTo(user.getName()),
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue()
                );

        // получение токена
        accessToken = userAPI.getAccessToken(response);
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
