package api;

import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import service.User;

import static io.restassured.RestAssured.authentication;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static service.ServiceLinks.*;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

public class UserAPI {

    @Step ("Получение ответа на POST запрос создания пользователя. Ручка /api/auth/register")
    public Response postForUserCreating (User user) {
        System.out.println("-> Создаётся новый пользователь...");

        Response response = given()
                .body(user)
                .when()
                .post(CREATE_USER);

        // вывод сообщения в зависимости от исхода запроса
        String responseBody = response.then().extract().body().asString();
        int statusCode = response.getStatusCode();
        String info = (statusCode == SC_CREATED)
                ? String.format("Статус-код: %d. Создан новый пользователь.%n", statusCode)
                : String.format("⚠\uFE0F ВНИМАНИЕ. Тело ответа: %s.%nПользователь не создан. Проверьте тело запроса.%n", responseBody);
        System.out.println(info);

        return response;
    }

    @Step ("Получение accessToken после создания пользователя")
    public String accessToken (Response response) {
        String untrimmedAccessToken = response.then().extract().body().path("accessToken").toString();
        String cleanAccessToken = untrimmedAccessToken.substring(8, untrimmedAccessToken.length()+1);

        System.out.println("Получен accessToken:%n" + cleanAccessToken);

        return cleanAccessToken;
    }

    @Step ("Получение ответа на POST запрос удаления пользователя. Ручка /api/auth/user")
    public void deleteUser (User user, String accessToken) {
        System.out.println("-> Удаляется пользователь...");

        Response response = given()
                .header("Authorization", accessToken)
                .body(user)
                .when()
                .delete(DELETE_USER);

        // вывод сообщения в зависимости от исхода запроса
        String responseBody = response.then().extract().body().asString();
        int statusCode = response.getStatusCode();
        String info = (statusCode == SC_OK)
                ? String.format("Статус-код: %d. Пользователь удалён.%n", statusCode)
                : String.format("⚠\uFE0F ВНИМАНИЕ. Тело ответа: %s.%nПользователь не удалён. Проверьте тело запроса.%n", responseBody);
        System.out.println(info);

        // проверка статуса и тела ответа
        response.then().assertThat()
                .statusCode(SC_ACCEPTED)
                .and()
                .body("message", equalTo("User successfully removed"));
    }







}
