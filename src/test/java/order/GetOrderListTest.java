package order;

import api.OrderAPI;
import api.UserAPI;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import net.datafaker.Faker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import service.Order;
import service.User;

import java.util.ArrayList;
import java.util.List;

import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static service.Utilities.checkUserPositiveResponse;

public class GetOrderListTest {
    // поля класса
    private Order order;
    private OrderAPI orderAPI;
    private UserAPI userAPI;
    private Faker faker;
    private List<String> ingredientsList;

    private String accessToken;
    private User user;
    private String ingredient;

    @Before
    public void setUp () {
        /// Создание пользователя и получение токена.
        // задали фейковые данные
        faker = new Faker();
        String userEmail = faker.internet().emailAddress();
        String userPassword = faker.internet().password();
        String userName = faker.name().username();
        // json с пользователем
        user = new User(userEmail, userPassword, userName);
        // создали пользователя в системе
        userAPI = new UserAPI();
        Response userCreatingResponse = userAPI.userCreating(user);
        // проверили статус и тело ответа
        checkUserPositiveResponse(userCreatingResponse, user, SC_OK, true);
        // получили accessToken
        accessToken = userAPI.getAccessToken(userCreatingResponse);

        /// Создание тела запроса для формирования заказа.
        // создали список ингредиентов
        orderAPI = new OrderAPI();
        System.out.println("-> Формируется json со списком ингредиентов.");
        ingredientsList = new ArrayList<>();
        ingredient = orderAPI.getIngredientId(orderAPI.getIngredients(), 0);
        System.out.println(String.format("\uD83D\uDD35 В список добавляется ингредиент \"%s\".", ingredient));
        ingredientsList.add(ingredient);
        // создали json со списком
        order = new Order(ingredientsList);
        System.out.println("✅ json сформирован.\n");
    }

    @Test
    @DisplayName("Тест получения списка заказов авторизованного пользователя.")
    @Description("Проверяется возможность создать заказ авторизованному пользователю. Выводится список заказов этого пользователя.")
    public void getOrderListOfAuthorizedUserTest () {
        /// Формируется заказ с привязкой к созданному пользователю.
        Response orderCreateForUserResponse = orderAPI.orderCreateForUser(order, user, accessToken);
        // проверяем ответ
        orderCreateForUserResponse.then().assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "name", notNullValue(),
                        "order.ingredients[0]._id", equalTo(order.getIngredients().get(0)),
                        "order.owner.name", equalTo(user.getName()),
                        "order.owner.email", equalTo(user.getEmail())
                );

        /// Запрос получения списка заказов пользователя.
        Response getUserOrderListResponse = orderAPI.getUserOrderList(accessToken);
        // проверяем ответ
        getUserOrderListResponse.then().assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "orders[0]._id", notNullValue(),
                        "orders[0].ingredients", notNullValue()
                );

        /// Формируется читаемый json с заказами пользователя.
        orderAPI.extractAllUserOrders(getUserOrderListResponse);
    }

    @Test
    @DisplayName("Тест создания заказа без авторизации.")
    @Description("Проверяется возможность создать заказ без авторизации. Выводится выборка из списка всех заказов БД.")
    public void getOrderListWithoutAuthorizationTest () {
        /// Формируется заказ без авторизации.
        Response orderCreateWithoutUserResponse = orderAPI.orderCreateWithoutUser(order);
        // проверяем ответ
        orderCreateWithoutUserResponse.then().assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "name", notNullValue(),
                        "order.number", notNullValue()
                );

        /// Запрос на получение списка ВСЕХ заказов из БД.
        Response getAllOrdersListResponse = orderAPI.getAllOrdersList();
        // проверяем ответ, что он содержит айди заказа и ингредиенты
        getAllOrdersListResponse.then().assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "orders[0]._id", notNullValue(),
                        "orders[0].ingredients", notNullValue()
                );

        /// Формируется читаемый json с ограниченной ВЫБОРКОЙ заказов.
        // для вывода большего числа заказов достаточно поменять второй индекс в большую сторону
        Response allOrdersResponse = orderAPI.getRequiredListOfOrdersFromDB(getAllOrdersListResponse, 0, 2);
    }

    @After /// Удаляем пользователя
    public void postconditions () {
        Response deleteUserResponse = userAPI.deleteUser(accessToken);
        // проверка статуса и тела ответа
        deleteUserResponse.then().assertThat()
                .statusCode(SC_ACCEPTED)
                .body(
                        "success", equalTo(true),
                        "message", equalTo("User successfully removed")
                );
    }
}
