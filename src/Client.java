// Класс клиента
class Client {
    private final int id;
    private double balance;
    private String currency;

    public Client(int id, double balance, String currency) {
        this.id = id;
        this.balance = balance;
        this.currency = currency;
    }

    public int getId() {
        return id;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized void setBalance(double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public synchronized void setCurrency(String currency) {
        this.currency = currency;
    }
}