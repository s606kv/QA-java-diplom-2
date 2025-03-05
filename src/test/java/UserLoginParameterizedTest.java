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

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@RunWith(Parameterized.class)
public class UserLoginParameterizedTest {

    // фейковые данные
    private static Faker faker = new Faker();
    private static final String userEmail = faker.internet().emailAddress();
    private static final String userPassword = faker.internet().password();
    private static final String userName = faker.name().username();
    private static final String newUserEmail = faker.internet().emailAddress();
    private static final String newUserPassword = faker.internet().password();

    // переменные класса
    private UserAPI userAPI;
    private User user;
    private Response response;
    private String refreshToken;
    private String accessToken;

    // переменные параметров
    private final String email;
    private final String password;
    private final boolean successKeyValue;
    private final int status;
    private final String testName;
    // конструктор
    public UserLoginParameterizedTest (String email,
                                       String password,
                                       int status,
                                       boolean successKeyValue,
                                       String testName) {
        this.email=email;
        this.password=password;
        this.status=status;
        this.successKeyValue=successKeyValue;
        this.testName=testName;
    }

    // параметры
    @Parameterized.Parameters (name="{4}")
    public static Object[][] data () {
        return new Object[][] {
                {userEmail, userPassword, SC_OK, true, "Позитивный кейс логина с актуальными данными"},
                {newUserEmail, userPassword, SC_UNAUTHORIZED, false, "Негативный кейс: неверный логин, верный пароль"},
                {userEmail, newUserPassword, SC_UNAUTHORIZED, false, "Негативный кейс: верный логин, неверный пароль"},
                {userEmail, "", SC_UNAUTHORIZED, false, "Негативный кейс: верный логин, пустой пароль"},
                {"", userPassword, SC_UNAUTHORIZED, false, "Негативный кейс: пустой логин, верный пароль"},
                {newUserEmail, newUserPassword, SC_UNAUTHORIZED, false, "Негативный кейс: неверный логин, неверный пароль"},
        };
    }

    @Before
    public void preconditions () {
        userAPI = new UserAPI();
        user = new User(userEmail, userPassword, userName);
        // запрос на создание юзера
        response = userAPI.userCreating(user);
        // получили accessToken
        accessToken = userAPI.getAccessToken(response);
        // получили refreshToken
        refreshToken = userAPI.getRefreshToken(response);
        // отобразили данные пользователя
        userAPI.getUserData(user, accessToken);
        // вошли в систему, проверили статус и ответ
        userAPI.loginUser(user)
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .body(
                        "success", equalTo(true),
                        "user.email", equalTo(userEmail),
                        "user.name", equalTo(userName),
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue()
                );
        // вышли из системы
        userAPI.logoutUser(refreshToken);
    }

    @Test
    @DisplayName("Параметризованный тест входа пользователя в систему с разными вариантами заполнения полей.")
    @Description("Проверяется возможность входа пользователя с верными данными и с неверным логином или паролем.")
    public void userLoginTest () {

        /// Определяем условия для негативных проверок.
        // если данные не менялись, то проверяется просто вход в систему
        if (email.equals(user.getEmail()) && password.equals(user.getPassword())) {
            System.out.println("Данные не менялись. " +
                    "Проверка повторного входа не требуется.\n");
            return;
        }

        // если заменили в запросе только емэйл
        if (!email.equals(user.getEmail()) && password.equals(user.getPassword())) {
            user.setEmail(newUserEmail);
            System.out.println(String.format("Сменили в запросе поле email на \"%s\".%n", email));
        }

        // если заменили в запросе только пароль
        if (email.equals(user.getEmail()) && !password.equals(user.getPassword())) {
            user.setPassword(newUserPassword);
            System.out.println(String.format("Сменили в запросе поле password на \"%s\".%n", password));
        }

        // если заменили в запросе оба поля
        if (!email.equals(user.getEmail()) && !password.equals(user.getPassword())) {
            user.setEmail(newUserEmail);
            user.setPassword(newUserPassword);
            System.out.println(String.format("Передали в запросе неверные поля email и password.%n" +
                    "Новый email в запросе: \"%s\".%n" +
                    "Новый password в запросе: \"%s\".%n", email, password));
        }

        // выполняем повторный логин в систему
        Response negativeResponse = userAPI.loginUser(user);
        // проверка статуса и ответа
        negativeResponse.then()
                .assertThat()
                .statusCode(status)
                .body("success", equalTo(successKeyValue),
                        "message", equalTo("email or password are incorrect")
                );
    }

    @After /// Удаляем пользователя
    public void postconditions () {
        userAPI.deleteUser(accessToken);
    }
}
