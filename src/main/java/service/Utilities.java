package service;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class Utilities {
    public static final String BASE_URI = "https://stellarburgers.nomoreparties.site/";
    public static final RequestSpecification REQUEST = given().baseUri(BASE_URI).contentType(ContentType.JSON);

    public static final String USER_CREATE = "api/auth/register";
    public static final String USER_DELETE = "api/auth/user";
    public static final String USER_LOGIN = "api/auth/login";
    public static final String USER_LOGOUT = "api/auth/logout";
    public static final String USER_DATA = "api/auth/user";
    public static final String ORDER_CREATE = "api/orders";
    public static final String ORDER_GET_ALL = "api/orders/all";
    public static final String GET_INGREDIENTS = "api/ingredients";

    // сервисный метод печати информации в зависимости от статус-кода
    public static void printResponseInfo (Response response, int expectedStatusCode, String otherInfo) {
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
                ? String.format("\uD83D\uDFE2 Статус-код: %d.%nУспешный запрос.%n%s", actualStatusCode, otherInfo)
                : String.format("\uD83D\uDFE1 ВНИМАНИЕ. Статус: %d.%nТело ответа: %s.%nЗапрос некорректный.%n", actualStatusCode, responseBody);
        System.out.println(info);
    }

    // сервисный метод извлечения данных пользователя из тела ответа
    public static String extractUserData (Response response) {
        // извлекаем емэйл
        String jsonEmail = response.then().extract().body().path("user.email");
        // извлекаем имя
        String jsonName = response.then().extract().body().path("user.name");
        // создаём информацию с данными пользователя
        String extractedInfo = String.format("Данные пользователя:%nemail: %s%nимя: %s%n", jsonEmail, jsonName);

        return extractedInfo;
    }

    // сервисный метод получения айди ингредиента по его индексу
    public static String getIngredientId (Response response, int index) {
        String ingredientId = response.then().extract().body().path(String.format("data[%d]._id", index));
        return ingredientId;
    }

    // сервисный метод проверки негативного ответа
    public static void checkNegativeResponse(Response response, int statusCode, boolean successKeyValue, String messageKeyValue) {
        response.then()
                .assertThat()
                .statusCode(statusCode)
                .body("success", equalTo(successKeyValue),
                        "message", equalTo(messageKeyValue));
    }

    // сервисный метод проверки позитивного ответа с юзером
    public static void checkUserPositiveResponse (Response response, User user, int statusCode, boolean successKeyValue) {
        response.then()
                .assertThat()
                .statusCode(statusCode)
                .body("success", equalTo(successKeyValue),
                        "user.email", equalTo(user.getEmail()),
                        "user.name", equalTo(user.getName()),
                        "accessToken", notNullValue(),
                        "refreshToken", notNullValue()
                );
    }
}
