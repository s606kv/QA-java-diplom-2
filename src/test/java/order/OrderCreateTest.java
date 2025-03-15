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

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static service.Utilities.*;

public class OrderCreateTest {
    // поля класса
    private Order order;
    private static OrderAPI orderAPI = new OrderAPI();
    private static Faker faker = new Faker();
    private List<String> ingredientsList;
    private UserAPI userAPI;
    private String accessToken;
    private User user;

    // тестируемые ингредиенты: fakedIngredient (сгенерированный) и realIngredient (существующий в бд)
    private String realIngredient;
    private String fakedIngredient;

    @Before
    public void setUp () {
        /// Создание пользователя и получение токена.
        // задали фейковые данные
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
    }

    @Test
    @DisplayName("Тест создания заказа авторизованным пользователем. Существующий айди ингредиента.")
    @Description("Проверяется возможность создать заказ авторизованному пользователю. Существующий айди ингредиента.")
    public void orderCreateAuthorizedUserRealIngredientTest () {
        /// Создание тела запроса.
        // создали список ингредиентов
        System.out.println("-> Формируется json со списком ингредиентов.");
        ingredientsList = new ArrayList<>();
        realIngredient = orderAPI.getIngredientId(orderAPI.getIngredients(), 0);
        System.out.println(String.format("\uD83D\uDD35 В список добавляется реальный ингредиент \"%s\".", realIngredient));
        ingredientsList.add(realIngredient);
        // создали json со списком
        order = new Order(ingredientsList);
        System.out.println("✅ json сформирован.\n");

        /// Для созданного пользователя формируется заказ.
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
    }

    @Test
    @DisplayName("Тест создания заказа авторизованным пользователем. Неверный айди ингредиента.")
    @Description("Проверяется возможность создать заказ авторизованному пользователю. Неверный айди ингредиента.")
    public void orderCreateAuthorizedUserFakedIngredientTest () {
        /// Создание тела запроса.
        // создали список ингредиентов
        System.out.println("-> Формируется json со списком ингредиентов.");
        ingredientsList = new ArrayList<>();
        fakedIngredient = faker.lorem().characters(24, 24, false, true);
        System.out.println(String.format("\uD83D\uDD35 В список добавляется неверный ингредиент \"%s\".", fakedIngredient));
        ingredientsList.add(fakedIngredient);
        // создали json со списком
        order = new Order(ingredientsList);
        System.out.println("✅ json сформирован.\n");

        /// Для созданного пользователя формируется заказ.
        Response orderCreateForUserResponse = orderAPI.orderCreateForUser(order, user, accessToken);
        orderCreateForUserResponse.then().assertThat()
                .statusCode(SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Тест создания заказа авторизованным пользователем. Ингредиенты отсутствую в запросе.")
    @Description("Проверяется возможность создать заказ авторизованному пользователю, при условии что ингредиенты отсутствую в запросе.")
    public void orderCreateAuthorizedUserNoIngredientTest () {
        /// Создание тела запроса.
        // создали список ингредиентов
        System.out.println("-> Формируется json со списком ингредиентов.");
        ingredientsList = new ArrayList<>();
        System.out.println("\uD83D\uDD35 Передан пустой список.");
        // создали json со списком
        order = new Order(ingredientsList);
        System.out.println("✅ json сформирован.\n");

        /// Для созданного пользователя формируется заказ.
        Response orderCreateForUserResponse = orderAPI.orderCreateForUser(order, user, accessToken);

        // проверили ответ
        String messageKeyValue = "Ingredient ids must be provided";
        checkNegativeResponse(orderCreateForUserResponse, SC_BAD_REQUEST, false, messageKeyValue);
    }

    @Test
    @DisplayName("Тест создания заказа без авторизации. Существующий айди ингредиента.")
    @Description("Проверяется возможность создать заказ без авторизации. Существующий айди ингредиента.")
    public void orderCreateNoAuthorizationRealIngredientTest () {
        /// Создание тела запроса.
        // создали список ингредиентов
        System.out.println("-> Формируется json со списком ингредиентов.");
        ingredientsList = new ArrayList<>();
        realIngredient = orderAPI.getIngredientId(orderAPI.getIngredients(), 0);
        System.out.println(String.format("\uD83D\uDD35 В список добавляется реальный ингредиент \"%s\".", realIngredient));
        ingredientsList.add(realIngredient);
        // создали json со списком
        order = new Order(ingredientsList);
        System.out.println("✅ json сформирован.\n");

        /// Формируется заказ без привязки к созданному пользователю.
        Response orderCreateWithoutUserResponse = orderAPI.orderCreateWithoutUser(order);
        // проверяем ответ
        orderCreateWithoutUserResponse.then().assertThat()
                .statusCode(SC_OK)
                .body("success", equalTo(true),
                        "name", notNullValue(),
                        "order.number", notNullValue()
                );
    }

    @Test
    @DisplayName("Тест создания заказа без авторизации. Неверный айди ингредиента.")
    @Description("Проверяется возможность создать заказ без авторизации. Неверный айди ингредиента.")
    public void orderCreateNoAuthorizationFakedIngredientTest () {
        /// Создание тела запроса.
        // создали список ингредиентов
        System.out.println("-> Формируется json со списком ингредиентов.");
        ingredientsList = new ArrayList<>();
        fakedIngredient = faker.lorem().characters(24, 24, false, true);
        System.out.println(String.format("\uD83D\uDD35 В список добавляется неверный ингредиент \"%s\".", fakedIngredient));
        ingredientsList.add(fakedIngredient);
        // создали json со списком
        order = new Order(ingredientsList);
        System.out.println("✅ json сформирован.\n");

        /// Формируется заказ.
        Response orderCreateWithoutUserResponse = orderAPI.orderCreateWithoutUser(order);
        // проверка ответа
        orderCreateWithoutUserResponse.then().assertThat()
                .statusCode(SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("Тест создания заказа без авторизации. Ингредиенты отсутствую в запросе.")
    @Description("Проверяется возможность создать заказ без авторизации, при условии что ингредиенты отсутствую в запросе.")
    public void orderCreateNoAuthorizationNoIngredientTest () {
        /// Создание тела запроса.
        // создали список ингредиентов
        System.out.println("-> Формируется json со списком ингредиентов.");
        ingredientsList = new ArrayList<>();
        System.out.println("\uD83D\uDD35 Передан пустой список.");
        // создали json со списком
        order = new Order(ingredientsList);
        System.out.println("✅ json сформирован.\n");

        /// Формируется заказ.
        Response orderCreateWithoutUserResponse = orderAPI.orderCreateWithoutUser(order);
        // проверили ответ
        String messageKeyValue = "Ingredient ids must be provided";
        checkNegativeResponse(orderCreateWithoutUserResponse, SC_BAD_REQUEST, false, messageKeyValue);
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
