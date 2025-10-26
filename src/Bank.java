import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

// Основной класс банка
class Bank {
    private final ConcurrentHashMap<Integer, Client> clients = new ConcurrentHashMap<>();
    private final List<Observer> observers = new ArrayList<>();
    private final BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService cashierPool;
    private final Map<String, Double> exchangeRates = new ConcurrentHashMap<>();
    private final AtomicBoolean isActive = new AtomicBoolean(true); // для остановки кассиров

    public Bank(int numCashiers) {
        // Инициализация валютных курсов
        exchangeRates.put("USD", 1.0);
        exchangeRates.put("EUR", 0.85);
        exchangeRates.put("RUB", 75.0);

        // Запуск автоматического обновления курсов
        scheduler.scheduleAtFixedRate(this::updateExchangeRates, 0, 1, TimeUnit.SECONDS);

        // Создание кассиров
        cashierPool = Executors.newFixedThreadPool(numCashiers);
        for (int i = 0; i < numCashiers; i++) {
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

    public void notifyObservers(String message) {
        for (Observer o : observers) {
            o.update(message);
        }
    }

    // Методы для работы с транзакциями
    public void enqueueTransaction(Transaction tx) {
        transactionQueue.add(tx);
    }

    public Transaction getTransaction() throws InterruptedException {
        return transactionQueue.poll(1, TimeUnit.SECONDS); // тайм-аут для проверки завершения
    }

    public boolean isTransactionQueueEmpty() {
        return transactionQueue.isEmpty();
    }

    // Работа с балансом
    public void deposit(int clientId, double amount) {
        Client client = clients.get(clientId);
        if (client != null) {
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
                double amountInBase = amount / rateFrom;
                double convertedAmount = amountInBase * rateTo;
                // Обновление баланса
                client.setBalance(client.getBalance() - amount);
                client.setBalance(client.getBalance() + convertedAmount);
                client.setCurrency(toCurrency);
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
            double fluctuation = (rand.nextDouble() - 0.5) * 0.02; // +/- 1%
            double newRate = rate + rate * fluctuation;
            exchangeRates.put(currency, newRate);
        });
        notifyObservers("Обновлены курсы валют: " + exchangeRates.toString());
    }

    // Получить клиента
    public Client getClient(int id) {
        return clients.get(id);
    }

    // Для проверки завершения работы
    public void shutdown() {
        // Остановка автоматического обновления курсов
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        // Установка флага для кассиров и выброс задания
        isActive.set(false);

        // Ожидание завершения кассиров
        cashierPool.shutdown();
        try {
            if (!cashierPool.awaitTermination(5, TimeUnit.SECONDS)) {
                cashierPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            cashierPool.shutdownNow();
        }
        // Очистка очереди, чтобы кассиры могли завершиться
        while (!transactionQueue.isEmpty()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
