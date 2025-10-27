// Снятие
class Withdrawal extends Transaction {
    final double amount; // Сумма снятия

    public Withdrawal(int clientId, double amount) {
        super(clientId);
        this.amount = amount;
    }

    @Override
    public void process(Bank bank) {
        bank.withdraw(clientId, amount); // Вызывает метод withdraw() объекта Bank
        bank.notifyObservers("Withdraw: клиент #" + clientId + " снял " + amount); // Уведомляет наблюдателей
    }
}
