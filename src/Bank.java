import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// Основной класс банка
class Bank {
    // ConcurrentHashMap для безопасного доступа к клиентам из разных потоков.
    // Ключ - ID клиента, значение - объект клиента.
    private final ConcurrentHashMap<Integer, Client> clients = new ConcurrentHashMap<>();
    // Список наблюдателей, которые будут получать уведомления о событиях.
    private final List<Observer> observers = new ArrayList<>();
    // BlockingQueue для безопасного добавления и извлечения транзакций из очереди.
    // LinkedBlockingQueue - реализация очереди с ограниченной пропускной способностью (если не указан размер).
    private final BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>();
    // ScheduledExecutorService для выполнения задач по расписанию (например, обновление курсов валют).
    // newSingleThreadScheduledExecutor() создает планировщик с одним потоком.
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    // ExecutorService для пула потоков-кассиров.
    // newFixedThreadPool() создает пул с фиксированным количеством потоков.
    private final ExecutorService cashierPool;
    // ConcurrentHashMap для хранения курсов валют.
    // Ключ - код валюты (например, "USD"), значение - курс относительно базовой валюты (предполагается, что USD - базовая).
    private final Map<String, Double> exchangeRates = new ConcurrentHashMap<>();
    // AtomicBoolean для сигнализации потокам-кассирам о необходимости остановки.
    // AtomicBoolean позволяет атомарно изменять значение, что безопасно в многопоточной среде.
    private final AtomicBoolean isActive = new AtomicBoolean(true); // для остановки кассиров

    public Bank(int numCashiers) {
        // Инициализация валютных курсов
        exchangeRates.put("USD", 1.0);// USD как базовая валюта
        exchangeRates.put("EUR", 0.85);
        exchangeRates.put("RUB", 75.0);

        // Запуск автоматического обновления курсов
        // scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit)
        // Запускает задачу updateExchangeRates() через 0 секунд, повторяя каждые 1 секунду.
        scheduler.scheduleAtFixedRate(this::updateExchangeRates, 0, 1, TimeUnit.SECONDS);

        // Создание кассиров
        cashierPool = Executors.newFixedThreadPool(numCashiers);
        // Запуск потоков-кассиров
        for (int i = 0; i < numCashiers; i++) {
            // submit(Runnable task) запускает задачу в пуле потоков и возвращает Future.
            cashierPool.submit(new Cashier(this, isActive));
        }
    }

    // Регистрация клиента
    public void addClient(Client client) {
        clients.put(client.getId(), client);
    }

    // Регистрация наблюдателя
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    // Уведомление всех наблюдателей
    public void notifyObservers(String message) {
        for (Observer o : observers) {
            o.update(message);// Вызывает метод update() у каждого наблюдателя
        }
    }

    // Методы для работы с транзакциями
    public void enqueueTransaction(Transaction tx) {
        // Добавляет транзакцию в очередь. Если очередь полная, метод будет блокироваться (в LinkedBlockingQueue это не произойдет, пока есть место).
        transactionQueue.add(tx);
    }

    public Transaction getTransaction() throws InterruptedException {
        // Извлекает транзакцию из очереди.
        // poll(long timeout, TimeUnit unit) - возвращает транзакцию, если она доступна, или null, если время ожидания истекло.
        // Это важно для корректного завершения работы: если очередь пуста, а банк уже не активен, поток-кассир не будет бесконечно ждать.
        return transactionQueue.poll(1, TimeUnit.SECONDS); // тайм-аут для проверки завершения
    }

    public boolean isTransactionQueueEmpty() {
        return transactionQueue.isEmpty();
    }

    // Работа с балансом
    public void deposit(int clientId, double amount) {
        Client client = clients.get(clientId);
        if (client != null) {
            // Синхронизация на объекте клиента для обеспечения атомарности операции deposit.
            // Только один поток может изменять баланс клиента в данный момент.
            synchronized (client) {
                client.setBalance(client.getBalance() + amount);
            }
        }
    }

    public void withdraw(int clientId, double amount) {
        Client client = clients.get(clientId);
        if (client != null) {
            synchronized (client) {
                if (client.getBalance() >= amount) {
                    client.setBalance(client.getBalance() - amount);
                } else {
                    notifyObservers("Ошибка: недостаточно средств у клиента #" + clientId);
                }
            }
        }
    }

