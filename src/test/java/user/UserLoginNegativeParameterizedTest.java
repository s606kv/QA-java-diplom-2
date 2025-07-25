package user;

import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import net.datafaker.Faker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import service.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static service.Utilities.checkUserPositiveResponse;

@RunWith(Parameterized.class)
public class UserLoginNegativeParameterizedTest {
    // фейковые данные
    private static Faker faker = new Faker();
    private static String userEmail = faker.internet().emailAddress();
    private static String userPassword = faker.internet().password();
    private static String userName = faker.name().username();
    private static String newUserEmail = faker.internet().emailAddress();
    private static String newUserPassword = faker.internet().password();

    // переменные класса
    private UserAPI userAPI;
    private User user;
    private Response response;
    private String refreshToken;
    private String accessToken;

    // переменные параметров
    private final String email;
    private final String password;
    private final String testName;
    // конструктор
    public UserLoginNegativeParameterizedTest(String email, String password, String testName) {
        this.email=email;
        this.password=password;
        this.testName=testName;
    }

    // параметры
    @Parameterized.Parameters (name="{2}")
    public static Object[][] data () {
        return new Object[][] {
                {newUserEmail, userPassword, "Неверный логин, верный пароль"},
                {userEmail, newUserPassword, "Верный логин, неверный пароль"},
                {userEmail, "", "Верный логин, пустой пароль"},
                {"", userPassword, "Пустой логин, верный пароль"},
                {newUserEmail, newUserPassword, "Неверный логин, неверный пароль"},
        };
    }

    @Before
    public void preconditions () {
        userAPI = new UserAPI();
        user = new User(userEmail, userPassword, userName);
        // создали пользователя
        response = userAPI.userCreating(user);
        // проверили статус и тело
        checkUserPositiveResponse(response, user, SC_OK, true);
        // получили токены
        accessToken = userAPI.getAccessToken(response);
        refreshToken = userAPI.getRefreshToken(response);
        // отобразили данные пользователя
        Response getUserDataResponse =  userAPI.getUserData(user, accessToken);
        // проверка статуса и тела ответа
        getUserDataResponse.then().assertThat()
                .statusCode(SC_OK)
                .body( "success", equalTo(true),
                        "user.email", equalTo(user.getEmail()),
                        "user.name", equalTo(user.getName())
                );
        // вошли в систему, чтобы убедиться, что созданный пользователь имеет доступ
        response = userAPI.loginUser(user);
        // проверили статус и тело
        checkUserPositiveResponse(response, user, SC_OK, true);
        // вышли из системы
        Response logoutUserResponse = userAPI.logoutUser(refreshToken);
        // проверили статус и тело
        logoutUserResponse.then()
                .assertThat()
                .statusCode(SC_OK)
                .body( "success", equalTo(true),
                        "message", equalTo("Successful logout")
                );
    }

    @Test
    @DisplayName("Параметризованный тест входа пользователя в систему с разными вариантами заполнения полей.")
    @Description("Проверяется возможность входа пользователя с верными данными и с неверным логином или паролем.")
    public void userLoginNegativeTest () {
        /// Условия для проверок
        // заменили в запросе только емэйл
        if (!email.equals(userEmail) && password.equals(userPassword)) {
            user.setEmail(email);
            System.out.println(String.format("\uD83D\uDD35 Поле email изменено в запросе на \"%s\"%n", email));
        }

        // заменили в запросе только пароль
        if (email.equals(userEmail) && !password.equals(userPassword)) {
            user.setPassword(password);
            System.out.println(String.format("\uD83D\uDD35 Поле password изменено в запросе на \"%s\"%n", password));
        }

        // заменили в запросе оба поля
        if (!email.equals(userEmail) && !password.equals(userPassword)) {
            user.setEmail(email);
            user.setPassword(password);
            System.out.println(String.format("\uD83D\uDD35 В запросе изменены оба поля.%n" +
                    "Новый email в запросе: %s%n" +
                    "Новый password в запросе: %s%n", email, password));
        }

        /// Проверяем повторный вход в систему с новыми данными
        Response secondResponse = userAPI.loginUser(user);
        // проверяем статус и ответ
        secondResponse.then().assertThat()
                .statusCode(SC_UNAUTHORIZED)
                .body("success", equalTo(false),
                        "message", equalTo("email or password are incorrect")
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
