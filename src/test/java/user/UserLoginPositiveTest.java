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
import static service.Utilities.checkUserPositiveResponse;

public class UserLoginPositiveTest {
    // фейковые данные
    private static Faker faker = new Faker();
    private static String email = faker.internet().emailAddress();
    private static String password = faker.internet().password();
    private static String name = faker.name().username();
    // поля класса
    private UserAPI userAPI;
    private String accessToken;
    private User user;

    @Before
    public void preconditions () {
        userAPI = new UserAPI();
        user = new User(email, password, name);
        // создали пользователя
        Response userCreatingResponse = userAPI.userCreating(user);
        // проверили статус и тело
        checkUserPositiveResponse(userCreatingResponse, user, SC_OK, true);
        // получили токен
        accessToken = userAPI.getAccessToken(userCreatingResponse);
        // отобразили данные пользователя
        Response getUserDataResponse =  userAPI.getUserData(user, accessToken);
        // проверка статуса и тела ответа
        getUserDataResponse.then().assertThat()
                .statusCode(SC_OK)
                .body( "success", equalTo(true),
                        "user.email", equalTo(user.getEmail()),
                        "user.name", equalTo(user.getName())
                );
    }

    @Test
    @DisplayName("Тест входа пользователя в систему с валидными данными.")
    @Description("Проверяется возможность входа пользователя с верными данными.")
    public void userPositiveLoginTest () {
        // вошли в систему
        Response loginUserResponse = userAPI.loginUser(user);
        // проверили статус и тело
        loginUserResponse.then().assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue(),
                        "user.email", equalTo(user.getEmail()),
                        "user.name", equalTo(user.getName())
                );
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
