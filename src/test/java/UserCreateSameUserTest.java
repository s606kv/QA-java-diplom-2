import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import net.datafaker.Faker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.User;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static service.Utilities.checkUserNegativeResponse;
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
        userAPI.getUserData(user, accessToken);
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
        checkUserNegativeResponse(secondResponse, SC_FORBIDDEN, false, "User already exists");
    }

    @After /// Удаляем пользователя
    public void postconditions () {
        userAPI.deleteUser(accessToken);
    }
}
