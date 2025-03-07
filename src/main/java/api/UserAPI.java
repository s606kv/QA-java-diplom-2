package api;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import service.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static service.Utilities.*;

public class UserAPI {

    @Step ("POST. Получение ответа на запрос создания пользователя. Ручка api/auth/register.")
    public Response userCreating (User user) {
        System.out.println("-> Создаётся пользователь.");

        Response response = REQUEST
                .body(user)
                .when()
                .post(USER_CREATE);

        // печатаем информацию о запросе
        printResponseInfo(response, SC_OK, "");

        return response;
    }

    @Step ("Извлечение accessToken после создания пользователя.")
    public String getAccessToken (Response response) {
        System.out.println("-> Получение accessToken.");

        String untrimmedAccessToken = response
                .then()
                .extract()
                .body()
                .path("accessToken")
                .toString();
        String cleanAccessToken = untrimmedAccessToken.substring(7);

        // вывод сообщения в зависимости от исхода запроса
        if(!untrimmedAccessToken.isEmpty()) {
            System.out.println(String.format("\uD83D\uDFE2 accessToken:%n%s%n", cleanAccessToken));
        } else {
            System.out.println("\uD83D\uDFE1 ВНИМАНИЕ. accessToken не получен.\n");
        }

        // проверка наличия accessToken
        assertNotNull(untrimmedAccessToken);

        return cleanAccessToken;
    }

    @Step ("Извлечение refreshToken после создания пользователя.")
    public String getRefreshToken (Response response) {
        System.out.println("-> Получение refreshToken.");

        String refreshToken = response
                .then()
                .extract()
                .body()
                .path("refreshToken")
                .toString();

        // вывод сообщения в зависимости от исхода запроса
        if(!refreshToken.isEmpty()) {
            System.out.println(String.format("\uD83D\uDFE2 refreshToken:%n%s%n", refreshToken));
        } else {
            System.out.println("\uD83D\uDFE1 ВНИМАНИЕ. refreshToken не получен.\n");
        }

        // проверка наличия refreshToken
        assertNotNull(refreshToken);

        return refreshToken;
    }

    @Step ("POST. Получение ответа на запрос логина пользователя. Ручка api/auth/login.")
    public Response loginUser (User user) {
        System.out.println("-> Выполняется вход пользователя в систему.");

        Response response = REQUEST
                .body(user)
                .when()
                .post(USER_LOGIN);

        // печатаем информацию о запросе
        printResponseInfo(response, SC_OK, "");

        return response;
    }

    @Step ("POST. Выход пользователя из системы с проверкой статус-кода и тела ответа. Ручка api/auth/logout.")
    public Response logoutUser (String refreshToken) {
        System.out.println("-> Выполняется выход пользователя из системы.");

        // задаём боди
        String body = String.format("{\"token\":\"%s\"}", refreshToken);

        Response response = REQUEST
                .body(body)
                .when()
                .post(USER_LOGOUT);

        // печатаем информацию о запросе
        printResponseInfo(response, SC_OK, "");

        // проверка статуса и тела ответа
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body( "success", equalTo(true),
                        "message", equalTo("Successful logout")
                );

        return response;
    }

    @Step ("GET. Получение ответа на запрос данных пользователя, проверка статуса и ответа. Ручка api/auth/user.")
    public void getUserData (User user, String accessToken) {
        System.out.println("-> Получение пользовательских данных.");

        Response response = REQUEST
                .auth().oauth2(accessToken)
                .get(USER_DATA);

        // извлекаем информацию из ответа
        String otherInfo = extractUserData(response);

        // печатаем информацию о запросе с данными пользователя
        printResponseInfo(response, SC_OK, otherInfo);

        // проверка статуса и тела ответа
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body( "success", equalTo(true),
                        "user.email", equalTo(user.getEmail()),
                        "user.name", equalTo(user.getName())
                );
    }

    @Step ("PATCH. Получение ответа на запрос изменения данных пользователя. Ручка api/auth/user.")
    public Response changeUserData (User user, String accessToken) {
        System.out.println("-> Меняются данные пользователя.");

        Response response = REQUEST
                .auth().oauth2(accessToken)
                .body(user)
                .when()
                .patch(USER_DATA);

        // извлекаем информацию из ответа
        String otherInfo = extractUserData(response);

        // печатаем информацию о запросе с данными пользователя
        printResponseInfo(response, SC_OK, otherInfo);

        return response;
    }

    @Step ("DELETE. Удаления пользователя с проверкой статус-кода и тела ответа. Ручка api/auth/user.")
    public void deleteUser (String accessToken) {
        System.out.println("-> Удаляется пользователь.");

        Response response = REQUEST
                .auth().oauth2(accessToken)
                .when()
                .delete(USER_DELETE);

        // печатаем информацию о запросе
        printResponseInfo(response, SC_ACCEPTED, "");

        // проверка статуса и тела ответа
        response.then()
                .assertThat()
                .statusCode(SC_ACCEPTED)
                .body(
                        "success", equalTo(true),
                        "message", equalTo("User successfully removed")
                );
    }

}
