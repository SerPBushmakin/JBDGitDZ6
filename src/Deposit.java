// Пополнение
class Deposit extends Transaction {
    final double amount;

    public Deposit(int clientId, double amount) {
        super(clientId);
        this.amount = amount;
    }

    @Override
    public void process(Bank bank) {
        bank.deposit(clientId, amount);
        bank.notifyObservers("Deposit: клиент #" + clientId + " пополнил " + amount);
    }
}