package userTests;

import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import net.datafaker.Faker;
import service.User;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static service.Utilities.checkNegativeResponse;
import static service.Utilities.checkUserPositiveResponse;

@RunWith(Parameterized.class)
public class UserCreateRequiredFieldsParameterizedTest {
    // фейковые данные
    private static Faker faker = new Faker();
    private static final String fakedEmail = faker.internet().emailAddress();
    private static final String fakedPassword = faker.internet().password();
    private static final String fakedName = faker.name().username();

    // поля класса
    private UserAPI userAPI = new UserAPI();
    private User user;
    private Response response;

    // поля для параметров
    private final String email;
    private final String password;
    private final String name;
    private final boolean successKeyValue;
    private final int status;
    private final String testName;
    // конструктор
    public UserCreateRequiredFieldsParameterizedTest(String email,
                                                     String password,
                                                     String name,
                                                     boolean successKeyValue,
                                                     int status,
                                                     String testName) {
        this.email=email;
        this.password=password;
        this.name=name;
        this.successKeyValue=successKeyValue;
        this.status=status;
        this.testName=testName;
    }

    // параметры
    @Parameterized.Parameters (name="{5}")
    public static Object[][] setData () {
        return new Object[][] {
                {fakedEmail, fakedPassword, fakedName, true, SC_OK, "Обязательные поля заполнены"},
                {"", fakedPassword, fakedName, false, SC_FORBIDDEN, "Отсутствует поле \"email\""},
                {fakedEmail, "", fakedName, false, SC_FORBIDDEN, "Отсутствует поле \"password\""},
                {fakedEmail, fakedPassword, "", false, SC_FORBIDDEN, "Отсутствует поле \"name\""},
        };
    }

    @Before
    public void preconditions () {
        user = new User(email, password, name);
    }

    @Test
    @DisplayName("Параметризованный тест создания пользователя с разными вариантами заполнения обязательных полей.")
    @Description("Проверяется возможность создать пользователя со всеми заполненными полями и без указания одного из обязательных полей.")
    public void createUserWithRequiredFields () {

        // создаётся пользователь с параметризацией
        response = userAPI.userCreating(user);

        // если пользователь создался, что проверяется тело ответа, извлекается accessToken и пользователь удаляется
        if (response.getStatusCode()==SC_OK) {
            checkUserPositiveResponse(response, user, status, successKeyValue);
            String accessToken = userAPI.getAccessToken(response);
            userAPI.deleteUser(accessToken);
        }

        // если пользователь не создался, что просто проверяется тело ответа
        if (response.getStatusCode()==SC_FORBIDDEN) {
            String messageKeyValue = "Email, password and name are required fields";
            checkNegativeResponse(response, status, successKeyValue, messageKeyValue);
        }

    }
}
