import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import net.datafaker.Faker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import service.ServiceLinks;
import service.User;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@RunWith(Parameterized.class)
public class UserLoginParameterizedTest {

    // фейковые данные
    private static Faker faker = new Faker();
    private static final String fakedEmail = faker.internet().emailAddress();
    private static final String fakedPassword = faker.internet().password();
    private static final String fakedName = faker.name().username();
    private static final String newFakedEmail = faker.internet().emailAddress();
    private static final String newFakedPassword = faker.internet().password();

    private UserAPI userAPI = new UserAPI();
    private User user = new User(fakedEmail, fakedPassword, fakedName);

    private final String email;
    private final String password;
    private final boolean successKeyValue;
    private final int status;
    private final String testName;

    private Response response;
    private String refreshToken;
    private String accessToken;

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
                {fakedEmail, fakedPassword, SC_OK, true, "Позитивный кейс логина с актуальными данными"},
                {newFakedEmail, fakedPassword, SC_UNAUTHORIZED, false, "Негативный кейс: неверный логин, верный пароль"},
                {fakedEmail, newFakedPassword, SC_UNAUTHORIZED, false, "Негативный кейс: верный логин, неверный пароль"},
                {fakedEmail, "", SC_UNAUTHORIZED, false, "Негативный кейс: верный логин, пустой пароль"},
                {"", fakedPassword, SC_UNAUTHORIZED, false, "Негативный кейс: пустой логин, верный пароль"},
                {newFakedEmail, newFakedPassword, SC_UNAUTHORIZED, false, "Негативный кейс: неверный логин, неверный пароль"},
        };
    }

    @Before
    public void preconditions () {
        RestAssured.baseURI = ServiceLinks.BASE_URI;
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
                        "user.email", equalTo(fakedEmail),
                        "user.name", equalTo(fakedName),
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue()
                );
        // вышли из системы
        userAPI.logoutUser(refreshToken);
    }

    // метод проверки ответа для негативных наборов параметров
    private void checkBody (User user, int status, boolean successKeyValue) {
        userAPI.loginUser(user).then()
                .assertThat()
                .statusCode(status)
                .body("success", equalTo(successKeyValue),
                        "message", equalTo("email or password are incorrect")
                );
    }

    @Test
    @DisplayName("Параметризованный тест входа пользователя в систему с разными вариантами заполнения полей.")
    @Description("Проверяется возможность входа пользователя с верными данными и с неверным логином или паролем.")
    public void userLoginTest () {

        /** Определяем условия для негативных проверок. */
        // если неверный емэйл
        if (email!=user.getEmail() && password==user.getPassword()) {
            user.setEmail(newFakedEmail);
            System.out.println(String.format("Сменили в запросе поле email на \"%s\".%n", email));
            checkBody(user, status, successKeyValue);
        }
        // если неверный пароль
        else if (email==user.getEmail() && password!=user.getPassword()) {
            user.setPassword(newFakedPassword);
            System.out.println(String.format("Сменили в запросе поле password на \"%s\".%n", password));
            checkBody(user, status, successKeyValue);
        }
        // если неверны оба поля
        else if (email!=user.getEmail() && password!=user.getPassword()) {
            user.setEmail(email);
            user.setPassword(newFakedPassword);
            System.out.println(String.format("Передали в запросе неверные поля email и password.%nНовый email в запросе: \"%s\".%nНовый password в запросе: \"%s\".%n", email, password));
            checkBody(user, status, successKeyValue);
        }
        // если всё нормально
        else {
            System.out.println("Позитивная проверка прошла успешно.\n");
        }
    }

    @After // удаляем пользователя
    public void postconditions () {
        userAPI.deleteUser(accessToken);
    }
}
