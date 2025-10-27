// Пополнение
class Deposit extends Transaction {
    final double amount; // Сумма пополнения

    public Deposit(int clientId, double amount) {
        super(clientId); // Вызов конструктора базового класса
        this.amount = amount;
    }

    @Override
    public void process(Bank bank) {
        bank.deposit(clientId, amount); // Вызывает метод deposit() объекта
        bank.notifyObservers("Deposit: клиент #" + clientId + " пополнил " + amount); // Уведомляет наблюдателей
    }
}