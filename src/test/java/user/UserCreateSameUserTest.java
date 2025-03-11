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
import static service.Utilities.checkNegativeResponse;
import static service.Utilities.checkUserPositiveResponse;

public class UserCreateSameUserTest {
    // сгенерированы данные
    private static Faker faker = new Faker();
    private String emailAddress = faker.internet().emailAddress();
    private String password = faker.internet().password();
    private String username = faker.name().username();

    // поля класса
    private User user = new User(emailAddress, password, username);
    private UserAPI userAPI = new UserAPI();
    private String accessToken;
    private Response response;

    @Before
    public void preconditions () {
        // создали пользователя
        response = userAPI.userCreating(user);
        // проверили статус и тело
        checkUserPositiveResponse(response, user, SC_OK, true);
        // получили accessToken
        accessToken = userAPI.getAccessToken(response);
        // Отобразили данные пользователя
        Response getUserDataResponse =  userAPI.getUserData(user, accessToken);
        // проверка статуса и тела ответа
        getUserDataResponse.then()
                .assertThat()
                .statusCode(SC_OK)
                .body( "success", equalTo(true),
                        "user.email", equalTo(user.getEmail()),
                        "user.name", equalTo(user.getName())
                );
    }

    @Test
    @DisplayName("Негативная проверка создания пользователя, уже существующего в бд.")
    @Description("Проверяется возможность создать пользователя, с теми же самыми данными, с которыми он уже был ранее создан.")
    public void createSameUserTest () {
        // сообщение в консоли
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
