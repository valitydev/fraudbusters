# Fraudbusters k6 tests

Набор использует реальные публичные HTTP-фасады:

- `fraudbusters-api`: `/inspect-payment` и `/payments`;
- `fraudbusters-management`: создание правил/references и чтение control plane.

Тем самым запрос k6 проходит через Swagger API, Thrift, Fraudbusters runtime,
Kafka configuration flow, внешние rule dependencies и хранилища, поднятые в
тестовом окружении.

Архитектурно это позволяет измерять два уровня отдельно:

- latency внешнего контракта `fraudbusters-api → Thrift → fraudbusters`;
- control/data plane всей системы вместе с management, Kafka и хранилищами.

## Сценарии

| Файл | Назначение |
|---|---|
| `smoke.js` | Сквозной интеграционный тест: создать правило и reference, дождаться активации, проверить `fatal/high`, загрузить payment event |
| `inspection-load.js` | Нагрузка только на критический онлайн-путь `/inspect-payment` |
| `mixed-load.js` | Смешанная нагрузка: inspection, event ingestion и management reads |

## Предварительные условия

Поднять полный контур, например из соседнего `fraudbusters-compose`, включая:

- `fraudbusters-api` на host-порту `9999`;
- `fraudbusters-management` на host-порту `8085`;
- Fraudbusters, Kafka, ClickHouse, PostgreSQL, `wb-list` и остальные зависимости.

В текущем `fraudbusters-compose/docker-compose.yml` определения основных
Fraudbusters-сервисов закомментированы. Перед запуском тестов нужен рабочий
compose/profile или развернутое тестовое окружение.

Если включена авторизация, передать JWT:

```bash
export FB_API_TOKEN=...
export FB_MANAGEMENT_TOKEN=...
```

## Запуск

k6 не установлен локально, поэтому `run.sh` запускает официальный Docker image:

```bash
./tests/k6/run.sh smoke.js
./tests/k6/run.sh inspection-load.js
./tests/k6/run.sh mixed-load.js
```

При локально установленном k6:

```bash
k6 run tests/k6/smoke.js
```

## Настройка нагрузки

```bash
FB_RATE=100 \
FB_DURATION=10m \
FB_PREALLOCATED_VUS=100 \
FB_MAX_VUS=500 \
./tests/k6/run.sh inspection-load.js
```

Для mixed workload:

```bash
FB_INSPECT_RATE=100 \
FB_INGEST_RATE=20 \
FB_MANAGEMENT_VUS=5 \
FB_DURATION=15m \
./tests/k6/run.sh mixed-load.js
```

Основные переменные:

| Переменная | Default | Назначение |
|---|---:|---|
| `FB_API_URL` | `http://host.docker.internal:9999` | fraudbusters-api |
| `FB_MANAGEMENT_URL` | `http://host.docker.internal:8085/fb-management/v1` | fraudbusters-management |
| `FB_ACTIVATION_WAIT_SECONDS` | `5` | Ожидание eventual consistency после создания reference |
| `FB_PARTY_ID`, `FB_SHOP_ID` | `k6-load-*` | Предварительно настроенный merchant для load-сценариев |
| `FB_CLEANUP` | `false` | Удалять созданные smoke-test template/reference |

## Подготовка данных для load-тестов

`inspection-load.js` и `mixed-load.js` намеренно не изменяют конфигурацию правил
во время нагрузки. До теста нужно один раз создать template/reference для
`FB_PARTY_ID` и `FB_SHOP_ID`, либо использовать merchant с уже активными
правилами.

Для сравнимых прогонов фиксировать:

- набор и сложность правил;
- объём истории ClickHouse;
- ответы/latency `wb-list`, Columbus и Trusted Tokens;
- число partitions и consumer concurrency Kafka;
- CPU/memory limits всех сервисов.

Не смешивать тест максимальной пропускной способности inspection с массовым
созданием правил: это разные профили нагрузки и разные bottlenecks.

## План покрытия всей системы

Текущий каркас покрывает базовый сквозной путь и готов для расширения. Полный
system-test набор рекомендуется разделить на независимые профили:

| Профиль | Подготовка | Проверяемая цепочка | Главная метрика |
|---|---|---|---|
| Simple rule | Template + reference | management → Kafka → rule pools → API → inspector | inspection latency |
| Aggregates | История платежей + aggregate rule | API → Thrift → Fraudo → ClickHouse | query latency, ClickHouse load |
| Black/white list | Записи через management | management → Kafka → wb-list → inspector | wb-list latency/errors |
| Grey list | RowInfo count/TTL + история | wb-list + ClickHouse → inspector | combined dependency latency |
| Geo IP | Rule `countryBy("ip")` | inspector → Columbus | cache hit/miss latency |
| Trusted token | Trusted-token conditions | inspector → trusted-tokens | dependency latency/errors |
| Configuration churn | Создание/удаление rules и references | management → Kafka → runtime/read-model | activation lag |
| Historical/emulation | Prepared datasets | management → Fraudbusters → ClickHouse | long-query latency |
| Failure modes | Controlled dependency delay/errors | circuit breakers, timeouts, recovery | error rate, recovery time |

Для каждого dependency-профиля стоит иметь два запуска:

1. **Integration smoke** с детерминированными данными и строгими checks.
2. **Isolated load profile**, где setup выполнен заранее и во время измерения
   меняется только исследуемая нагрузка.

Существующие сценарии в соседнем
`fraudbusters-compose/e2e-test/test/` уже содержат полезные fixtures для
aggregates, Columbus, Trusted Tokens и списков. Их следует переносить в k6
по одному, сохраняя те же ожидаемые risk scores.

## Ограничения интерпретации результатов

Нагрузка через `fraudbusters-api` измеряет пользовательский REST-путь целиком,
включая JSON conversion и Thrift proxy. Для локализации bottleneck нужны
одновременные метрики:

- `fraudbusters-api`: HTTP latency, errors, connection pools;
- `fraudbusters`: inspection/function timers, JVM, rule pool sizes;
- внешние сервисы: latency/error rate по `wb-list`, Columbus, Trusted Tokens;
- ClickHouse: query latency, running queries, CPU/IO;
- Kafka: producer latency, consumer lag и activation lag;
- PostgreSQL management: query latency и pool saturation.
