package api;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import service.Order;
import service.User;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static service.Utilities.*;

public class OrderAPI {

    @Step ("GET. Получение ответа на запрос списка ингредиентов и проверка ответа. Ручка api/ingredients.")
    public Response getIngredients () {
        System.out.println("-> Запрос на получение списка ингредиентов.");

        Response response = REQUEST
                .when()
                .get(GET_INGREDIENTS);

        // проверка, что в ответе статус ОК и есть хотя бы один элемент с непустым id и именем
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "data[0]._id", notNullValue(),
                        "data[0].name", notNullValue()
                );

        return response;
    }

    @Step ("POST. Получение ответа на запрос создания заказа для существующего пользователя. Ручка api/orders.")
    public Response orderCreateForUser (Order order, User user, String accessToken) {
        System.out.println(String.format("-> Формируется заказ для пользователя %s.", user.getName()));

        Response response = REQUEST
                .auth().oauth2(accessToken)
                .body(order)
                .when()
                .post(ORDER_CREATE);

        return response;
    }

    @Step ("GET. Получение ответа на запрос списка всех заказов для авторизованного пользователя. Ручка api/orders.")
    public Response getUserOrderList (String accessToken) {
        System.out.println("-> Формируется список заказов пользователя.");

        Response response = REQUEST
                .auth().oauth2(accessToken)
                .when()
                .get(ORDER_CREATE);

        return response;
    }

    @Step ("POST. Получение ответа на запрос создания заказа без создания пользователя и проверка ответа. Ручка api/orders.")
    public Response orderCreateWithoutUser (Order order) {
        System.out.println("-> Формируется заказ.");

        Response response = REQUEST
                .body(order)
                .when()
                .post(ORDER_CREATE);

        /// ⛔️ Вынести проверку из метода, т.к. может быть невалидный запрос в теле заказа
        // проверили статус ответа и тело
        response.then()
                .assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "name", notNullValue(),
                        "order.number", notNullValue()
                );

        return response;
    }

//    @Step ("GET. Получение ответа на запрос первых пяти заказов из списка всех заказов без авторизации и проверка ответа. Ручка api/orders/all.")
//    public Response retAllOrdersList () {
//        // Response response;
//        // ⛔️⛔️⛔️ написать код
//        // return response;
//    }

}
