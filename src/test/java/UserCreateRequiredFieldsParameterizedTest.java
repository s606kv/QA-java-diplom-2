import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import net.datafaker.Faker;
import service.ServiceLinks;
import service.User;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(Parameterized.class)
public class UserCreateRequiredFieldsParameterizedTest {

    private static Faker faker = new Faker();
    private UserAPI userAPI = new UserAPI();
    private User user;

    // поля для задания параметров
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
                {faker.internet().emailAddress(), faker.internet().password(), faker.name().username(), true, SC_OK, "Обязательные поля заполнены"},
                {"", faker.internet().password(), faker.name().username(), false, SC_FORBIDDEN, "Отсутствует поле \"email\""},
                {faker.internet().emailAddress(), "", faker.name().username(), false, SC_FORBIDDEN, "Отсутствует поле \"password\""},
                {faker.internet().emailAddress(), faker.internet().password(), "", false, SC_FORBIDDEN, "Отсутствует поле \"name\""},
        };
    }

    @Before
    public void setUp () {
        RestAssured.baseURI = ServiceLinks.BASE_URI;
        user = new User(email, password, name);
    }

    @Test
    @DisplayName("Параметризованный тест создания пользователя с разными вариантами заполнения обязательных полей.")
    @Description("Проверяется возможность создать пользователя без указания одного из обязательных полей.")
    public void impossibleToCreateUserWithoutRequiredField () {

        // попытка создать пользователя без требуемых полей
        Response response = userAPI.userCreating(user);

        // проверили статус и тело ответа
        response.then().assertThat()
                .statusCode(status)
                .and()
                .body(
                        "success", equalTo(successKeyValue)
                );

        // если пользователь создался, то удаляем его
        if (status == 200) {
            String accessToken = userAPI.getAccessToken(response);
            userAPI.deleteUser(accessToken);
        }
    }
}
