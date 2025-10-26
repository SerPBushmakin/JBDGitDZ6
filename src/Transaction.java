// Базовый класс транзакции
abstract class Transaction {
    final int clientId;

    public Transaction(int clientId) {
        this.clientId = clientId;
    }

    public abstract void process(Bank bank);
}