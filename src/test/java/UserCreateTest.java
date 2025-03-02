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

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class UserCreateTest {
    private static Faker faker = new Faker();
    private String emailAddress = faker.internet().emailAddress();
    private String password = faker.internet().password();
    private String username = faker.name().username();

    private User user;
    private UserAPI userAPI;

    private String accessToken;

    @Before
    public void setUp () {
        RestAssured.baseURI = ServiceLinks.BASE_URI;
        user = new User(emailAddress, password, username);
        userAPI = new UserAPI();
    }

    @Test
    @DisplayName("Позитивная проверка создания пользователя.")
    @Description("Проверяется возможность создать пользователя со всеми обязательными полями.")
    public void userCreatePositiveTest () {

        // получили респонс создания юзера
        Response response = userAPI.postForUserCreating(user);

        // получили accessToken
        accessToken = userAPI.accessToken(response);

        // проверили статус и тело
        response.then().assertThat()
                .statusCode(SC_OK)
                .and()
                .body("success", equalTo(true))
                .and()
                .body("user.email", equalTo(emailAddress))
                .and()
                .body("user.name", equalTo(username))
                .and()
                .body("accessToken", notNullValue())
                .and()
                .body("refreshToken", notNullValue());
    }

    @Test
    @DisplayName("Негативная проверка создания пользователя, уже существующего в бд.")
    @Description("Проверяется возможность создать пользователя, с теми же самыми данными, с которыми он уже был ранее создан.")
    public void checkImpossibleToCreateSameUser () {

        // получили первый респонс создания юзера
        Response firstResponse = userAPI.postForUserCreating(user);

        // получили accessToken
        accessToken = userAPI.accessToken(firstResponse);

        // проверили статус и частично тело
        firstResponse.then().assertThat()
                .statusCode(SC_OK)
                .and()
                .body("success", equalTo(true));

        // отправили новый запрос с теми же данными
        Response secondResponse = userAPI.postForUserCreating(user);

        // проверили статус и тело ответа
        secondResponse.then().assertThat()
                .statusCode(SC_FORBIDDEN)
                .and()
                .body("message", equalTo("User already exists"));
    }







    @After
    public void userDelete () {
        userAPI.deleteUser(user, accessToken);
    }
}