    // Обмен валют
    public void exchangeCurrency(int clientId, String fromCurrency, String toCurrency, double amount) {
        Client client = clients.get(clientId);
        if (client != null) {
            synchronized (client) {
                // Проверка, совпадает ли валюта клиента с валютой, которую он хочет обменять.
                if (!client.getCurrency().equals(fromCurrency)) {
                    notifyObservers("Ошибка: валюты клиента не совпадают с fromCurrency");
                    return;
                }
                if (client.getBalance() < amount) {
                    notifyObservers("Ошибка: недостаточно средств для обмена у клиента #" + clientId);
                    return;
                }
                Double rateFrom = exchangeRates.get(fromCurrency);
                Double rateTo = exchangeRates.get(toCurrency);
                if (rateFrom == null || rateTo == null) {
                    notifyObservers("Ошибка: неизвестные валюты");
                    return;
                }
                // Расчет конвертированной суммы.
                // amount / rateFrom - конвертируем amount из fromCurrency в базовую валюту (USD).
                // (amount / rateFrom) * rateTo - конвертируем из базовой валюты в toCurrency.
                double amountInBase = amount / rateFrom;
                double convertedAmount = amountInBase * rateTo;
                // Обновление баланса
                client.setBalance(client.getBalance() - amount);// Списываем исходную сумму
                client.setBalance(client.getBalance() + convertedAmount);// Добавляем сконвертированную сумму
                client.setCurrency(toCurrency);// Меняем валюту клиента
            }
        }
    }

    // Перевод между клиентами
    public void transferFunds(int senderId, int receiverId, double amount) {
        Client sender = clients.get(senderId);
        Client receiver = clients.get(receiverId);
        if (sender == null || receiver == null) return;
        synchronized (sender) {
            if (sender.getBalance() >= amount) {
                sender.setBalance(sender.getBalance() - amount);
                synchronized (receiver) {
                    receiver.setBalance(receiver.getBalance() + amount);
                }
            } else {
                notifyObservers("Ошибка: недостаточно средств у клиента #" + senderId);
            }
        }
    }

    // Обновление курсов валют
    private void updateExchangeRates() {
        Random rand = new Random();
        exchangeRates.forEach((currency, rate) -> {
            // rand.nextDouble() дает число от 0.0 до 1.0. (0.0 - 0.5) = -0.5, (1.0 - 0.5) = 0.5. Умножаем на 0.02.
            double fluctuation = (rand.nextDouble() - 0.5) * 0.02; // +/- 1%
            double newRate = rate + rate * fluctuation;
            exchangeRates.put(currency, newRate);
        });
        // Уведомляем наблюдателей об обновлении курсов.
        notifyObservers("Обновлены курсы валют: " + exchangeRates.toString());
    }

    // Получить клиента по ID
    public Client getClient(int id) {
        return clients.get(id);
    }

    // Метод для корректной остановки банка и его потоков
    public void shutdown() {
        // Остановка автоматического обновления курсов
        scheduler.shutdown();// Отключает возможность добавления новых задач и ожидает завершения текущих.
        try {
            // Ожидаем завершения потока планировщика до 2 секунд.
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();// Если не завершился, принудительно останавливаем.
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        // Установка флага для кассиров и выброс задания
        isActive.set(false);// Устанавливаем флаг, сигнализирующий кассирам, что пора завершаться.

        // Ожидание завершения кассиров
        cashierPool.shutdown();// Отключает возможность добавления новых задач в пул кассиров.
        try {
            // Ожидаем завершения всех задач кассиров до 5 секунд.
            if (!cashierPool.awaitTermination(5, TimeUnit.SECONDS)) {
                cashierPool.shutdownNow();// Если не завершились, принудительно останавливаем.
            }
        } catch (InterruptedException e) {
            cashierPool.shutdownNow();
        }
        // Очистка очереди, чтобы кассиры могли завершиться
        while (!transactionQueue.isEmpty()) {
            try {
                Thread.sleep(100);// Небольшая пауза, чтобы не нагружать процессор
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
