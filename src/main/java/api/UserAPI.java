package api;

import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import service.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static service.ServiceLinks.*;
import static io.restassured.RestAssured.given;

public class UserAPI {
    private RequestSpecification request = given().contentType(ContentType.JSON);

    // сервисный метод печати информации в зависимости от статус-кода
    public void printResponseInfo (Response response, int expectedStatusCode, String otherInfo) {
        // формируем тело ответа
        String responseBody = response
                .then()
                .extract()
                .body()
                .asString();
        // получаем статус-код
        int actualStatusCode = response.getStatusCode();
        // печатаем результат запроса
        String info = (actualStatusCode == expectedStatusCode)
                ? String.format("Статус-код: %d.%nУспешный запрос.%n%s", actualStatusCode, otherInfo)
                : String.format("⚠\uFE0F ВНИМАНИЕ. Статус: %d.%nТело ответа: %s.%nЗапрос некорректный.%n", actualStatusCode, responseBody);
        System.out.println(info);
    }

    // сервисный метод извлечения данных пользователя из тела ответа
    public String extractUserData (Response response) {
        // извлекаем емэйл
        String jsonEmail = response.then().extract().body().path("user.email");
        // извлекаем имя
        String jsonName = response.then().extract().body().path("user.name");
        // создаём информацию с данными пользователя
        String extractedInfo = String.format("Данные пользователя:%nemail: %s%nимя: %s%n", jsonEmail, jsonName);

        return extractedInfo;
    };

    @Step ("POST. Получение ответа на запрос создания пользователя. Ручка api/auth/register.")
    public Response userCreating (User user) {
        System.out.println("-> Создаётся пользователь.");

        Response response = request
                .body(user)
                .when()
                .post(USER_CREATE);

        // печатаем информацию о запросе
        printResponseInfo(response, SC_OK, "");

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

    @Step ("POST. Получение ответа на запрос логина пользователя. Ручка api/auth/login.")
    public Response loginUser (User user) {
        System.out.println("-> Выполняется вход пользователя в систему.");

        Response response = given()
                .contentType(ContentType.JSON)
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

        Response response = given()
                .contentType(ContentType.JSON)
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

        Response response = given()
                .contentType(ContentType.JSON)
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

        Response response = given()
                .contentType(ContentType.JSON)
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

        Response response = given()
                .contentType(ContentType.JSON)
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
