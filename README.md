# rnd_enterprise — Broken Links Checker

## Что это

Автоматизированный инструмент для поиска неработающих (broken) ссылок и изображений в контентной базе целевого сайта.

Проект работает полностью через API (без браузера): получает контент из CMS, парсит HTML, проверяет каждую ссылку и изображение на доступность, формирует отчёт о сломанных URL и отправляет его обратно в API.

---

## Как это работает

### Полный цикл за один тест

```
API (getAuthToken)
    → получить token
    → API (exportContentVerify) — получить пачку контент-записей
    → распарсить HTML из поля verifyData (Jsoup)
    → извлечь все href и src
    → дедуплицировать
    → нормализовать URL
    → проверить HTTP-статус каждого URL (HttpURLConnection)
    → сломанные записать в broken_links.ndjson
    → повторить цикл (следующий offset)
    → API (importContentVerify) — отправить финальный отчёт
```

### Итерационная логика

Тест работает в цикле: каждая итерация запрашивает пачку записей (`limit` + `offset`), проверяет их и сдвигает `offset` на следующую пачку. Количество итераций и размер пачки управляются через конфигурационный файл (и через Jenkins-параметры при запуске в CI).

---

## Стек

| Компонент | Технология |
|---|---|
| Язык | Java 21 |
| Тест-фреймворк | JUnit 5 |
| API-тестирование | REST Assured |
| HTML-парсинг | Jsoup |
| JSON-сериализация | Jackson |
| Отчёты | Allure (JUnit 5 listener) |
| Boilerplate | Lombok |
| Сборка | Maven |
| CI | Jenkins |

---

## Структура проекта

```
src/test/java/com/linkvalidator/
├── core/
│   ├── ConfigurationReader.java   — загрузка конфигурации (CI или локально)
│   └── FlowMethods.java           — сценарный API-слой (@Step для Allure)
├── pojo/
│   ├── LinkCheckItem.java         — одна проверяемая ссылка (id, type, link)
│   ├── ExportItem.java            — payload-запись для отправки в API (id + verifyData[])
│   └── VerifyDataItem.java        — элемент verifyData (type + value)
├── tests/
│   ├── BaseTest.java              — @BeforeAll: настройка baseURI + Allure filter
│   ├── TestBrokenLinks.java       — основной тест (итерационный цикл)
│   └── DraftMethods.java          — черновики / экспериментальный код (будет удалён)
└── utilities/
    ├── Utils.java                 — HTTP-проверка, Jsoup-парсинг, NDJSON-операции
    ├── NdJsonWriter.java          — запись/очистка broken_links.ndjson
    └── BlockedHostsProvider.java  — фильтр исключений по списку хостов
```

---

## Конфигурация

`configuration.properties` **не хранится в репозитории**. Файл конфигурации поставляется через Jenkins Managed Files при каждом запуске пайплайна.

Для локального запуска необходимо создать `configuration.properties` в корне проекта самостоятельно.

`ConfigurationReader` автоматически определяет источник:
- в CI — читает файл по пути из системного свойства `-DConfiguration.properties`
- локально — читает `Configuration.properties` из корня проекта

### Параметры итерации

Три параметра управляют объёмом проверки за один прогон. В Jenkins они перезаписываются через параметры пайплайна (`LIMITED_ID`, `OFFSET`, `ITERATIONS_LIMIT`) — без изменения файла конфигурации.

| Параметр | Назначение |
|---|---|
| `idNumberInResponseLimit` | сколько записей запрашивать за раз |
| `offsetParam` | стартовое смещение |
| `iterationsLimit` | максимум итераций |

---

## Список исключений (blocked hosts)

Файл со списком хостов-исключений **не хранится в репозитории** — он содержит чувствительную информацию о целевой инфраструктуре. Файл поставляется через Jenkins Managed Files аналогично конфигурации.

`BlockedHostsProvider` читает список при старте и пропускает совпадающие URL без HTTP-запроса.

---

## Логика валидации URL

Метод `normalizeURL()` фильтрует ссылки перед проверкой:

| Входное значение | Результат |
|---|---|
| `null` | пропустить |
| пустая строка | `__INVALID__` (сломана) |
| `#anchor`, `javascript:`, `mailto:` | пропустить |
| `/relative/path` | преобразовать в абсолютный URL |
| `https://...` / `http://...` | проверить как есть |
| всё остальное | `__INVALID__` (сломана) |

Ссылки с HTTP-статусом ≥ 400 записываются в NDJSON как сломанные.

---

## Отчётность

- **Allure Report** — генерируется автоматически после каждого прогона (локально: `mvn allure:serve`; в Jenkins: плагин Allure).
- **broken_links.ndjson** — построчный JSON-файл со всеми сломанными ссылками. После проверки отправляется в API через `importContentVerify`.

---

## CI/CD

`Jenkinsfile` реализует пайплайн:

1. **Config file** — подкладывает `configuration.properties` и `blocked-hosts.txt` из Jenkins Managed Files, перезаписывает параметры итерации.
2. **Run tests** — `mvn clean test`.
3. **Post: always** — публикует Allure Report.
4. **Post: failure / unstable / aborted** — отправляет уведомление в Telegram через Bot API (токен и chat_id хранятся в Jenkins Credentials, не в коде).

---

## Быстрый старт (локально)

```bash
# Запустить тест
mvn clean test

# Открыть Allure Report
mvn allure:serve
```

Перед запуском создать `Configuration.properties` в корне проекта с необходимыми параметрами.
