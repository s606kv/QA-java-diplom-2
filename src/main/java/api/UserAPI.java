package api;

import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import service.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;
import static service.ServiceLinks.*;
import static io.restassured.RestAssured.given;

public class UserAPI {

    @Step ("Получение ответа на POST запрос создания пользователя. Ручка api/auth/register.")
    public Response userCreating (User user) {
        System.out.println("-> Создаётся пользователь.");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post(USER_CREATE);

        // вывод сообщения в зависимости от исхода запроса
        String responseBody = response
                .then()
                .extract()
                .body()
                .asString();
        int statusCode = response.getStatusCode();
        String info = (statusCode == SC_OK)
                ? String.format("Статус-код: %d.%nСоздан пользователь.%n", statusCode)
                : String.format("⚠\uFE0F ВНИМАНИЕ. Статус: %d.%nТело ответа: %s.%nПользователь не создан.%n", statusCode, responseBody);
        System.out.println(info);

        return response;
    }

    @Step ("Получение accessToken после создания пользователя.")
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
            System.out.println(String.format("Получен accessToken:%n%s%n", cleanAccessToken));
        } else {
            System.out.println("⚠\uFE0F ВНИМАНИЕ. accessToken не получен.\n");
        }

        // проверка наличия accessToken
        assertNotNull(untrimmedAccessToken);

        return cleanAccessToken;
    }

    @Step ("Получение refreshToken после создания пользователя.")
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
            System.out.println(String.format("Получен refreshToken:%n%s%n", refreshToken));
        } else {
            System.out.println("⚠\uFE0F ВНИМАНИЕ. refreshToken не получен.\n");
        }

        // проверка наличия refreshToken
        assertNotNull(refreshToken);

        return refreshToken;
    }

    @Step ("Получение ответа на POST запрос логина пользователя. Ручка api/auth/login.")
    public Response loginUser (User user) {
        System.out.println("-> Выполняется вход пользователя в систему.");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post(USER_LOGIN);

        // вывод сообщения в зависимости от исхода запроса
        String responseBody = response
                .then()
                .extract()
                .body()
                .asString();
        int statusCode = response.getStatusCode();
        String info = (statusCode == SC_OK)
                ? String.format("Статус-код: %d.%nПользователь залогинен.%n", statusCode)
                : String.format("⚠\uFE0F ВНИМАНИЕ. Статус: %d.%nТело ответа: %s.%nПользователь не залогинен.%n", statusCode, responseBody);
        System.out.println(info);

        return response;
    }

    @Step ("Выход пользователя из системы с проверкой статус-кода и тела ответа. Ручка api/auth/logout.")
    public Response logoutUser (String refreshToken) {
        System.out.println("-> Выполняется выход пользователя из системы.");

        // задаём боди
        String body = String.format("{\"token\":\"%s\"}", refreshToken);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(USER_LOGOUT);

        // вывод сообщения в зависимости от исхода запроса
        String responseBody = response
                .then()
                .extract()
                .body()
                .asString();
        int statusCode = response.getStatusCode();
        String info = (statusCode == SC_OK)
                ? String.format("Статус-код: %d.%nПользователь вышел из системы.%n", statusCode)
                : String.format("⚠\uFE0F ВНИМАНИЕ. Статус-код не совпал с ожидаемым.%nТело ответа: %s.%nПользователь не вышел из системы.%n", responseBody);
        System.out.println(info);

        // проверка статуса и тела ответа
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body( "success", equalTo(true),
                        "message", equalTo("Successful logout")
                );

        return response;
    }

    @Step ("Получение ответа на GET запрос данных пользователя, проверка статуса и ответа. Ручка api/auth/user.")
    public void getUserData (User user, String accessToken) {
        System.out.println("-> Получение пользовательских данных.");

        Response response = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .get(USER_DATA);

        // вывод сообщения в зависимости от исхода запроса
        String responseBody = response
                .then()
                .extract()
                .body()
                .asString();
        int statusCode = response.getStatusCode();
        String info = (statusCode == SC_OK)
                ? String.format("Статус-код: %d.%nДанные пользователя получены:%nemail: %s%nимя: %s%n", statusCode, user.getEmail(), user.getName())
                : String.format("⚠\uFE0F ВНИМАНИЕ. Статус-код не совпал с ожидаемым.%nТело ответа: %s.%nДанные о пользователе не получены.%n", responseBody);
        System.out.println(info);

        // проверка статуса и тела ответа
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body( "success", equalTo(true),
                        "user.email", notNullValue(),
                        "user.name", notNullValue()
                );
    }

    @Step ("Удаления пользователя с проверкой статус-кода и тела ответа. Ручка api/auth/user.")
    public void deleteUser (String accessToken) {
        System.out.println("-> Удаляется пользователь.");

        Response response = given()
                .contentType(ContentType.JSON)
                .auth().oauth2(accessToken)
                .when()
                .delete(USER_DELETE);

        // вывод сообщения в зависимости от исхода запроса
        String responseBody = response
                .then()
                .extract()
                .body()
                .asString();
        int statusCode = response.getStatusCode();
        String info = (statusCode == SC_ACCEPTED)
                ? String.format("Статус-код: %d.%nПользователь удалён.%n", statusCode)
                : String.format("⚠\uFE0F ВНИМАНИЕ. Статус-код не совпал с ожидаемым.%nТело ответа: %s.%nПользователь не удалён.%n", responseBody);
        System.out.println(info);

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
