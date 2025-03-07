package orderTests;

import api.OrderAPI;
import api.UserAPI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.response.Response;
import net.datafaker.Faker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import service.Order;
import service.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static service.Utilities.*;

@RunWith(Parameterized.class)
public class OrderCreateParameterizedTest {
    // поля класса
    private Order order;
    private static OrderAPI orderAPI = new OrderAPI();
    private Gson gson = new Gson();
    private static Faker faker = new Faker();
    private List<String> ingredientsList;

    // тестируемые ингредиенты: fakedIngredient (сгенерированный) и realIngredient (существующий в бд)
    private static String fakedIngredient = faker.lorem()
            .characters(24, 24, false, true);
    private static Response getIngredientsResponse = orderAPI.getIngredients();
    private static String realIngredient = orderAPI.getIngredientId(getIngredientsResponse, 0);

    // поля для параметризации
    private final String testIngredientId;
    private final int statusCode;
    private final String testName;
    // конструктор
    public OrderCreateParameterizedTest(String testIngredientId, int statusCode, String testName) {
        this.testIngredientId=testIngredientId;
        this.statusCode=statusCode;
        this.testName=testName;
    }

    // параметры для тестов
    @Parameterized.Parameters (name="{2}")
    public static Object[][] data () {
        return new Object[][] {
                {realIngredient, SC_OK, "Существующий айди ингредиента" },
                {fakedIngredient, SC_INTERNAL_SERVER_ERROR, "Неверный айди ингредиента"},
                {null, SC_BAD_REQUEST, "Ингредиенты отсутствую в запросе"},
        };
    }

    @Before
    public void preconditions () {
        /// Создание тела запроса.
        // создали список ингредиентов
        System.out.println("-> Формируется json со списком ингредиентов.");
        ingredientsList = new ArrayList<>();
        if (testIngredientId!=null) {
            ingredientsList.add(testIngredientId);
            System.out.println(String.format("\uD83D\uDD35 В список добавляется ингредиент \"%s\".", testIngredientId));
        } else {
            System.out.println("\uD83D\uDD35 Передан пустой список ингредиентов.");
        }
        // создали json со списком
        order = new Order(ingredientsList);
        System.out.println("✅ Запрос сформирован.\n");
    }

    @Test
    @DisplayName("Параметризованный тест создания заказа авторизованным пользователем.")
    @Description("Проверяется возможность создать заказ авторизованному пользователю с существующим ингредиентом и несуществующим.")
    public void orderCreateAuthorizedUserTest () {
        /// Создание пользователя и получение токена.
        // задали фейковые данные
        String userEmail = faker.internet().emailAddress();
        String userPassword = faker.internet().password();
        String userName = faker.name().username();
        // json с пользователем
        User user = new User(userEmail, userPassword, userName);
        // создали пользователя в системе
        UserAPI userAPI = new UserAPI();
        Response userCreatingResponse = userAPI.userCreating(user);
        // проверили статус и тело ответа
        checkUserPositiveResponse(userCreatingResponse, user, SC_OK, true);
        // получили accessToken
        String accessToken = userAPI.getAccessToken(userCreatingResponse);

        /// Для созданного пользователя формируется заказ.
        Response orderCreateForUserResponse = orderAPI.orderCreateForUser(order, user, accessToken);

        /// Блок проверок в зависимости от полученного статус-кода
        // проверка ответа, если всё ok, а также получение списка заказов пользователя
        if (orderCreateForUserResponse.getStatusCode() == SC_OK) {
            System.out.println("\uD83D\uDFE2 Заказ для пользователя успешно создан.\n");
            // проверяем ответ
            orderCreateForUserResponse.then().assertThat().statusCode(statusCode)
                    .body("success", equalTo(true),
                            "name", notNullValue(),
                            "order.ingredients[0]._id", equalTo(order.getIngredients().get(0)),
                            "order.owner.name", equalTo(user.getName()),
                            "order.owner.email", equalTo(user.getEmail())
                    );
            // запрос на получение списка заказов
            Response getUserOrderListResponse = orderAPI.getUserOrderList(accessToken);
            // проверяем ответ
            getUserOrderListResponse.then().assertThat().statusCode(SC_OK)
                    .body("success", equalTo(true),
                            "orders[0]._id", notNullValue(),
                            "orders[0].ingredients", notNullValue()
                    );
            // формируется читаемый json с заказами пользователя
            orderAPI.extractAllUserOrders(getUserOrderListResponse);
        }
        // проверка статуса и тела, если в запросе несуществующий ингредиент
        else if (orderCreateForUserResponse.getStatusCode() == SC_INTERNAL_SERVER_ERROR) {
            System.out.println("\uD83D\uDD34 Ошибка сервера.\n");
            orderCreateForUserResponse.then()
                    .assertThat()
                    .statusCode(statusCode);
        }
        // проверка статуса и тела, если в запросе пусто
        else if (orderCreateForUserResponse.getStatusCode() == SC_BAD_REQUEST) {
            String messageKeyValue = "Ingredient ids must be provided";
            // проверили ответ
            System.out.println("Проверяется статус и тело ответа.");
            checkNegativeResponse(orderCreateForUserResponse, statusCode, false, messageKeyValue);
        }
        // если иной код, то показывается сообщение
        else {
            System.out.println("⛔\uFE0F Неизвестный статус-код.");
        }

        /// Удаление созданного пользователя.
        userAPI.deleteUser(accessToken);
    }



}
