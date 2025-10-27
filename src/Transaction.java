// Базовый класс транзакции
abstract class Transaction {
    final int clientId; // ID клиента, к которому относится транзакция

    public Transaction(int clientId) {
        this.clientId = clientId;
    }

    // Абстрактный метод, который должен быть реализован в каждом конкретном типе транзакции.
    // Он определяет, как транзакция должна быть обработана банком.
    public abstract void process(Bank bank);
}