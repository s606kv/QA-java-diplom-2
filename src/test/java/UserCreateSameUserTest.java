import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.datafaker.Faker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.Utilities;
import service.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

public class UserCreateSameUserTest {
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
    public void preconditions () {
        RestAssured.baseURI = Utilities.BASE_URI;
        userAPI = new UserAPI();

        // создан пользователь
        user = new User(emailAddress, password, username);
        // Запрос на создание юзера. POST
        response = userAPI.userCreating(user);
        // получили accessToken
        accessToken = userAPI.getAccessToken(response);
        // получили refreshToken
        refreshToken = userAPI.getRefreshToken(response);
        // Отобразили данные пользователя. GET
        userAPI.getUserData(user, accessToken);
    }

    @Test
    @DisplayName("Негативная проверка создания пользователя, уже существующего в бд.")
    @Description("Проверяется возможность создать пользователя, с теми же самыми данными, с которыми он уже был ранее создан.")
    public void createSameUserTest () {
        // проверили статус и частично тело
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .and()
                .body("success", equalTo(true));

        System.out.println("\uD83D\uDD35 Попытка создать пользователя с теми же данными.\n");

        // отправляем повторный запрос с теми же данными
        Response secondResponse = userAPI.userCreating(user);
        // проверили статус и тело ответа
        secondResponse.then()
                .assertThat()
                .statusCode(SC_FORBIDDEN)
                .body("message", equalTo("User already exists"));
    }

    @After /// Удаляем пользователя
    public void postconditions () {
        userAPI.deleteUser(accessToken);
    }
}
