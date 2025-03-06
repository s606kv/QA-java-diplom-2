package userTests;

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
import static service.Utilities.checkNegativeResponse;
import static service.Utilities.checkUserPositiveResponse;

@RunWith(Parameterized.class)
public class UserPatchDataParameterizedTest {
    // фейковые данные
    private static Faker faker = new Faker();
    private static final String userEmail = faker.internet().emailAddress();
    private static final String userPassword = faker.internet().password();
    private static final String userName = faker.name().username();
    private static final String newUserEmail = faker.internet().emailAddress();
    private static final String newUserName = faker.internet().password();

    // переменные класса
    private UserAPI userAPI = new UserAPI();
    private User user;
    private Response response;
    private String accessToken;

    // переменные параметров
    private final String email;
    private final String name;
    private final boolean successKeyValue;
    private final int status;
    private final String testName;
    // конструктор
    public UserPatchDataParameterizedTest (String email,
                                       String name,
                                       int status,
                                       boolean successKeyValue,
                                       String testName) {
        this.email=email;
        this.name=name;
        this.status=status;
        this.successKeyValue=successKeyValue;
        this.testName=testName;
    }

    // параметры
    @Parameterized.Parameters (name="{4}")
    public static Object[][] data () {
        return new Object[][] {
                {userEmail, userName, SC_OK, true, "Позитивный кейс: данные не меняли"},
                {newUserEmail, userName, SC_OK, true, "Позитивный кейс: меняем емэйл, не меняем имя"},
                {userEmail, newUserName, SC_OK, true, "Позитивный кейс: не меняем емэйл, меняем имя"},
                {newUserEmail, newUserName, SC_OK, true, "Позитивный кейс: меняются оба поля"}
        };
    }

    @Before
    public void preconditions () {
        user = new User(userEmail, userPassword, userName);
        // запрос на создание юзера
        response = userAPI.userCreating(user);
        // проверили статус и тело
        checkUserPositiveResponse(response, user, SC_OK, true);
        // получили accessToken
        accessToken = userAPI.getAccessToken(response);
        // отобразили данные пользователя
        userAPI.getUserData(user, accessToken);
    }

    @Test
    @DisplayName("Параметризованный тест смены пользовательских данных для авторизованного пользователя.")
    @Description("Проверяется возможность смены данных авторизованного пользователя.")
    public void userAuthorizedPatchDataTest () {

         /// Определяем условия для проверок.
        // если данные не менялись
        if (email.equals(userEmail) && name.equals(userName)) {
            System.out.println("\uD83D\uDD35 Данные в запросе не изменены.\nОтправляется запрос на изменение.\n");
        }

        // меняем только емэйл
        if (!email.equals(userEmail) && name.equals(userName)) {
            user.setEmail(email);
            System.out.println(String.format("\uD83D\uDD35 Сменили в запросе поле email на \"%s\".%n", email));
        }

        // меняем только имя
        if (email.equals(userEmail) && !name.equals(userName)) {
            user.setName(name);
            System.out.println(String.format("\uD83D\uDD35 Сменили в запросе поле name на \"%s\".%n", name));
        }

        // меняем оба поля
        if (!email.equals(userEmail) && !name.equals(userName)) {
            user.setEmail(email);
            user.setName(name);
            System.out.println(String.format("\uD83D\uDD35 Сменили в запросе оба поля.%n" +
                    "Новый email в запросе: \"%s\".%n" +
                    "Новый name в запросе: \"%s\".%n", email, name));
        }

        // запрос на изменение данных и вывод на экран новых данных
        Response patchResponse = userAPI.changeUserData(user, accessToken);
        // проверяем, что в теле ответа поля email и name совпадают с переданными в патч-запросе
        patchResponse.then()
                .assertThat()
                .statusCode(status)
                .body("success", equalTo(successKeyValue),
                        "user.email", equalTo(user.getEmail()),
                        "user.name", equalTo(user.getName())
                );
    }

    @Test
    @DisplayName("Параметризованный тест смены пользовательских данных для неавторизованного пользователя.")
    @Description("Проверяется возможность смены данных неавторизованного пользователя.")
    public void userNonAuthorizedPatchDataTest () {
        // запрос на изменение данных без авторизации
        System.out.println("\uD83D\uDD35 Попытка замены данных без токена авторизации.\n");
        Response patchResponse = userAPI.changeUserData(user, "");

        // проверка статуса и тела
        String messageKeyValue = "You should be authorised";
        checkNegativeResponse(patchResponse, SC_UNAUTHORIZED, false, messageKeyValue);
    }

    @After /// Удаляем пользователя
    public void postconditions () {
        userAPI.deleteUser(accessToken);
    }
}
