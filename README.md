## Задание 2: API

> В работе реализована проверка api учебного сервиса
> [Stellar Burgers](https://stellarburgers.nomoreparties.site/)
> с возможностью [генерации отчета](#получение-отчета-в-allure)
> в Allure.

![Вкусняшка](https://code.s3.yandex.net/react/code/sauce-04.png)


---
### Технические данные.
* **Платформа:** Java 11.
* **Фреймворк:** JUnit 4.13.2
* **Использованные библиотеки:**
> + Google Gson (2.12.1);
> + Qameta Allure (2.29.1);
> + Rest-Assured (5.5.0);
> + DataFaker (2.4.2);
> + ProjectLombok Lombok (1.18.36)

---
## Запуск тестов:
`mvn clean test`

## Получение отчета в Allure:
`mvn allure:serve`

