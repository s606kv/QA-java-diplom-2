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
import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(Parameterized.class)
public class CreateUserRequiredFieldsParameterizedTest {

    private static Faker faker = new Faker();
    private UserAPI userAPI = new UserAPI();;
    private User user;

    private final String email;
    private final String password;
    private final String name;
    private final String testName;



    public CreateUserRequiredFieldsParameterizedTest (String email, String password, String name, String testName) {
        this.email=email;
        this.password=password;
        this.name=name;
        this.testName=testName;
    }

    @Parameterized.Parameters (name="{3}")
    public static Object[][] setData () {
        return new Object[][] {
                {"", faker.internet().password(), faker.name().username(), "Отсутствует поле \"email\""},
                {faker.internet().emailAddress(), "", faker.name().username(), "Отсутствует поле \"password\""},
                {faker.internet().emailAddress(), faker.internet().password(), "", "Отсутствует поле \"name\""},
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
        Response response = userAPI.postForUserCreating(user);

        // проверили статус и тело ответа
        response.then().assertThat()
                .statusCode(SC_FORBIDDEN)
                .and()
                .body("message", equalTo("Email, password and name are required fields"));
    }
}
