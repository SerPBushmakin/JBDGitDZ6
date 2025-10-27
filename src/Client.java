// Класс клиента
class Client {
    private final int id; // Уникальный идентификатор клиента
    private double balance; // Баланс клиента
    private String currency; // Валюта клиента

    public Client(int id, double balance, String currency) {
        this.id = id;
        this.balance = balance;
        this.currency = currency;
    }

    public int getId() {
        return id;
    }
    // Синхронизированный метод для получения баланса
    public synchronized double getBalance() {
        return balance;
    }
    // Синхронизированный метод для установки баланса
    public synchronized void setBalance(double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }
    // Синхронизированный метод для установки валюты
    public synchronized void setCurrency(String currency) {
        this.currency = currency;
    }
}