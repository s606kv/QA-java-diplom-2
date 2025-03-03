import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.datafaker.Faker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.ServiceLinks;
import service.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class UserCreateAndLoginTest {
    private User user;
    private UserAPI userAPI;
    private String accessToken;
    private String refreshToken;
    private Response response;

    // сгенерированы данные
    private static Faker faker = new Faker();
    private String emailAddress = faker.internet().emailAddress();
    private String password = faker.internet().password();
    private String username = faker.name().username();

    @Before
    public void setUp () {
        RestAssured.baseURI = ServiceLinks.BASE_URI;
        userAPI = new UserAPI();

        // создан пользователь
        user = new User(emailAddress, password, username);
        // запрос на создание юзера
        response = userAPI.postForUserCreating(user);
        // получили accessToken
        accessToken = userAPI.getAccessToken(response);
        // получили refreshToken
        refreshToken = userAPI.getRefreshToken(response);
    }

    @Test
    @DisplayName("Позитивная проверка создания пользователя.")
    @Description("Проверяется возможность создать пользователя со всеми обязательными полями.")
    public void userCreatePositiveTest () {

        // проверили статус и тело
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body(
                        "success", equalTo(true),
                        "user.email", equalTo(emailAddress),
                        "user.name", equalTo(username),
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue()
                );
    }

    @Test
    @DisplayName("Негативная проверка создания пользователя, уже существующего в бд.")
    @Description("Проверяется возможность создать пользователя, с теми же самыми данными, с которыми он уже был ранее создан.")
    public void checkImpossibleToCreateSameUser () {

        // проверили статус и частично тело
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .and()
                .body("success", equalTo(true));

        // отправили новый запрос с теми же данными
        Response secondResponse = userAPI.postForUserCreating(user);

        // проверили статус и тело ответа
        secondResponse.then()
                .assertThat()
                .statusCode(SC_FORBIDDEN)
                .body("message", equalTo("User already exists"));
    }

    @Test
    @DisplayName("Позитивная проверка логина пользователя.")
    @Description("Проверяется возможность входа нового созданного пользователя в систему с корректными данными.")
    public void successfulUserLogin () {

        // получили ответ на запрос входа в систему
        Response loginResponse = userAPI.loginUser(user);

        // проверили статус и поля
        loginResponse.then()
                .assertThat()
                .statusCode(SC_OK)
                .body(
                        "success", equalTo(true),
                        "user.email", equalTo(emailAddress),
                        "user.name", equalTo(username),
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue()
                );
    }

    @Test
    @DisplayName("Негативная проверка логина пользователя.")
    @Description("Проверяется возможность входа нового созданного пользователя в систему с некорректными данными.")
    public void unsuccessfulUserLogin () {
        // убедились, что созданный пользователь может войти
        Response loginResponse = userAPI.loginUser(user);
        loginResponse.then()
                .assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true));

        // выходим из системы
        userAPI.logoutUser(refreshToken);

        // поменяли пользователю пароль
        String newPassword = faker.internet().password();
        user.setPassword(newPassword);

        // снова пытаемся войти в систему
        Response negativeResponse = userAPI.loginUser(user);

        // проверяем статус и ответ
        negativeResponse.then()
                .assertThat()
                .statusCode(SC_UNAUTHORIZED)
                .body("success", equalTo(false),
                        "message", equalTo("email or password are incorrect")
                );
    }



    @After
    public void userDelete () {
        userAPI.deleteUser(accessToken);
    }
}
