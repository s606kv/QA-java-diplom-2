package api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import service.Order;
import service.User;

import java.util.List;
import java.util.Map;

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

        // печатаем информацию о запросе
        printResponseInfo(response, SC_OK, "");

        // дополнительная проверка того, что в ответе есть хотя бы один элемент с непустым id и именем
        response.then()
                .assertThat()
                .body("success", equalTo(true),
                        "data[0]._id", notNullValue(),
                        "data[0].name", notNullValue()
                );

        return response;
    }

    @Step ("Извлечение айди ингредиента по его индексу.")
    public static String getIngredientId (Response response, int index) {
        String ingredientId = response.then().extract().body().path(String.format("data[%d]._id", index));
        return ingredientId;
    }

    @Step ("POST. Получение ответа на запрос создания заказа для существующего пользователя. Ручка api/orders.")
    public Response orderCreateForUser (Order order, User user, String accessToken) {
        System.out.println(String.format("-> Формируется заказ для пользователя %s.", user.getName()));

        Response response = REQUEST
                .auth().oauth2(accessToken)
                .body(order)
                .when()
                .post(ORDER_CREATE);

        // печатаем информацию о запросе
        printResponseInfo(response, SC_OK, "");

        return response;
    }

    @Step ("GET. Получение ответа на запрос списка всех заказов для авторизованного пользователя. Ручка api/orders.")
    public Response getUserOrderList (String accessToken) {
        System.out.println("-> Формируется список заказов пользователя.");

        Response response = REQUEST
                .auth().oauth2(accessToken)
                .when()
                .get(ORDER_CREATE);

        // печатаем информацию о запросе
        printResponseInfo(response, SC_OK, "");

        return response;
    }

    @Step ("Извлечение списка заказов пользователя.")
    public void extractAllUserOrders (Response response) {
        System.out.println("-> Извлекается список заказов пользователя.");

        // приводим к красивому виду
        Map<String, Object> userOrderList = response.getBody().as(Map.class);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyUserOrderList = gson.toJson(userOrderList);

        // выводим на экран
        System.out.println(String.format("Заказы пользователя:%n%s%n", prettyUserOrderList));
    }

    @Step ("POST. Получение ответа на запрос создания заказа без создания пользователя. Ручка api/orders.")
    public Response orderCreateWithoutUser (Order order) {
        System.out.println("-> Формируется заказ.");

        Response response = REQUEST
                .body(order)
                .when()
                .post(ORDER_CREATE);

        // печатаем информацию о запросе
        printResponseInfo(response, SC_OK, "");

        return response;
    }

    @Step ("GET. Отправка запроса на получение списка всех заказов без авторизации. Ручка api/orders/all.")
    public Response getAllOrdersList () {
        System.out.println("-> Происходит отправка запроса на получение всех заказов в базе данных.");
        Response response = REQUEST
                .when()
                .get(ORDER_GET_ALL);

        // печатаем информацию о запросе
        printResponseInfo(response, SC_OK, "");

        return response;
    }

    @Step ("Извлечение нужного количества заказов из списка всех заказов базы данных.")
    public Response getRequiredListOfOrdersFromDB (Response response, int fromIndex, int toIndex) {
        System.out.println(String.format("-> Извлекаются список заказов от индекса %d (включительно) до индекса %d (не включительно) из списка всех заказов в базе данных.", fromIndex, toIndex));

        // проверка нижней границы
        if (fromIndex<0) {
            System.out.println("⚠\uFE0F Ошибка. Начальный индекс не может быть меньше нуля.");
        }

        // приводим к красивому виду
        List<Map<String, Object>> allOrders = response.getBody().path("orders");
        List<Map<String, Object>> requiredList = allOrders.subList(fromIndex, toIndex);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String allOrdersPrettyJson = gson.toJson(requiredList);

        // выводим на экран
        System.out.println(String.format("Запрошенный список заказов:%n%s%n", allOrdersPrettyJson));

        return response;
    }

}
