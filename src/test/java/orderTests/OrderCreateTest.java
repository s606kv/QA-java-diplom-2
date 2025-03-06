package orderTests;

import api.OrderAPI;
import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import net.datafaker.Faker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import service.Order;
import service.User;

import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static service.Utilities.checkNegativeResponse;
import static service.Utilities.checkUserPositiveResponse;

@RunWith(Parameterized.class)
public class OrderCreateTest {
    // сгенерированы данные пользователя
    private static Faker faker = new Faker();
    private String userEmail = faker.internet().emailAddress();
    private String userPassword = faker.internet().password();
    private String userName = faker.name().username();

    // поля класса
    private UserAPI userAPI;
    private Order order;
    private static OrderAPI orderAPI;
    private Response response;
    private String accessToken;

    // ингредиенты для тестов
    private static String realIngredient = orderAPI.getIngredients().then().extract().body().path("data[0]._id");
    private static String fakedIngredient = faker.lorem().characters(24, 24, false, true);

    // поле для параметров
    private final String testIngredientId;
    private final int statusCode;
    private final String testName;
    // конструктор
    public OrderCreateTest (String testIngredientId, int statusCode, String testName) {
        this.testIngredientId=testIngredientId;
        this.statusCode=statusCode;
        this.testName=testName;
    }

    @Parameterized.Parameters (name="{1}")
    public static Object[][] data () {
        return new Object[][]{
                {realIngredient, SC_OK, "Существующий айди ингредиента" },
                {fakedIngredient, SC_INTERNAL_SERVER_ERROR, "Неверный айди ингредиента"},
                {null, SC_BAD_REQUEST, "Ингредиенты отсутствую в запросе"}
        };
    }

    @Test
    @DisplayName("Параметризованный тест создания заказа авторизованным пользователем.")
    @Description("Проверяется возможность создать заказ авторизованному пользователю с существующим ингредиентом и несуществующим.")
    public void orderCreateAuthorizedUser () {

        ///  Создание пользователя.
        // json с пользователем
        User user = new User(userEmail, userPassword, userName);
        // создали пользователя
        response = userAPI.userCreating(user);
        // проверили статус и тело
        checkUserPositiveResponse(response, user, SC_OK, true);
        // получили accessToken
        accessToken = userAPI.getAccessToken(response);

        /// Создание заказа.
        // создали список ингредиентов
        List<String> ingredientsList = new ArrayList<>();
        ingredientsList.add(testIngredientId);
        // создали json со списком
        order = new Order(ingredientsList);
        // отправили запрос на создание заказа для созданного пользователя
        Response orderCreateForUserResponse = orderAPI.orderCreateForUser(order, user, accessToken);

        // проверка ответа, если всё ok и получение списка заказов
        if (orderCreateForUserResponse.getStatusCode() == SC_OK) {
            // проверяем ответ
            orderCreateForUserResponse.then().assertThat().statusCode(statusCode)
                    .body("success", equalTo(true),
                            "name", notNullValue(),
                            "order.ingredients[0]._id", equalTo(order.getIngredients().get(0)),
                            "order.owner.name", equalTo(user.getName()),
                            "order.owner.email", equalTo(user.getEmail())
                    );
            // получаем список заказов
            Response getUserOrderListResponse = orderAPI.getUserOrderList(accessToken);
            // проверяем ответ
            getUserOrderListResponse.then().assertThat().statusCode(SC_OK)
                    .body("success", equalTo(true),
                            "orders[0]._id", notNullValue(),
                            "orders[0].ingredients", notNullValue()
                    );
        }
        // проверка статуса и тела, если в запросе неверный айди ингредиента
        else if (orderCreateForUserResponse.getStatusCode() == SC_INTERNAL_SERVER_ERROR) {
            System.out.println("Ошибка сервера.");
            orderCreateForUserResponse.then()
                    .assertThat()
                    .statusCode(statusCode)
                    .body("head.title", equalTo("Error"));
            return;
        }
        // проверка статуса и тела, если в запросе пусто
        else if (orderCreateForUserResponse.getStatusCode() == SC_BAD_REQUEST) {
            String messageKeyValue = "Ingredient ids must be provided";
            checkNegativeResponse(orderCreateForUserResponse, statusCode, false, messageKeyValue);
            // проверили, что в список ничего не добавилось
            Response getUserOrderListResponse = orderAPI.getUserOrderList(accessToken);
            getUserOrderListResponse.then().assertThat().statusCode(SC_OK)
                    .body("success", equalTo(true),
                            "orders".isEmpty()
                    );
        } else return;


        // удалили созданного пользователя
        userAPI.deleteUser(accessToken);
    }



}
