package user;

import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import net.datafaker.Faker;
import service.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(Parameterized.class)
public class UserCreateRequiredFieldsNegativeParameterizedTest {
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
    private final String testName;
    // конструктор
    public UserCreateRequiredFieldsNegativeParameterizedTest(String email, String password, String name, String testName) {
        this.email=email;
        this.password=password;
        this.name=name;
        this.testName=testName;
    }

    // параметры
    @Parameterized.Parameters (name="{3}")
    public static Object[][] setData () {
        return new Object[][] {
                {"", fakedPassword, fakedName, "Отсутствует поле \"email\""},
                {fakedEmail, "", fakedName, "Отсутствует поле \"password\""},
                {fakedEmail, fakedPassword, "", "Отсутствует поле \"name\""},
        };
    }

    @Before
    public void preconditions () {
        user = new User(email, password, name);
        response = userAPI.userCreating(user);
    }

    @Test
    @DisplayName("Параметризованный тест создания пользователя с проверкой всех обязательных полей.")
    @Description("Проверяется возможность создать пользователя без указания одного из обязательных полей.")
    public void createUserWithWithoutOneOfRequiredFieldTest () {
        // проверяется тело ответа
        response.then().assertThat()
                .statusCode(SC_FORBIDDEN)
                .body("success", equalTo(false),
                        "message", equalTo("Email, password and name are required fields")
                );
    }

    @After /// Если пользователь вдруг всё-таки создался
    public void deleteUser () {
        if (response.getStatusCode()==SC_OK) {
            String accessToken = userAPI.getAccessToken(response);
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
}
